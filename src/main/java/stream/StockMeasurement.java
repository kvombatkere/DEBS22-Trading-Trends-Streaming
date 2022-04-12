package stream;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import java.io.Serializable;

/**
 * Class for individual stock measurements
 */
public class StockMeasurement implements Serializable {

    private String symbol;
    private String source;
    private float lastTradePrice;
    private Timestamp lastTradeTime;
    private Integer securityType;
    private Boolean lastEvent;
    private Boolean lastBatch;
    private Boolean subscribed;
    private Boolean sendBenchMark;
    private Integer totalBatchBenchmarkEvents;
    private Long batchSeqId;
    private Long timestamp;

    /**
     * Create a stock measurement object based on the parameters provided
     * @param symbol
     * @param source
     * @param lastTradePrice
     * @param lastTradeTime
     * @param securityType
     * @param isSubscribed
     * @param batchSeqId
     * @param isLastBatch
     */
    public StockMeasurement(String symbol,String source,float lastTradePrice, Timestamp lastTradeTime, Integer securityType,Boolean isSubscribed, Long batchSeqId, Boolean isLastBatch) {
        this.symbol = symbol;
        this.source = source;
        this.lastTradePrice = lastTradePrice;
        this.lastTradeTime = lastTradeTime;
        this.timestamp = Timestamps.toMillis(lastTradeTime);
        this.securityType = securityType;
        this.subscribed = isSubscribed;
        lastEvent = false;
        this.batchSeqId = batchSeqId;
        this.sendBenchMark = false;
        this.totalBatchBenchmarkEvents = 0;
        this.lastBatch = isLastBatch;
    }

    /**
     * Default constructor
     */
    public StockMeasurement() {
    }

    public String getSymbol() {
        return this.symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public float getLastTradePrice() {
        return this.lastTradePrice;
    }

    public void setLastTradePrice(float lastTradePrice) {
        this.lastTradePrice = lastTradePrice;
    }

    public Timestamp getLastTradeTime() {
        return this.lastTradeTime;
    }

    public void setLastTradeTime(Timestamp lastTradeTime) {
        this.lastTradeTime = lastTradeTime;
    }

    public Integer getSecurityTypeValue(){
        return this.securityType;
    }

    public void setSecurityTypeValue(Integer securityType){
        this.securityType = securityType;
    }

    public Boolean getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(Boolean lastEvent) {
        this.lastEvent = lastEvent;
        this.sendBenchMark = this.subscribed && this.lastEvent;
    }

    public Boolean getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(Boolean subscribed) {
        this.subscribed = subscribed;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getBatchSeqId() {
        return batchSeqId;
    }

    public void setBatchSeqId(Long batchSeqId) {
        this.batchSeqId = batchSeqId;
    }

    public Boolean getSendBenchMark() {
        return sendBenchMark;
    }

    public void setSendBenchMark(Boolean sendBenchMark) {
        this.sendBenchMark = sendBenchMark;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getSecurityType() {
        return securityType;
    }

    public void setSecurityType(Integer securityType) {
        this.securityType = securityType;
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
}
