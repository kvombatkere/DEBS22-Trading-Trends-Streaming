package stream;

import org.apache.flink.api.common.eventtime.TimestampAssigner;
import org.apache.flink.api.common.eventtime.TimestampAssignerSupplier;

/**
 * Timestamp assigner for stock measurement events generated
 */
public class StockMeasurementWaterMark implements TimestampAssignerSupplier<StockMeasurement>{

    @Override
    public TimestampAssigner<StockMeasurement> createTimestampAssigner(Context context) {
        return (element, recordTimestamp) -> element.getTimestamp();
    }
}
