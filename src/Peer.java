/*TODO
1) Todos los procesos clientes o pares (peer) deben ser capaces de actuar como seed inicial. Para ello deben ser capaces de:

a) Seleccionar al menos un archivo a compartir (que se pasará como parámetro al ejecutar el cliente o de otra forma).
b) Crear un “torrent” del archivo:
c) Registrar en el tracker el archivo que se comparte. De manera que el tracker pueda a su vez informar al resto de los pares de los archivos disponibles.
d) Registrar su propia información de acceso. Es decir,  el nombre del objeto remoto, si se utiliza RMI.
De este modo, cuando un par actúa como seed, primero genera una lista de hash del archivo que comparte, después
registra esta información en el tracker y después queda a la espera de peticiones de bloques del archivo de otros pares.
 */

import java.io.*;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer extends java.rmi.server.UnicastRemoteObject implements Serializable, IPeer {
    private String name;    //Nombre del peer. (¿Posibilidad de introducir el nomnbre del Peer manualmente por comando, o automaticamente cogiendo info del ordenador/IP?)
    private ArrayList<String> listOfNeighbors; //Lista de vecinos disponibles
    private HashMap<String, HashMap<String,HashMap<Integer, Integer>>> mapOfBlocks; //Nombre del peer, torrent, bloque y 0 o 1.
    private int blocksDownloaded=0;
    private ArrayList<Integer> listOfDownloadedBlocks;
    private int peopleDownloading = 0;

    Random rnd = new Random();


    public Peer(String name) throws RemoteException{
        this.listOfNeighbors = new ArrayList<>();
        this.name = name;
        this.mapOfBlocks = new HashMap<>();
        this.listOfDownloadedBlocks = new ArrayList<>();
    }


    //Metodos de acceso
    public String getName() {
        return name;
    }

    public ArrayList<String> getListOfNeighbors() {
        return listOfNeighbors;
    }

    public HashMap<String, HashMap<String, HashMap<Integer, Integer>>> getMapOfBlocks() {
        return mapOfBlocks;
    }

    public int getPeopleDownloading() {
        return peopleDownloading;
    }


    //Establece una nueva lista de vecinos
    public void setListOfNeighbors(ArrayList<String> listOfNeighbors) {
        this.listOfNeighbors.clear();
        for (String s: listOfNeighbors) {
            if(!s.equals(this.name))this.listOfNeighbors.add(s);
        }
    }

    //Actualiza la lista de vecinos. (Cada vez que un par que no está en la lista le solicita conexión)
    public void updateListOfNeighbors(String peer){
        if(!listOfNeighbors.contains(peer)) listOfNeighbors.add(peer);
    }


    //Cuando informas al tracker de que tienes un archivo, se usa este método para actualizar su mapa de bloques
    public void uploadTorrentInfo(Torrent t){
        int numBlocks = t.getNumBlocks();
        HashMap<Integer, Integer> hm = new HashMap<>();
        HashMap<String, HashMap<Integer, Integer>> hmTotal = new HashMap<>();
        for(int i = 0; i < numBlocks; i++){
            hm.put(i,1);
        }
        hmTotal.put(t.getFileName(),hm);
        this.mapOfBlocks.put(this.name, hmTotal);
    }

    //Cuando informas al tracker de que quieres descargar un archivo, se usa este metodo para actualizar su mapa de bloques y reservar espacio en el disco
    public void downloadTorrent(Tracker t, String torrent){
        //Actualizar el mapa de bloques
        int numBlocks = t.getTorrent(torrent).getNumBlocks();
        HashMap<Integer, Integer> hm = new HashMap<>();
        HashMap<String, HashMap<Integer, Integer>> hmTotal = new HashMap<>();
        for(int i = 0; i < numBlocks; i++){
            hm.put(i,0);
        }
        hmTotal.put(t.getTorrent(torrent).getFileName(),hm);
        this.mapOfBlocks.put(this.name, hmTotal);

        //Reservar espacio en el disco
        try {
            //Creamos un nuevo archivo con el mismo tamaño que el que vamos a descargar (Para reservar espacio)
            RandomAccessFile f = new RandomAccessFile("dwl_" + torrent, "rw");
            f.setLength(t.getTorrent(torrent).getSize());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //El peer actualiza su mapa de bloques con el mapa de bloques actualizado de otro peer.
    public void updateMapOfBlocks(Peer p){
        HashMap<String,HashMap<Integer, Integer>> map = p.getMapOfBlocks().get(p.getName());
        this.mapOfBlocks.put(p.getName(), map);
    }


    //Cuando un par termina de descargarse un bloque, se usa este método.
    public void blockDownloaded(Tracker t, String torrent, int numBlock){
        //Comprobacion de la integridad del bloque
        Hashtable<Integer, String> ht = t.getHashOfFile(torrent);
        String hashRequired = ht.get(numBlock);
        String hash  = TorrentUtils.getHashOfBlock(torrent, numBlock) ;

        if(hashRequired.equals(hash)){
            blocksDownloaded++; //Añadimos uno al numero de bloques descargados
            listOfDownloadedBlocks.add(numBlock);   //Añadimos el bloque descargado a la lista de bloques descargados para facil acceso
            //Este peer actualiza su propio mapa de bloques
            this.getMapOfBlocks().get(this.getName()).get(torrent).put(numBlock,1);

            //Informar a los vecinos de los bloques que adquieres
            /* NO FUNCIONA
            for (String peer : listOfNeighbors) {
                IPeer p = (IPeer) Naming.lookup(peer);
                p.updateMapOfBlocks(this);
            }
            */

        }

    }
    //Devuelve 0 si no tiene ese bloque o 1 si lo tiene
    public int hasBlock(String torrent, int numBlock){
        int result = 0;
        HashMap<String, HashMap<Integer, Integer>> first = this.getMapOfBlocks().get(this.name);
        if(first == null){
            return result;
        }else{
            HashMap<Integer, Integer> second = first.get(torrent);
            if(second == null){
                return result;
            }else{
                return second.getOrDefault(numBlock,0);
            }
        }
    }
    //Devuelve el bloque seleccionado mediante dos algoritmos: Primero seleccion aleatoria de bloques, y luego mediante Rarest-First.
    public int blockSelectionAlg(Tracker tracker, String file) throws RemoteException{
        int totalBlocks = tracker.getTorrent(file).getNumBlocks();
        if(blocksDownloaded < 4){
            //Seleccion aleatoria de bloques
            int next = rnd.nextInt(totalBlocks);        //Elegimos un numero al azar entre los bloques disponibles
            while(listOfDownloadedBlocks.contains(next)){   //Si el bloque disponible ya está decargado, elegimos otro
                next = rnd.nextInt(tracker.getTorrent(file).getNumBlocks());
            }
            return next;    //Si el bloque elegido al azar no esta descargado, lo descargamos.

        }else{
            //Rarest-First
            int lowest = 0;
            for(int i = 0; i < listOfDownloadedBlocks.size(); i++){ //Seleccionamos el siguiente no descargado.
                if(listOfDownloadedBlocks.contains(lowest)){
                    lowest++;
                }else{
                    break;
                }
            }
            //Ahora lowest es el siguiente bloque no descargado.
            int totalNumOfSeeds = Integer.MAX_VALUE;
            int next = lowest;
            for(int i = lowest; i < totalBlocks; i++){
                //NewTotal es el numero de pares que tienen el archivo lowest
                int newTotal = 0;
                for (String peer : listOfNeighbors) {
                    IPeer p = null;
                    try {
                        p = (IPeer) Naming.lookup(peer);
                    } catch (NotBoundException | MalformedURLException e) {
                        e.printStackTrace();
                    }
                    if(((Peer)p).hasBlock(file, i) == 1) newTotal++;
                }
                if((newTotal < totalNumOfSeeds) && (newTotal != 0)){
                    totalNumOfSeeds = newTotal;
                    next = i;
                }

            }

            return next;
        }
    }

    //Algoritmo de seleccion de Peer.
    public String peerSelectionAlg(String torrent, int numBlock) throws RemoteException{
        String result = "error";
        for(String peer : listOfNeighbors){
            IPeer p = null;
            try {
                p = (IPeer) Naming.lookup(peer);
            } catch (NotBoundException | MalformedURLException e) {
                e.printStackTrace();
            }
            if(((Peer)p).hasBlock(torrent, numBlock) == 1 && p.getPeopleDownloading() < 4){
                //Una especie de FIFO. Devuelve el primer peer que encuentra que tiene el archivo y esta subiendo el archivo a menos de 4 personas.
                result =  p.getName();
                break;
            }
        }
        return result;
    }

    //Reseteamos los valores de bloques descargados
    public void downloadFinished(){
        listOfDownloadedBlocks.clear();
        blocksDownloaded = 0;
    }

    //Metodo para enviar un bloque
    public void sendBlock(String torrent, int numBlock) throws RemoteException{
        int BLOCKSIZE = 256 * 1024;
        //Obtenemos el bloque a transmitir
        byte[] bb = null;
        try {
            RandomAccessFile file = new RandomAccessFile(torrent,"r");
            try(FileChannel fin = file.getChannel()){
                ByteBuffer buffer = ByteBuffer.allocate(BLOCKSIZE);
                int bytesRead = fin.read(buffer, numBlock*BLOCKSIZE);
                bb = new byte[bytesRead];
                buffer.flip();
                buffer.get(bb,0,bytesRead);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        //Transmitimos el bloque
        try {
        File file = new File(torrent);
        FileOutputStream fout = new FileOutputStream(file);
        fout.write(bb,0,bb.length);
        fout.flush();
        fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Metodo para recibir un bloque
    public void recieveBlock(Peer p, String torrent, int numBlock){
        int BLOCKSIZE = 256 * 1024;
        try{
            File file = new File("dwl_" + torrent);
            FileInputStream fin = new FileInputStream(file);
            byte[] block = new byte[BLOCKSIZE];
            int bytesRead = fin.read(block);
            while(bytesRead > 0){
                p.sendBlock(torrent, numBlock);
                bytesRead = fin.read(block);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;
        String file = null;
        Torrent newTorrent = null;

        if((args.length <1 || args.length > 2)) //Test for correct # of args
            throw new IllegalArgumentException("Parameter(s): <Nombre del Peer> <Nombre del archivo a compartir>");

        //Se crea el Peer
        Peer p = new Peer(args[0]);

        //NO FUNCIONA
        //Naming.rebind(p.getName(), p);

        //Seleccionar un archivo para compartir (¿Parametro o GUI?) //V3: Si tiene archivo
        if(args.length == 2){
            file = args[1];
        }

        //Crear el torrent del archivo. //V3: Si hay archivo
        if(file != null) newTorrent = new Torrent(file);
        //V3: Actualiza su mapa de bloques
        if(file != null) p.uploadTorrentInfo(newTorrent);

        //Conectarse al tracker //V3: Y actualiza la lista de vecinos
        ITracker tracker = (ITracker) Naming.lookup("myTracker");
        p.setListOfNeighbors(tracker.getListOfPeers());

        //Registrar en el tracker el archivo que se comparte //V3: Si hay archivo
        if(file != null) { tracker.registerTorrent(newTorrent, p);
        }

        //Registrar su informacion de acceso.
        tracker.registerAccessInfo(p);

        //Introduccion de comandos.
        while(exit != true){
            System.out.println("Introduce un comando:\ntorrents hash peers seeds download map abandon exit");
            String command = scanner.nextLine();

            switch (command){

                case "torrents":    //Devuelve la lista de torrents disponibles.
                    System.out.println("Torrents disponibles: " + tracker.getAvailableTorrents());
                    break;

                case "hash":        //Pregunta por un torrent del que quieras conocer el hash y devuelve dicho hash.
                    System.out.println("¿De qué torrent quieres conocer el hash?\n" + tracker.getAvailableTorrents());
                    command = scanner.nextLine();
                    for(String s : tracker.getAvailableTorrents()){
                        if(s.toLowerCase().equals(command.toLowerCase())){
                            System.out.println("Hash del torrent " + tracker.getHashOfFile(command));
                            break;
                        }
                        System.out.println("Archivo incorrecto.");
                    }
                    break;

                case "download":   //Pregunta por un torrent que quieras descargar
                    System.out.println("¿Qué torrent quieres descargarte?\n" + tracker.getAvailableTorrents());
                    command = scanner.nextLine();
                    for(String s : tracker.getAvailableTorrents()){
                        if(s.toLowerCase().equals(command.toLowerCase())){
                            System.out.println("Descargando " + s + "...");
                            tracker.registerIntention(s, 1, p);
/*                          NO FUNCIONA
                            //V3:
                            p.downloadTorrent((Tracker) tracker, s);    //Primero informamos al tracker de que quieres bajar este archivo y actualiza su mapa de bloques y reserva espacio en disco.
                            //Mientras queden bloques por descargar, sigue descargandolos
                            while(p.blocksDownloaded < tracker.getTorrent(s).getNumBlocks()){
                                int numBlock = p.blockSelectionAlg((Tracker) tracker, s);   //Algoritmo de seleccion de bloque
                                String peer = p.peerSelectionAlg(s, numBlock);              //Algoritmo de seleccion de pares
                                IPeer seed = (IPeer) Naming.lookup(peer);
                                p.recieveBlock((Peer) seed, s, numBlock);                   //Recibes el bloque del seed.
                                p.blockDownloaded((Tracker) tracker, s, numBlock);      //Comprueba integridad del bloque e informa a los demás vecinos.
                            }
                            p.downloadFinished();   //Al acabar, resetea sus valores de bloques descargados.
*/
                            break;
                        }
                        System.out.println("Archivo incorrecto.");
                    }
                    break;

                case "peers":       //Devuelve una lista con los peers actualmente registrados en el Tracker
                    System.out.println("Peers registrados: " + tracker.getListOfPeers());
                    break;

                case "seeds":       //Pregunta por un torrent del que quieras conocer los seeds y devuelve dichos seeds
                    System.out.println("¿De qué torrent quieres conocer sus seeds?\n" + tracker.getAvailableTorrents());
                    command = scanner.nextLine();
                    System.out.println("Seeds del torrent " + tracker.getListOfSeeds(command));
                    break;

                case "map":         //Devuelve el mapa de intenciones de todos los torrents disponibles
                    System.out.println("Mapa de intenciones: ");
                    System.out.println(tracker.getMapOfIntentions());
                    break;

                case "abandon":     //Abandona el enjambre
                    tracker.abandonHive(p);
                    System.out.println("Has abandonado el enjambre.");
                    break;

                case "exit":        //Abandona el enjambre y cierra el cliente
                    System.out.println("Cerrando Peer.");
                    tracker.abandonHive(p);
                    exit = true;
                    break;

                default:
                    System.out.println("Comando incorrecto.");
                    break;

            }
        }
    }
}
