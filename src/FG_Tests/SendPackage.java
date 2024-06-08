package FG_Tests;

public class SendPackage {
    public static final int MAX_SIZE = 40;
    public static final String ACK_FLAG = "ACK";
    public static final String BYE_FLAG = "BYE";
    private int binarySeq = -1;
    private String content=null;
    private int checksum = -1;
    public SendPackage(int binarySeq,String data,int checksum){
        this.binarySeq=binarySeq;
        this.content=data;
        this.checksum=checksum;
    }
    public int getBinarySeq(){
        return this.binarySeq;
    }
    public String getData(){
        return this.content;
    }

    public int getChecksum(){
        return this.checksum;
    }

    public boolean isCorruptet(){
        return content.length()!=checksum;
        //return false;
    }



    /**
     * COnvert current SendPackage to byte array with MAX_SIZE length
     * @return
     */
    public byte[]  getBytes(){

        byte[] dataBytes = content.getBytes();
        byte seqBytes = (byte) binarySeq;
        byte checkBytes = (byte) checksum;
        byte contentLenght = (byte) dataBytes.length;

        byte[] allBytes = new byte[dataBytes.length+3];

        allBytes[0] = seqBytes;
        allBytes[1] = checkBytes;
        allBytes[2] = contentLenght;

        System.arraycopy(dataBytes,0,allBytes,3,dataBytes.length);


        return allBytes;

    }

    /**
     * Create new SendPackage from Bytes (ByteBuffer)
     * @param bytes
     * @return
     */
    public static SendPackage extract(byte[] bytes){
        int bytesSeq = (int) bytes[0];
        int bytesCheckSum = (int) bytes[1];
        int contentLength = (int) bytes[2];


        StringBuilder bytesContent = new StringBuilder();
        for(int i = 3 ; i < contentLength + 3 ; i++){
            bytesContent.append((char)bytes[i]);
        }
        return new SendPackage(bytesSeq,bytesContent.toString(),bytesCheckSum);

    }

}
