import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
public static final int MAX_CLIENTS_NUMBER = 3;
public static int clientCounter=0;

private static String ich;

    /*
    * 1. Serversocket anlegen und auf Verbidnungen warten
    * 2. Verbindung annehmen und 30-sec-timer starten
    * 3. Sendung empfangen oder ablehnen?
    * 4. Befehl verarbeiten
    *
    *
    * */

    public static void main(String[] args) throws IOException {

        int port = Integer.parseInt(args[0]);


        try(ServerSocket serverSocket = new ServerSocket(port)){
            while (true && clientCounter<MAX_CLIENTS_NUMBER){
                try(Socket s = serverSocket.accept()){
                    new ServerThread(s).run();
                    clientCounter++;
                }
                catch (IOException iox){
                    System.err.println(iox.getMessage());
                }

            }

        }

    }

    public static void clientClosed(){
        synchronized (ich){
            clientCounter--;
        }
    }

    private static void makeSocket(){}

    private static void waitConnection(){}

    private static void timer(){}



}
