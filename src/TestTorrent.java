import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class TestTorrent {
    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {
        String PEER_1_NAME = "P1";
        String PEER_2_NAME = "P2";
        String PEER_3_NAME = "P3";
        String TRACKER_NAME = "myTracker";


        //Test de hashFile
        System.out.println("TEST 1: Comprobacion de HashFile");
        TorrentUtils.hashFile(args[0]);
        int block = 0;  //Bloque del que hacer hash
        System.out.println("Hash SHA-1 del bloque "+ block + " de " + args[0] + " -> " + TorrentUtils.getHashOfBlock(args[0], block));
        System.out.println("----------------------------------------------------");

        //Test de Torrent
        System.out.println("TEST 2: Creacion de un nuevo Torrent");
        Torrent t = new Torrent(args[0]);
        System.out.println("Metodo getFileName: " + t.getFileName());
        System.out.println("Metodo getSize: " + t.getSize());
        System.out.println("Metodo getTracker: " + t.getTracker());
        System.out.println("Metodo getHt: " + t.getHt().toString());
        System.out.println("----------------------------------------------------");


        //Test de Peer
        System.out.println("TEST 3: Peer (PARTE 1)");
        Peer p = new Peer(PEER_1_NAME);
        System.out.println("Metodo getFileName: " + p.getName());
        System.out.println("Metodo getListOfNeighbors: " + p.getListOfNeighbors());
        System.out.println("----------------------------------------------------");
        p.uploadTorrentInfo(t);
        System.out.println("Mapa de bloques de P1: " + p.getMapOfBlocks());
        //Test de Tracker
        System.out.println("TEST 4: Tracker");
        Tracker tracker = new Tracker(TRACKER_NAME);
        System.out.println("Metodo getName: " + tracker.getName());
        tracker.registerTorrent(t, p);
        System.out.println("Registrando el torrent " + t.getFileName() + " desde el Peer " + p.getName() +".");
        tracker.registerAccessInfo(p);
        System.out.println("Registrando la información de acceso del Peer  " + p.getName() +".");
        System.out.println("Torrents disponibles: " + tracker.getAvailableTorrents());
        System.out.println("Hash del torrent: " + tracker.getHashOfFile(t.getFileName()));
        System.out.println("Peers registrados: " + tracker.getListOfPeers());
        System.out.println("Seeds del torrent " + t.getFileName() + " : " + tracker.getListOfSeeds(t.getFileName()));
        System.out.println("Mapa de intenciones: ");
        System.out.println(tracker.getMapOfIntentions());
        Peer p2 = new Peer(PEER_2_NAME);
        tracker.registerAccessInfo(p2);
        tracker.registerIntention(t.getFileName(), 1, p2);
        System.out.println("El Peer " + p2.getName() + " quiere descargar el torrent " + t.getFileName() + ".");
        System.out.println(tracker.getMapOfIntentions());

        //tracker.abandonHive(p);
        //System.out.println("El Peer " + p.getName() + " ha abandonado el enjambre.");
        System.out.println("Peers registrados: " + tracker.getListOfPeers());
        System.out.println("Seeds del torrent " + t.getFileName() + " : " + tracker.getListOfSeeds(t.getFileName()));
        System.out.println("Mapa de intenciones: ");
        System.out.println(tracker.getMapOfIntentions());
        System.out.println("----------------------------------------------------");

        //VERSION 3
        System.out.println("----------- ULTIMO ENTREGABLE DEL TRABAJO -----------");
        Peer p3 = new Peer(PEER_3_NAME);
        tracker.registerAccessInfo(p3);
        tracker.registerIntention(t.getFileName(), 1, p3);
        p3.downloadTorrent(tracker, t.getFileName());   //Actualiza su mapa de bloques.
        System.out.println("El Peer " + p3.getName() + " quiere descargar el torrent " + t.getFileName() + ".");
        System.out.println("Mapa de bloques de P3: " + p3.getMapOfBlocks());
        p3.setListOfNeighbors(tracker.getListOfPeers());
        System.out.println("La lista de pares ahora mismo es: " + tracker.getListOfPeers());
        System.out.println("La lista de vecinos de p3 tras asctualizar es: " + p3.getListOfNeighbors());

        p2.downloadTorrent(tracker, t.getFileName());
        //Simulamos bloque 0 descargado
        p3.blockDownloaded(tracker, t.getFileName(), block);
        System.out.println("Mapa de bloques de P3 una vez bajado el bloque 0: " + p3.getMapOfBlocks());

        System.out.println("Informamos a P2 de que P3 tiene el bloque");
        p2.updateMapOfBlocks(p3);
        System.out.println("Mapa de bloques de P2: " + p2.getMapOfBlocks());
        System.out.println("Prceso P3 tiene el bloque 0? : " + p3.hasBlock(t.getFileName(), 0));

        System.exit(0);
    }
}
