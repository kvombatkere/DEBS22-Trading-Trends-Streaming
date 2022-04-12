package stream;

import com.google.protobuf.ProtocolStringList;
import com.grpc.Event;

import java.io.Serializable;
import java.util.List;

/**
 * Incoming batch event from the GRPC client
 */
public class IncomingBatch implements Serializable {

    private List<Event> events;
    private Long batchSeqId;
    private List<String> lookUpSymbolList;
    private Boolean lastBatch;

    /**
     * Default constructor
     */
    public IncomingBatch(){

    }

    /**
     * Constructor to define incoming batch object based on parameters
     * @param events
     * @param batchSeqId
     * @param lookUpSymbolList
     * @param isLastBatch
     */
    public IncomingBatch(List<Event> events, Long batchSeqId, List<String> lookUpSymbolList,Boolean isLastBatch) {
        this.events = events;
        this.batchSeqId = batchSeqId;
        this.lookUpSymbolList = lookUpSymbolList;
        this.lastBatch = isLastBatch;

    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public Long getBatchSeqId() {
        return batchSeqId;
    }

    public void setBatchSeqId(Long batchSeqId) {
        this.batchSeqId = batchSeqId;
    }

    public void setLookUpSymbolList(ProtocolStringList lookUpSymbolList) {
        this.lookUpSymbolList = lookUpSymbolList;
    }

    public List<String> getLookUpSymbolList() {
        return lookUpSymbolList;
    }

    public void setLookUpSymbolList(List<String> lookUpSymbolList) {
        this.lookUpSymbolList = lookUpSymbolList;
    }

    public Boolean getLastBatch() {
        return lastBatch;
    }

    public void setLastBatch(Boolean lastBatch) {
        this.lastBatch = lastBatch;
    }
}
