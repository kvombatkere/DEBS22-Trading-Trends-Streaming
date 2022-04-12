package stream;

import java.io.Serializable;

/**
 * Wrapper class for sending Q1 results for a symbol
 */
public class ResultQ1Wrapper implements Serializable {

    private String symbol;
    private Float ema38Value;
    private Float ema100Value;
    private Long batchSeqId;
    private Integer totalBatchBenchmarkEvents;
    private Boolean lastBatch;
    private Boolean sendBenchmark;

    /**
     * Default constructor
     */
    public ResultQ1Wrapper() {
    }

    /**
     * Create a ResultQ1Wrapper object based on the parameters
     * @param symbol
     * @param ema38Value
     * @param ema100Value
     * @param batchSeqId
     * @param totalBatchBenchmarkEvents
     * @param isLastBatch
     * @param isSendBenchmark
     */
    public ResultQ1Wrapper(String symbol, Float ema38Value, Float ema100Value, Long batchSeqId,Integer totalBatchBenchmarkEvents, Boolean isLastBatch,Boolean isSendBenchmark) {
        this.symbol = symbol;
        this.ema38Value = ema38Value;
        this.ema100Value = ema100Value;
        this.batchSeqId = batchSeqId;
        this.totalBatchBenchmarkEvents = totalBatchBenchmarkEvents;
        this.lastBatch = isLastBatch;
        this.sendBenchmark = isSendBenchmark;
    }


    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Float getEma38Value() {
        return ema38Value;
    }

    public void setEma38Value(Float ema38Value) {
        this.ema38Value = ema38Value;
    }

    public Float getEma100Value() {
        return ema100Value;
    }

    public void setEma100Value(Float ema100Value) {
        this.ema100Value = ema100Value;
    }

    public Long getBatchSeqId() {
        return batchSeqId;
    }

    public void setBatchSeqId(Long batchSeqId) {
        this.batchSeqId = batchSeqId;
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
