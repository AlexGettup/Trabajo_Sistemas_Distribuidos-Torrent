import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;

public class TorrentUtils {
    final static int BLOCKSIZE = 256 * 1024;    //Tamaño del bloque: 256 KB

    ///Funcion que convierte bytes en un string hexadecimal.
    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i=0; i < b.length; i++) {
            result +=
                    Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }

    //Funcion que calcula los hash de un archivo y lo guarda en una HashTable
    public static Hashtable<Integer,String> hashFile(String fileName){
        Hashtable<Integer,String> ht = null;
        try {
            RandomAccessFile file = new RandomAccessFile(fileName,"r");                     //Creamos el acceso aleatorio al archivo (Solo lectura)
            try(FileChannel fin = file.getChannel()){                                       //Usamos un try-with-resources
                int numBlocks =(int) Math.ceil((double)fin.size() / BLOCKSIZE);             //Calculamos el numero de bloques en los que se va a dividir el archivo

                //System.out.println("Numero de bloques que vamos a hacer: " + numBlocks);

                ht = new Hashtable<>(numBlocks);                  //Almacenamos el resumen en un HashTable
                ByteBuffer buffer = ByteBuffer.allocate(BLOCKSIZE);                         //Le damos al buffer el mismo tamaño del bloque

                int key = 0;    //Clave con la que vamos a rellenar el HashTable
                int bytesRead;  //Numero de bytes leídos

                System.out.println("---------- Calculando hash de " + fileName + " ----------");
                while((bytesRead = fin.read(buffer)) != -1){
                    byte[] bb = new byte[bytesRead];    //Creamos un array de bytes con el numero de bytes leidos
                    buffer.flip();                      //Flipeamos el buffer.El limite se convierte en la posicion actual y la posicion en cero
                    buffer.get(bb,0,bytesRead);         //Guarda en el array bb los bytes que ha leido. Siempre <= BLOCKSIZE

                    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");    //Usamos el MessageDigest con el algoritmo SHA-1
                    byte[] sha = sha1.digest(bb);                               //Guardamos en un array el resultado de hacer el SHA-1 sobre bb

                    System.out.println("[Bloque " + key +", SHA-1 ----> " + TorrentUtils.byteArrayToHexString(sha) + "]");

                    ht.put(key++,TorrentUtils.byteArrayToHexString(sha));        //Guardamos en la Hashtable los valores (Key, SHA-1)
                    buffer.clear();                                 //Finalmente, rseteamos el buffer para usarlo en la siguiente iteracion
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                System.err.println("SHA-1 algorithm is not available...");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("----------------------------------------------------");
        return ht;
    }

    //Funcion que guarda el hash de un bloque de un archivo dado.
    public static String getHashOfBlock(String fileName, int numBlock){
        String hash = null;
        try {
            RandomAccessFile file = new RandomAccessFile(fileName,"r");
            try(FileChannel fin = file.getChannel()){                                       //Usamos un try-with-resources
                int numBlocks =(int) Math.ceil((double)fin.size() / BLOCKSIZE);
                ByteBuffer buffer = ByteBuffer.allocate(BLOCKSIZE);
                int bytesRead = fin.read(buffer, numBlock*BLOCKSIZE);
                byte[] bb = new byte[bytesRead];
                buffer.flip();
                buffer.get(bb,0,bytesRead);
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");    //Usamos el MessageDigest con el algoritmo SHA-1
                byte[] sha = sha1.digest(bb);
                hash = TorrentUtils.byteArrayToHexString(sha);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        return hash;
    }
}
