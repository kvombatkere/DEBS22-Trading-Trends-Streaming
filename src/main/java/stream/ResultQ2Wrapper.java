package stream;

import com.grpc.CrossoverEvent;

import java.io.Serializable;
import java.util.List;

/**
 * Wrapper class for sending Q2 results for a symbol
 */
public class ResultQ2Wrapper implements Serializable {

    private Long batchSeqId;
    private List<CrossoverEvent> crossoverEventList;
    private Integer totalBatchBenchmarkEvents;
    private Boolean lastBatch;
    private Boolean sendBenchmark;

    /**
     * Default constructor
     */
    public ResultQ2Wrapper() {
    }

    /**
     * Create a ResultQ2Wrapper object based on the parameters
     * @param batchSeqId
     * @param crossoverEventList
     * @param totalBatchBenchmarkEvents
     * @param isLastBatch
     * @param isSendBenchmark
     */
    public ResultQ2Wrapper(Long batchSeqId, List<CrossoverEvent> crossoverEventList,Integer totalBatchBenchmarkEvents,Boolean isLastBatch, Boolean isSendBenchmark) {
        this.batchSeqId = batchSeqId;
        this.crossoverEventList = crossoverEventList;
        this.totalBatchBenchmarkEvents = totalBatchBenchmarkEvents;
        this.lastBatch = isLastBatch;
        this.sendBenchmark = isSendBenchmark;
    }

    public Long getBatchSeqId() {
        return batchSeqId;
    }

    public void setBatchSeqId(Long batchSeqId) {
        this.batchSeqId = batchSeqId;
    }

    public List<CrossoverEvent> getCrossoverEventList() {
        return crossoverEventList;
    }

    public void setCrossoverEventList(List<CrossoverEvent> crossoverEventList) {
        this.crossoverEventList = crossoverEventList;
    }

    public Integer getTotalBatchBenchmarkEvents() {
        return totalBatchBenchmarkEvents;
    }

    public void setTotalBatchBenchmarkEvents(Integer totalBatchBenchmarkEvents) {
        this.totalBatchBenchmarkEvents = totalBatchBenchmarkEvents;
    }

    public Boolean getLastBatch() {
        return lastBatch;
    }

    public void setLastBatch(Boolean lastBatch) {
        this.lastBatch = lastBatch;
    }

    public Boolean getSendBenchmark() {
        return sendBenchmark;
    }

    public void setSendBenchmark(Boolean sendBenchmark) {
        this.sendBenchmark = sendBenchmark;
    }
}
