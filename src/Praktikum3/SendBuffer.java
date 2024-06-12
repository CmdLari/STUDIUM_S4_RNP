package Praktikum3;

import java.util.HashMap;
import java.util.Map;

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
        return _sendBuffer.entrySet().stream()
                .filter(x->!x.getValue().isValidACK())
                .mapToLong(Map.Entry::getKey)
                .sorted()
                .findFirst()
                .orElse((long)_sendBuffer.size()) ;
    }




}
