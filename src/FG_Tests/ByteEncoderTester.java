package FG_Tests;

public class ByteEncoderTester {


    public static void main(String[] args) {

        String data = "Moin";
        SendPackage sndpkt = new SendPackage(0,data,data.length());

        System.out.printf("SndPkt: SeqNum: %d\n",sndpkt.getBinarySeq());
        System.out.printf("SndPkt: Data: %s\n",sndpkt.getData());
        System.out.printf("SndPkt: checksum: %d\n", sndpkt.getChecksum());


        byte[] pktAsBytes = sndpkt.getBytes();

        SendPackage decodedPkt = SendPackage.extract(pktAsBytes);

        System.out.printf("decodedPkt: SeqNum: %d\n",decodedPkt.getBinarySeq());
        System.out.printf("decodedPkt: Data: %s\n",decodedPkt.getData());
        System.out.printf("decodedPkt: checksum: %d\n", decodedPkt.getChecksum());



        System.out.printf("Checksum sollte sein %d\n", "Moin".length());


        byte checkSumBytes =Integer.valueOf( "Moin".length()).byteValue();
        System.out.printf("ByteValue des HashCode %d\n", checkSumBytes);

        //
//         // sndpkt.extractData().equals(decodedPkt.extractData())
//                // && sndpkt.getBinarySeq()==decodedPkt.getBinarySeq()
//                // &&
//                // !sndpkt.isCorruptet() &&
//                !decodedPkt.isCorruptet()
//                ;



    }
}
