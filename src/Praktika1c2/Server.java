package Praktika1c2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
public static final int MAX_CLIENTS_NUMBER = 3;
public static final int MAX_LENGTH = 255;
public static short clientCounter=0;
private static final String helperString ="";
private static ServerSocket serverSocket;
private static boolean acceptNewConnections = true ; // Needed to handle SHUTDOWN command

private static List<ServerThreadCorrected> clients;


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
        ServerThreadCorrected currentServerthread;

        boolean serverSocketClosed = false;

        try{
            serverSocket = new ServerSocket(port);
            System.out.printf("Praktika1c2.Server on port %d aviable \n",port);

            while (acceptNewConnections){

//                System.out.println("PING");

                // Needed to handle SHUTDOWN-Command.
                //if(! acceptNewConnections) continue;


                // Only MAX_CLIENTS_NUMBER connects allowed. If already a certain number of clients are connect, do not establish new connections.
                if(clientCounter<MAX_CLIENTS_NUMBER ) {
                    try{

                        if (serverSocketClosed){
                            serverSocket = new ServerSocket(port);
                            serverSocketClosed = false;
                        }
                        Socket s = serverSocket.accept(); // Hier wartet der Praktika1c2.Server auf neue Clienten
                        clientCounter++;
                        System.out.printf("Number of clients: %2d\n",clientCounter);

                        // Create a new Praktika1c2.ServerThread to serve clients requests
                        currentServerthread = new ServerThreadCorrected(s,clientCounter);
                        currentServerthread.start();
                        clients.add(currentServerthread); // Collect all clients to Handle Shutdown Command.
                        if (clientCounter < MAX_CLIENTS_NUMBER){
                            System.out.println("Waiting for next client...");}
                        else {
                            System.out.println("Currently not accepting more clients");
                        }

                    }
                    catch (IOException iox){
                        System.err.println(iox.getMessage());
                    }

                }
                if(clientCounter >= MAX_CLIENTS_NUMBER){
                    serverSocket.close();
                    serverSocketClosed = true;
                }

            }

            // Warte auf das ende aller verbindungen...
            //while (!clients.isEmpty()){}

            System.out.println("Praktika1c2.Server Socket is shutting down - See you soon!");
        }finally {
            System.out.println("\n");
            serverSocket.close();
        }

    }

    /**
     * Clients call this methode while disconnection from server, to enable other/further client-connections
     */
    public static void clientClosed(){

        synchronized (helperString){
            if (!acceptNewConnections) {
                clientCounter--;
                System.out.printf("Number of clients: %2d\n", clientCounter);
                if (clientCounter <= 0) {
                    System.exit(0);
                }
            } else  {
                clientCounter--;
                System.out.printf("Number of clients: %2d\n", clientCounter);
            }
        }



    }

    /**
     * Clients call this methode while disconnection from server, to enable other/further client-connections
     */
    public static void clientClosedBye(){
        synchronized (helperString){
            clientCounter--;
            System.out.printf("Number of clients: %2d\n",clientCounter);
        }
    }


    /**
     * A Praktika1c2.Client call this methode to the server to initialize the SHUTDOWN procedure.
     * First of all, the server refuses new connections.
     * Then the Praktika1c2.Server tell all the ServerThreadCorrected to start the 30-sec timer.
     */
    public static void handleShutdownCommand(){
        // Stop this server from accept new Connections
        synchronized (helperString){
            acceptNewConnections = false;
        }

        // Tell all existing ServerThreadCorrected to terminate within SHUTDOWN_DELAY if no command are received.
        for(ServerThreadCorrected st : clients){
            st.initializeShutdownTimer();
        }

        try{

            serverSocket.close();
            //if(clients.isEmpty()){ System.exit(0);}
            if (clientCounter==0){ System.exit(0);}
        }

        catch (IOException iox){
            System.out.println("Problem shutting down the server.");
        }
    }
}
