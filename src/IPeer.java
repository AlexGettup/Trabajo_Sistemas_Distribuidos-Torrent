import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

public interface IPeer extends Remote {
    String getName() throws RemoteException;
    ArrayList<String> getListOfNeighbors() throws RemoteException;
    HashMap<String, HashMap<String, HashMap<Integer, Integer>>> getMapOfBlocks() throws RemoteException;
    int getPeopleDownloading() throws RemoteException;

    int hasBlock(String torrent, int numBlock) throws RemoteException;
    void setListOfNeighbors(ArrayList<String> listOfNeighbors) throws RemoteException;
    void updateListOfNeighbors(String peer) throws RemoteException;
    void uploadTorrentInfo(Torrent t) throws RemoteException;
    void downloadTorrent(Tracker t, String torrent) throws RemoteException;
    void updateMapOfBlocks(Peer p) throws RemoteException;
    void blockDownloaded(Tracker t, String torrent, int numBlock) throws RemoteException;
    int blockSelectionAlg(Tracker tracker, String file) throws RemoteException;
    String peerSelectionAlg(String torrent, int numBlock) throws RemoteException;
    void downloadFinished() throws RemoteException;
    void recieveBlock(Peer p, String torrent, int numBlock) throws RemoteException;
    void sendBlock(String torrent, int numBlock) throws RemoteException;
}
