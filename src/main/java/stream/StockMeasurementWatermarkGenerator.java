package stream;

import org.apache.flink.api.common.eventtime.Watermark;
import org.apache.flink.api.common.eventtime.WatermarkGenerator;
import org.apache.flink.api.common.eventtime.WatermarkOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Watermark generator for stock object coming from the GRPC client
 */
public class StockMeasurementWatermarkGenerator implements WatermarkGenerator<StockMeasurement> {

    private final Map<String,Long> sourceTimeStampMap;

    private static final Logger logger = LoggerFactory.getLogger(StockMeasurementWatermarkGenerator.class);

    public StockMeasurementWatermarkGenerator() {
        sourceTimeStampMap = new HashMap<>();
    }

    /**
     * On each event we are updating the max timestamp for each source of events
     * @param event
     * @param eventTimestamp
     * @param output
     */
    @Override
    public void onEvent(StockMeasurement event, long eventTimestamp, WatermarkOutput output) {
        sourceTimeStampMap.put(event.getSource(),Math.max(eventTimestamp,
                sourceTimeStampMap.getOrDefault(event.getSource(),eventTimestamp)));
    }

    /**
     * On default periodic time, we are emitting a new watermark which is the max timestamp of all the available sources
     * @param output
     */
    @Override
    public void onPeriodicEmit(WatermarkOutput output) {
        logger.debug("Emitting max watermark: Timestamp values: {}",sourceTimeStampMap.values().stream().map(Instant::ofEpochMilli).collect(Collectors.toList()));
        Optional<Map.Entry<String,Long>> maxValue = sourceTimeStampMap.entrySet().stream().max(Map.Entry.comparingByValue());
        output.emitWatermark(new Watermark(maxValue.isEmpty()?0:maxValue.get().getValue()));
    }
}
