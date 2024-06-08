package FG_Tests;

import java.io.IOException;
import java.net.*;

public class RDT_Receiver {

    static enum STATES {
        RUNNING ("RUNNING"),
        BYE("BYE");
        private final String asString;
        private STATES (String name){
            this.asString=name;
        };
        @Override
        public String toString(){
            return asString;
        }
    }

    private static STATES currentState = STATES.RUNNING;

    static int waitingFor = 0;

    static DatagramSocket listenerSocket;
    static DatagramPacket pkg;

    public static final int RECEIVER_PORT = 5678;

    private static int shouldFailCounter = 0 ;
    private static int shouldFailNum = 2 ;


    private static void udt_send(SendPackage sndpkt) {
        System.out.printf("Receiver - udt_send - mit sndpkg %s\n ",sndpkt.getData());
        try {
            pkg = new DatagramPacket(
                    sndpkt.getBytes(),
                    sndpkt.getBytes().length,
                    InetAddress.getByName(RDT_Sender.HOST),
                    RDT_Sender.SENDER_PORT
            );

            System.out.printf("Receiver - udt_send - Versuche zu senden An: HOST: %s, PORT: %d\n ",InetAddress.getByName(RDT_Sender.HOST),RDT_Sender.SENDER_PORT);


            //senderSocket.send(pkg);
            listenerSocket.send(pkg);
        } catch (UnknownHostException uhe){
            System.err.println("Schade - Receiver - sendersocket - unknowhost exteption");
        }catch ( IOException ioe) {
            System.err.println("Schade paket nicht gesendet");
        }
    }


    private static int rdt_rcv(){

        System.out.printf("Receiver: rdt_rcv - waitingFor Seq : %d \n",waitingFor);

        int nextSeq = waitingFor;

        byte[] rcvBuffer = new byte[SendPackage.MAX_SIZE];
        DatagramPacket dataPkg = new DatagramPacket(rcvBuffer, SendPackage.MAX_SIZE);

        SendPackage rcvpkt=null;
        SendPackage responsePkt;
        String data = SendPackage.ACK_FLAG;

            try {
                System.out.printf("Receiver: rdt_rcv - Lausche Am ListenerSocket, PORT: %d auf eingehende daten... \n",RECEIVER_PORT);
                listenerSocket.receive(dataPkg);
            } catch (IOException e) {
                System.err.println("Schade beim empfange ist etwas kaput gegangen");
                System.exit(-1);
            }

            rcvpkt = SendPackage.extract(rcvBuffer);

            System.out.printf("Receiver: rdt_rcv - Nachricht des Sender Erhalten \n");

        boolean corruption = rcvpkt.isCorruptet();
        boolean rightSeqNum = rcvpkt.getBinarySeq() == waitingFor;

        System.out.printf("Receiver: rdt_rvc Receivers antwort erhalten - Mit Werten : Richtige Nummer %b, Corrupted %b  \n",rightSeqNum,corruption);



        // Waiting for 0 and receiving 1
            if ( corruption || !rightSeqNum) {
                System.out.printf("Receiver: rdt_rcv - Nachricht des Sender ist fehlerhaft , Erwarte Seq: %d, Bekomme: %d, Corrupted: %b \n",waitingFor,rcvpkt.getBinarySeq(),rcvpkt.isCorruptet());
                responsePkt = new SendPackage(waitingFor,data,data.length());

                udt_send(responsePkt);

            }else{ // Waiting for 1 and receiving 1

                if(shouldFailCounter == shouldFailNum){
                    System.out.printf("Receiver: Simuliere Pakte verlust ...\n");
                    shouldFailCounter = 0;
                    return nextSeq;
                }
                shouldFailCounter++;

                System.out.printf("Receiver: rdt_rcv - Nachricht des Sender ist in Ordnung \n");

                deliver_data(rcvpkt.getData());
                responsePkt = new SendPackage(waitingFor,data,data.length());
                udt_send(responsePkt);
                nextSeq = waitingFor==1?0:1;
                System.out.printf("Receiver: rdt_rcv - nächste Seq soll sein : %d \n",nextSeq);
            }

        return nextSeq;
    }

    private static void deliver_data(String s) {


        System.out.printf("Receiver: deliver_data -  %s\n",s);

        if(s.equals(SendPackage.BYE_FLAG)){
            System.out.printf("Receiver: deliver_data - Werde beenden \n");
            currentState = STATES.BYE;

        }

    }

    public static void main(String[] args) {

        System.out.printf("Receiver: Warte auf den Sender... \n");

        try {
            listenerSocket = new DatagramSocket(RECEIVER_PORT);
            //senderSocket = new DatagramSocket();
            while (currentState == STATES.RUNNING){

               waitingFor = rdt_rcv();

            }
            System.out.printf("Good bye\n");
        }catch (SocketException soe){
            System.err.println("Am Arsch - kein Socket erstellt. ");
        }
//        catch (UnknownHostException uhe){
//            System.out.println("Schade die Adresse für den Socket ist nicht bekannt.");
//        }


    }











}
