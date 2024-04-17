import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;


public class Client {

    /**
     * Erstellt einen neuen Clienten und baut die verbindung zum Server auf.
     * @param args - 1. hostname/IP-Addresse des Servers, 2. Portnummer des Servers
     */
    public static void main(String[] args) {
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        while(true){
            printPrompt();
            //readPrompt();
        }
//        try(Socket s = new Socket(hostname,port);InputStream input = socket.getInputStream(); Output){
//
//        }catch (IOException iox){
//            System.err.printf("Verbindung zum server ist fehlgeschlagen\n\t%s\n",iox.getMessage());
//        }
    }


    /**
     *
     */
    static void printPrompt(){
        System.out.printf("Client>>");
    }

    /**
     * Method to check a string of characters
     */
    public void checkMsg(String string) throws MsgTooLongException{
        try {
                printMsg(string);
            }
        }
        catch(MsgTooLongException ex){
            int maxCharacters = 120;
            if (string.length() > maxCharacters){
                throw new MsgTooLongException();
        }
    }

    /**
     * Method to print a string of characters
     */
    public void printMsg(String string) throws MsgTooLongException{
        System.out.println(string);
    }

}



