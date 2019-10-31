import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

public interface ITracker extends Remote {
    //Metodos de acceso
    String getName() throws RemoteException;
    ArrayList<String> getAvailableTorrents() throws RemoteException;
    ArrayList<String> getListOfPeers() throws RemoteException;
    String getMapOfIntentions() throws RemoteException;

    //Metodos de uso para el peer.
    void registerTorrent(Torrent t, Peer p) throws RemoteException;
    void registerAccessInfo(Peer p) throws RemoteException;
    void abandonHive(Peer p) throws RemoteException;
    void registerIntention(String torrent, int intention, Peer p) throws RemoteException;
    Torrent getTorrent(String s) throws RemoteException;
    Hashtable<Integer,String> getHashOfFile(String file) throws RemoteException;
    ArrayList<String> getListOfSeeds(String file) throws RemoteException;
}