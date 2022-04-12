package stream;

import com.google.protobuf.Timestamp;

import java.io.Serializable;
import java.util.Objects;

/**
 * EmaStream object passed from Query 1 to Query 2
 */
public class EmaStream implements Serializable {

    private String symbol;
    private float ema38DaysValue;
    private float ema100DaysValue;
    private Timestamp lastTradeTime;
    private Long windowId;
    private Integer securityType;
    private Boolean calculate;
    private Long batchSeqId;
    private Boolean batchEnd;
    private Boolean benchMarkSend;
    private Integer totalBenchMarkEvents;

    /**
     * Constructor to create an EMA Stream object
     * @param symbol
     * @param ema38Days
     * @param ema100Days
     * @param lastTradeTime
     * @param processingTime
     * @param securityType
     */
    public EmaStream(String symbol, Ema ema38Days, Ema ema100Days, Timestamp lastTradeTime, Long processingTime, Integer securityType) {
        this.symbol = symbol;
        this.ema38DaysValue = Objects.isNull(ema38Days)?0:ema38Days.getEmaValue();
        this.ema100DaysValue = Objects.isNull(ema100Days)?0:ema100Days.getEmaValue();
        this.lastTradeTime = lastTradeTime;
        this.windowId = processingTime;
        this.securityType = securityType;
        this.calculate = true;
        this.batchEnd = false;
        this.benchMarkSend = false;
        this.totalBenchMarkEvents = 0;
    }

    /**
     * Default constructor
     */
    public EmaStream() {
    }

    public Timestamp getLastTradeTime() {
        return this.lastTradeTime;
    }
    public void setLastTradeTime(Timestamp lastTradeTime) {
        this.lastTradeTime = lastTradeTime;
    }
    public Long getWindowId() {
        return this.windowId;
    }

    public void setwindowId(Long windowId) {
        this.windowId = windowId;
    }

    public Integer getSecurityTypeValue(){
        return this.securityType;
    }

    public void setSecurityTypeValue(Integer securityType){
        this.securityType = securityType;
    }

    public void setWindowId(Long windowId) {
        this.windowId = windowId;
    }

    public Integer getSecurityType() {
        return securityType;
    }

    public void setSecurityType(Integer securityType) {
        this.securityType = securityType;
    }

    public Boolean getCalculate() {
        return calculate;
    }

    public void setCalculate(Boolean calculate) {
        this.calculate = calculate;
    }

    public Boolean getBatchEnd() {
        return batchEnd;
    }

    public void setBatchEnd(Boolean batchEnd) {
        this.batchEnd = batchEnd;
    }

    public Boolean getBenchMarkSend() {
        return benchMarkSend;
    }

    public void setBenchMarkSend(Boolean benchMarkSend) {
        this.benchMarkSend = benchMarkSend;
    }

    public Integer getTotalBenchMarkEvents() {
        return totalBenchMarkEvents;
    }

    public void setTotalBenchMarkEvents(Integer totalBenchMarkEvents) {
        this.totalBenchMarkEvents = totalBenchMarkEvents;
    }

    public Long getBatchSeqId() {
        return batchSeqId;
    }

    public void setBatchSeqId(Long batchSeqId) {
        this.batchSeqId = batchSeqId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public float getEma38DaysValue() {
        return ema38DaysValue;
    }

    public void setEma38DaysValue(float ema38DaysValue) {
        this.ema38DaysValue = ema38DaysValue;
    }

    public float getEma100DaysValue() {
        return ema100DaysValue;
    }

    public void setEma100DaysValue(float ema100DaysValue) {
        this.ema100DaysValue = ema100DaysValue;
    }

}
