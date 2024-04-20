import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
public static final int MAX_CLIENTS_NUMBER = 3;
public static int clientCounter=0;
private static final String ich ="";
private static boolean acceptNewConnections = true ; // Needed to handle SHUTDOWN command
private static List<ServerThread> clients;


    /*
    * 1. Serversocket anlegen und auf Verbindungen warten
    * 2. Verbindung annehmen und 30-sec-timer starten
    * 3. Sendung empfangen oder ablehnen?
    * 4. Befehl verarbeiten
    *
    *
    * */

    public static void main(String[] args) throws IOException {

        int port = Integer.parseInt(args[0]);

        clients = new ArrayList<>();
        ServerThread currentServerthread;

        try(ServerSocket serverSocket = new ServerSocket(port)){

            System.out.printf("Server auf Port %d aktiviert und bereit\n",port);

            while (true){


                // Needed to handle SHUTDOWN-Command.
                if(! acceptNewConnections) continue;


                // Only MAX_CLIENTS_NUMBER connects allowed. If already a certain number of clients are connect, do not establish new connections.
                if(clientCounter<MAX_CLIENTS_NUMBER ) {
                    try{
                        Socket s = serverSocket.accept();
                        clientCounter++;
                        System.out.printf("Number of clients: %2d\n",clientCounter);

                        // Create a new ServerThread to serve clients requests
                        currentServerthread = new ServerThread(s,clientCounter);
                        currentServerthread.start();
                        clients.add(currentServerthread); // Collect all clients to Handle Shutdown Command.

                        System.out.println("Waiting for next client...");}
                    catch (IOException iox){
                        System.err.println(iox.getMessage());
                    }
                }

            }

        }

    }

    /**
     * Clients call this methode while disconnection from server, to enable other/further client-connections
     */
    public static void clientClosed(){
        synchronized (ich){
            clientCounter--;
            System.out.printf("Number of clients: %2d\n",clientCounter);
        }
    }


    /**
     * A Client call this methode to the server to initialize the SHUTDOWN procedure.
     * First of all, the server refuses new connections.
     * Then the Server tell all the ServerThreads to start the 30-sec timer.
     */
    public static void handleShutdownCommand(){
        // Stop this server from accept new Connections
        synchronized (ich){
            acceptNewConnections = false;
        }

        // Tell all existing ServerThreads to terminate within SHUTDOWN_DELAY if no command are received.
        for(ServerThread st : clients){
            st.initializeShutdownTimer();
        }

    }

    private static void makeSocket(){}

    private static void waitConnection(){}



}
