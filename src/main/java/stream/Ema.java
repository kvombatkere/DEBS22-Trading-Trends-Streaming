package stream;

import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class to hold the EMA values
 */
public class Ema implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Ema.class);

    private String symbol;
    private float emaValue;
    private float smoothingFactor;
    private Timestamp timeStamp;
    private Ema previousEmaValue;
    private float lastPriceChange;

    /**
     * Constructor to create a specific EMA object
     * @param symbol
     * @param smoothingFactor
     * @param timeStamp
     * @param previousEmaValue
     * @param lastPriceChange
     */
    public Ema(String symbol, float smoothingFactor, Timestamp timeStamp,Ema previousEmaValue,float lastPriceChange) {
        this.symbol = symbol;
        this.smoothingFactor = smoothingFactor;
        this.timeStamp = timeStamp;
        this.previousEmaValue = previousEmaValue;
        this.lastPriceChange = lastPriceChange;

    }

    /**
     * Default constructor
     */
    public Ema() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public float getEmaValue() {
        return emaValue;
    }

    public void setEmaValue(float emaValue) {
        this.emaValue = emaValue;
    }

    public float getSmoothingFactor() {
        return smoothingFactor;
    }

    public void setSmoothingFactor(float smoothingFactor) {
        this.smoothingFactor = smoothingFactor;
    }

    public Timestamp getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Timestamp timeStamp) {
        this.timeStamp = timeStamp;
    }

    public float getLastPriceChange() {return lastPriceChange;}

    public void setLastPriceChange(float lastPriceChange){this.lastPriceChange = lastPriceChange;}

    /**
     * Calculate the EMA value based on set parameters
     */
    public void calculateEMA() {
        float j = this.smoothingFactor;
        float closePrice = this.lastPriceChange;
        float previousEMA = Objects.isNull(this.getPreviousEmaValue()) ?0:this.getPreviousEmaValue().getEmaValue();
        float currentEMAValue = closePrice * (2 / (1 + j)) + previousEMA * (1 - (2 / (1 + j)));
        logger.info("EMA {}: Symbol {} Previous value: {} Current Value: {}",smoothingFactor,symbol,previousEMA,currentEMAValue);
        this.emaValue = currentEMAValue;
    }

    public Ema getPreviousEmaValue() {
        return previousEmaValue;
    }

    public void setPreviousEmaValue(Ema previousEmaValue) {
        this.previousEmaValue = previousEmaValue;
    }

    @Override
    public String toString() {
        return "Ema{" +
                "symbol='" + symbol + '\'' +
                ", emaValue=" + emaValue +
                ", smoothingFactor=" + smoothingFactor +
                '}';
    }
}
