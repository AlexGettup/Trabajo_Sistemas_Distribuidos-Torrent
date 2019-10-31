import java.io.File;
import java.io.Serializable;
import java.util.Hashtable;

public class Torrent implements Serializable{
    private File fileName;                          //El nombre del archivo
    private Hashtable<Integer,String> hashing;      //Informacion de Hash
    private long size;                              //Tamaño del archivo en bytes
    private String tracker = "myTracker";           //El Tracker al que esta asociado el Torrent.
    private int numBlocks;                          //Número de bloques en los que se divide el archivo.
    final int BLOCKSIZE = 256 * 1024;               //256 KB

    //Constructor
    public Torrent(String fileName) {
        this.fileName = new File(fileName);
        this.hashing = TorrentUtils.hashFile(fileName);
        this.size = this.fileName.length();
        this.numBlocks = (int) Math.ceil((double)this.size / BLOCKSIZE);
    }

    //Métodos de acceso
    public String getFileName() {
        return fileName.getName();
    }
    public String getTracker() {
        return tracker;
    }
    public Hashtable<Integer, String> getHt() {
        return hashing;
    }
    public long getSize() {
        return size;
    }
    public int getNumBlocks(){
        return numBlocks;
    }


}
