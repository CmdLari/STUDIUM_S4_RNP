import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
public static final int MAX_CLIENTS_NUMBER = 3;
public static int clientCounter=0;

private static String ich ="";

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

            System.out.printf("Server auf Port %d aktiviert und bereit\n",port);

            while (true){
                try{
                    Socket s = serverSocket.accept();
                    clientCounter++;
                    System.out.printf("Anzahl Clienten: %2d\n",clientCounter);
                    new ServerThread(s).start();

                    System.out.printf("Warte auf nächsten CLient...\n");}
                catch (IOException iox){
                    System.err.println(iox.getMessage());
                }

            }

        }

    }

    public static void clientClosed(){
        synchronized (ich){
            clientCounter--;
            System.out.printf("Anzahl Clienten: %2d\n",clientCounter);
        }
    }

    private static void makeSocket(){}

    private static void waitConnection(){}

    private static void timer(){}



}
