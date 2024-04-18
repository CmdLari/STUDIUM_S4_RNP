import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;


public class Client {

    /**
     * Makes a new client and establishes a connection to the server
     * @param args - 1. hostname/IP-Address of the servers, 2. PortNr of the server
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
     * Method to check a string of characters received
     * @param string string of characters
     */
    public void checkMsg(String string){
        int maxCharacters = 120;
        if (string.length() > maxCharacters){
            System.err.println("This message exceeds length limitations.");
        }
        else{
            useMsg(string);
        }
    }

    /**
     * Method to process a string of characters received
     * @param string string of characters
     */
    public void useMsg(String string){

    }












    /**
     *
     */
    static void printPrompt(){
        System.out.printf("Client>>");
    }



    /**
     * Method to print a string of characters
     */
    public void printMsg(String string){
        System.out.println(string);
    }

}



