/*
El proceso tracker se encarga de coordinar a los pares. Para ello:
a) Se proporciona una IP/puerto conocido a todos los pares o bien un nombre de objeto remoto.
b) Permite el registro del resumen del archivo que se está compartiendo, de manera que un seed pueda proporcionar dicha información.
c) Permite el registro de información de acceso de los pares. Los pares se registrarán con el tracker, proporcionando la información que permite conectarse a ellos: IP/puerto o nombre de objeto remoto.
d) Permite el registro de seed/descarga. Los pares registrarán en el tracker si están descargando el archivo o hacen de seed.
e) Proporciona el resumen del archivo a los pares que lo solicitan.
f) Proporciona una lista de pares que están compartiendo el archivo. Es decir, proporciona una lista con la información de acceso de dichos pares.
g) Permite registrar cuando un par deja el enjambre. De manera que el tracker lo elimine de la lista.
Como se puede ver, el tracker proporciona funcionalidad que permite la coordinación de los pares. Los pares usarán el tracker básicamente para obtener la información del archivo que se descarga,
y para saber qué pares lo comparten, es decir, unirse o dejar el enjambre.
 */

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashMap;

public class Tracker extends java.rmi.server.UnicastRemoteObject implements ITracker{
    private String name;                                                                    //Nombre del Tracker, que se usara para el objeto remoto.
    private ArrayList<Torrent> availableTorrents;                                           //Lista de Torrents
    private ArrayList<String> listOfPeers;                                                  //Lista de info de los Peers
    private HashMap<Torrent,HashMap<Integer, ArrayList<String>>> mapOfIntentions;           //Mapa de los torrents. 0 for seeder, 1 for downloader (Seria conveniente crear un objeto propio)

    //Constructor
    public Tracker(String name) throws RemoteException{
        this.name = name;
        availableTorrents = new ArrayList<>();
        listOfPeers = new ArrayList<>();
        mapOfIntentions = new HashMap<>();
    }

    //Metodos de acceso
    public String getName() {
        return name;
    }
    public ArrayList<String> getAvailableTorrents() {
        ArrayList<String> result = new ArrayList<>();
        for (Torrent t: availableTorrents) {
            result.add(t.getFileName());
        }
        return result;

    }
    public ArrayList<String> getListOfPeers() {
        return listOfPeers;
    }
    public String getMapOfIntentions() {
        ArrayList<String> seeders;
        ArrayList<String> leechers;
        String result = "---- MAPA DE INTENCIONES ----\n";
        for (Torrent t : availableTorrents){
            result += "--- " +t.getFileName()+" ---\n";
            HashMap<Integer, ArrayList<String>> map = mapOfIntentions.get(t);
            seeders = map.get(0);
            leechers = map.get(1);
            result += "Seeders: ";
            if (seeders != null) {
                if(!seeders.isEmpty()){
                    for (String p: seeders){
                        result += p + " ";
                    }
                }
            }
            result += "\nLeechers: ";
            if (leechers != null) {
                if(!leechers.isEmpty()){
                    for (String p: leechers){
                        result += p + " ";
                    }
                }
            }
            result += "\n--- " +t.getFileName()+" ---\n";
        }
        result += "-----------------------------";
        return result;
    }

    //Obtiene un objeto torrent para registrar la intención.
    public Torrent getTorrent(String s){
        Torrent torrent = null;
        for(Torrent t: availableTorrents){
            if(t.getFileName().equals(s)){
                torrent = t;
            }
        }
        return torrent;
    }

    //Metodo para obtener el resumen de un archivo para los pares que lo solicitan
    public Hashtable<Integer,String> getHashOfFile(String file){
        Hashtable<Integer,String> result = null;
        if (!availableTorrents.isEmpty()) {
            for(Torrent t : availableTorrents){
                if(t.getFileName().equals(file)){
                    result = t.getHt();
                }
            }
        }
        return result;
    }

    //Metodo para obtener una lista de pares que estan compartiendo un archivo.
    public ArrayList<String> getListOfSeeds(String file){
        Torrent t = getTorrent(file);
        ArrayList<String> list = new ArrayList<>();
        if(mapOfIntentions.containsKey(t)){
            list = mapOfIntentions.get(t).get(0);
        }

        return list;
    }

    //Metodo para registrar el resumen del archivo que se esta compartiendo.
    public void registerTorrent(Torrent t, Peer p){
        if (!availableTorrents.contains(t)) {
            //Añadimos el torrent a la lista de torrents disponibles
            availableTorrents.add(t);

            //Creamos una tabla para introducir el torrent al mapa de intenciones.
            HashMap<Integer, ArrayList<String>> map = new HashMap<>();
            ArrayList<String> peers = new ArrayList<>();
            ArrayList<String> empty = new ArrayList<>();
            peers.add(p.getName());
            map.put(0, peers);  //Lista de seeders
            map.put(1, empty);   //Lista de downloaders
            mapOfIntentions.put(t, map);
        }

    }

    //Metodo para registrar la informacion de acceso a los pares
    public void registerAccessInfo(Peer p){
        if(!listOfPeers.contains(p.getName())){
            listOfPeers.add(p.getName());
        }
    }

    //Metodo para registrar si es seed o downloader. Si intention es 0, es un seeder. Si es 1, es un downloader.
    public void registerIntention(String torrent, int intention, Peer p){
        Torrent t = getTorrent(torrent);
        if ((intention == 1 || intention == 0) && t != null) {
            HashMap<Integer, ArrayList<String>> listOfIntentions = mapOfIntentions.get(t);
            ArrayList<String> peers = listOfIntentions.get(intention);
            peers.add(p.getName());
            listOfIntentions.replace(intention, peers);
            mapOfIntentions.replace(t, listOfIntentions);
        }else{
            System.out.println("Ha ocurrido un error registrando la intencion.");
        }

    }

    //Metodo para registrar cuando un par deja el enjambre.
    public void abandonHive(Peer p){
        //Se elimina al peer de la lista de peers.
        listOfPeers.remove(p.getName());


        //Se elimina al peer del mapa de intenciones.
        for (Torrent t : availableTorrents){
            HashMap<Integer, ArrayList<String>> listOfIntentions = mapOfIntentions.get(t);
            for(int i = 0; i < 2; i++){
                ArrayList<String> peers = listOfIntentions.get(i);
                if (peers != null) {
                    peers.remove(p.getName());
                }
                listOfIntentions.replace(i, peers);
            }
            mapOfIntentions.replace(t,listOfIntentions);
        }
    }



    //Metodo main
    public static void main(String[] args){
        try {
            Tracker myTracker = new Tracker("myTracker");
            System.out.println("Tracker: " + myTracker.getName() + " listo.");
            Naming.rebind(myTracker.getName(), myTracker);
        } catch (Exception e) {
            System.out.println("Hubo un problema registrando el tracker.");
        }
    }

}
