package FG_Tests;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RDT_Sender {

    private static int waitingFor = 0;
    static DatagramSocket socket;
    static DatagramPacket pkg;
    public static final String HOST = "localhost";
    public static final int SENDER_PORT = 6789;
    static int RESEND_DELAY = 1000;


    private static SendPackage rdt_send(String data) {

        System.out.printf("Sender: rdt_send mit %s\n",data);

        SendPackage sndpkt = make_pkt(waitingFor, data);
        udt_send(sndpkt);
        return sndpkt;
    }

    private static void udt_send(SendPackage sndpkt) {
        System.out.printf("Sender: udt_send mit %s\n",sndpkt.getData());




        if(checkSndPkt(sndpkt)){
            System.out.printf("Sender: udt_send : Das Packet wird richtig Codiert und dekodiert.\n");
        }else{
            System.err.printf("Sender: udt_send : Das Packet wird NICHT richtig Codiert und dekodiert.\n");
        }

        try {
            pkg = new DatagramPacket(sndpkt.getBytes(),
                    sndpkt.getBytes().length,
                    InetAddress.getByName(HOST),
                    RDT_Receiver.RECEIVER_PORT
            );
            System.out.printf("Sender: udt_send Versuche zu senden an: HOST: %s, PORT: %d\n",InetAddress.getByName(HOST),RDT_Receiver.RECEIVER_PORT);

            socket.send(pkg);
        }catch (UnknownHostException uhe){
            System.err.println("Schade - sender - udtsend - unknowhost exception");
        }
        catch (IOException e) {
            System.err.println("Schade paket nicht gesendet");
        }
    }

    private static boolean checkSndPkt(SendPackage sndpkt) {

        byte[] pktAsBytes = sndpkt.getBytes();

        SendPackage decodedPkt = SendPackage.extract(pktAsBytes);

        return  // sndpkt.extractData().equals(decodedPkt.extractData())
                // && sndpkt.getBinarySeq()==decodedPkt.getBinarySeq()
                // &&
                // !sndpkt.isCorruptet() &&
                        !decodedPkt.isCorruptet()
                        ;

    }

    private static SendPackage make_pkt(int binarySeq, String data) {
        return new SendPackage(binarySeq, data, data.length());
    }

    private static int rdt_rvc(SendPackage sndpkt) {

        System.out.printf("Sender: rdt_rvc für sndpkt: %s\n",sndpkt.getData());

        byte[] rcvBuffer = new byte[SendPackage.MAX_SIZE];
        DatagramPacket dataPkg = new DatagramPacket(rcvBuffer, SendPackage.MAX_SIZE);
        int nextSeq = waitingFor;
        SendPackage rcvpkt=null;
        while (true) {

            try {
                System.out.printf("Sender: rdt_rvc warte auf Receivers antwort \n");

                socket.receive(dataPkg);


            }catch (SocketTimeoutException ste){
                System.out.printf("Sender: rdt_rvc Receivers antwort nicht. Timeout - resend Paket\n ");
                udt_send(sndpkt);
                continue;
            } catch (IOException e) {
                System.err.println("Schade beim empfange ist etwas kaput gegangen");
                System.exit(-1);
            }

            rcvpkt = SendPackage.extract(rcvBuffer);

            System.out.printf("Sender: rdt_rvc Receivers antwort erhalten \n");


            boolean corruption = rcvpkt.isCorruptet();
            boolean rightSeqNum = rcvpkt.getBinarySeq() == waitingFor;
            boolean acknowledged = isACK(rcvpkt);

            System.out.printf("Sender: rdt_rvc Receivers antwort erhalten - Mit Werten : Richtige Nummer %b, Corrupted %b, ACK: %b \n",rightSeqNum,corruption,acknowledged);


            if ( corruption || !rightSeqNum) {
                System.out.printf("Sender: rdt_rvc Receivers antwort erhalten - fehlerhaft : Erwarte: %d Bekomme: %d, Corrupted %b \n",waitingFor,rcvpkt.getBinarySeq(),rcvpkt.isCorruptet());
                udt_send(sndpkt);
            } else if (acknowledged) {
                System.out.printf("Sender: rdt_rvc Receivers antwort erhalten korrektes ACK, für Seq: %d \n",waitingFor);
                nextSeq = waitingFor==1?0:1;
                System.out.printf("Sender: rdt_rvc Receivers antwort -NächsteSeq: %d \n",nextSeq);
                break;
            }
        }

        return nextSeq;
    }

    private static boolean isACK(SendPackage rcvpkg) {

        if (rcvpkg.getData().equals(SendPackage.ACK_FLAG))
            return rcvpkg.getBinarySeq() == waitingFor;
        else
            return false;
    }

    public static void main(String[] args) {

        System.out.printf("Sender - Starte Client mit SenderPORT: %d\n",SENDER_PORT);


        List<String> conversation = new ArrayList<>();
        conversation.add("============Moin");
        conversation.add("============Wie gehts?");
        conversation.add("============Hast du nicht gesagt!");
        conversation.add("============Das glaube ich nicht.");
        conversation.add("============Er wird schon sehen...");
        conversation.add("============Binominalkoeffizient");
        conversation.add("============Erwache !");
        conversation.add("============Meinst due Wirklich?");
        conversation.add("============Morgen vielleicht");
        conversation.add("============Andere Nachricht");
        //conversation.add("Größte Küchlein ändern"); // Diese Variante von Sender und Receiver mögen kein UTF8 / Umlaute
        conversation.add(SendPackage.BYE_FLAG);


        try {



            socket = new DatagramSocket(SENDER_PORT);
            socket.setSoTimeout(RESEND_DELAY);

            SendPackage sndpkt;

            for(String msg:conversation){
                sndpkt = rdt_send(msg);
                waitingFor = rdt_rvc(sndpkt);

            }


        } catch (SocketException se) {
            System.out.printf("Schade socket ist explodiert\n%s\n",se.getMessage());
        }
//        catch (UnknownHostException uhe){
//            System.out.println("Schade die Adresse für den Socket ist nicht bekannt.");
//        }

        System.out.printf("\t\t ich bin fertig - tschüß\n\t\t SENDER endet normal\n");

    }
}
