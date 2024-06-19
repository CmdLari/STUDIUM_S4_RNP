package Praktikum3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

public class SendBuffer {
    private final FileCopyClient fc;
    private Map<Long, FCpacket> _sendBuffer = new HashMap<>();

    public SendBuffer(FileCopyClient fc) {
        this.fc = fc;
    }

    public synchronized void addPaket(FCpacket fcp) {
        _sendBuffer.put(fcp.getSeqNum(), fcp);
        fcp.setTimestamp(System.nanoTime());
        fc.startTimer(fcp);
    }

    public synchronized FCpacket getPkg(long index) {
        return _sendBuffer.get(index);
    }

    /**
     * True wenn noch pakete fehlen ...
     * @return
     */
    public synchronized boolean missingACK() {
        return _sendBuffer.values().stream().anyMatch(x->!x.isValidACK());
    }

    public synchronized Long getLowestUnsend() {

//        System.err.printf("Länge des Sendbuffers ist: %d\n",_sendBuffer.size());
//        System.err.printf("Sendbuffer enthält die SeqNums: %s\n",_sendBuffer.keySet().stream().map(Object::toString).collect(Collectors.joining(", ")));
//
//
//        if(_sendBuffer.containsKey(1L)){
//            System.err.printf("Das paket 1 im Sendbuffer hat isValidACK: %b\n",_sendBuffer.get(1L).isValidACK());
//        }
//
//        System.err.printf("Sendbuffer enthält die SeqNums: %s\n",_sendBuffer.keySet().stream().map(Object::toString).collect(Collectors.joining(", ")));

        return  _sendBuffer.entrySet().stream()
                .filter(x->!x.getValue().isValidACK())
                .mapToLong(x->x.getKey())
                .sorted()
                .findFirst()
                .orElse((long)_sendBuffer.size()-1);
    }




}
