package application;

import com.google.protobuf.Timestamp;
import com.grpc.*;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.triggers.TriggerResult;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stream.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MainApplication {

    public static long benchId;
    public static ChallengerGrpc.ChallengerBlockingStub client;
    public static Benchmark benchmark;
    public static Boolean visualizationFlag = Boolean.FALSE;
    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
    private static final OutputTag<ResultQ1Wrapper> benchMarkQ1 = new OutputTag<>("benchmark-q1"){};
    private static final OutputTag<ResultQ2Wrapper> benchMarkQ2 = new OutputTag<>("benchmark-q2"){};

    public static void main(String[] args) throws Exception {
        //Set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(20); //set parallelism
        env.setBufferTimeout(10L); //buffer timeout

        Class<?> unmodColl = Class.forName("java.util.Collections$UnmodifiableCollection");
        env.getConfig().addDefaultKryoSerializer(unmodColl, UnmodifiableCollectionsSerializer.class);
        benchmark = new GrpcClient().configure();
        DataStream<IncomingBatch> incomingBatchDataStream = env.addSource(new GrpcClient(benchmark)).name("API");

        //Source Operator
        DataStream<StockMeasurement> measurements = incomingBatchDataStream.flatMap(new EventGenerator(benchmark)).name("Event Generator").setParallelism(10);
        KeyedStream<StockMeasurement,String> stockKeyedStream = measurements
                .assignTimestampsAndWatermarks(WatermarkStrategy.forGenerator(context -> new StockMeasurementWatermarkGenerator()).withTimestampAssigner(new StockMeasurementWaterMark()))
                .keyBy(StockMeasurement::getSymbol);

        //Operator for Q1
        SingleOutputStreamOperator<EmaStream> emaDataStream = stockKeyedStream.process(new StockCustomWindow(TimeUnit.MINUTES.toMillis(5))).name("Query 1: EMA Calculation");

        //Sideoutput for processing Q1 results
        SingleOutputStreamOperator<Boolean> q1ResultsDataStream = emaDataStream.getSideOutput(benchMarkQ1).keyBy(ResultQ1Wrapper::getBatchSeqId).window(GlobalWindows.create())
                .trigger(new ResultQ1Trigger())
                .process(new ResultQ1ProcessWindow(benchmark)).setParallelism(20).name("Q1 Benchmark SideOutput");

        //Operator for Q2
        KeyedStream<EmaStream, String> keyStream = emaDataStream.keyBy(EmaStream::getSymbol);
        SingleOutputStreamOperator<String> q2OutputStream = keyStream.process(new FindBreakPattern()).name("Query 2: Crossover Check");

        //Handle Q2 result
        SingleOutputStreamOperator<Boolean> q2ResultsDataStream = q2OutputStream.getSideOutput(benchMarkQ2).keyBy(ResultQ2Wrapper::getBatchSeqId).window(GlobalWindows.create())
                .trigger(new ResultQ2Trigger())
                .process(new ResultQ2ProcessWindow(benchmark)).setParallelism(20).name("Q2 Benchmark SideOutput");

        //Close Operator: Closes on completion of Q1 and Q2
        q1ResultsDataStream.union(q2ResultsDataStream).countWindowAll(2).apply(new CloseBenchMarkClass(benchmark)).name("Close Benchmark");
        env.execute("DEBS 2022 EMA-Crossover Stream");
    }

    /**
     * Class for defining Window Function of Closing benchmark
     */
    private static class CloseBenchMarkClass implements AllWindowFunction<Boolean, Object, GlobalWindow> {

        private final Benchmark createdBenchMark;

        /**
         * Constructor for the class
         * @param benchmark
         */
        public CloseBenchMarkClass(Benchmark benchmark) {
            this.createdBenchMark = benchmark;
        }

        /**
         * Apply function for applying the window on condition
         * @param globalWindow
         * @param iterable
         * @param collector
         * @throws Exception
         */
        @Override
        public void apply(GlobalWindow globalWindow, Iterable<Boolean> iterable, Collector<Object> collector) throws Exception {
            logger.debug("Obtained last batch events of both Queries....closing benchmark....");
            logger.debug("Sleep for 1s to ensure all processing is completed....");
            Thread.sleep(1000);
            ChallengerGrpc.ChallengerBlockingStub resultClient = null;
            if(Objects.nonNull(client)) {
                resultClient = client;
            } else {
                logger.warn("Client not found....creating a new one.....");
                resultClient = new GrpcClient(createdBenchMark).getChallengeClient();
            }
            resultClient.endBenchmark(benchmark);
            logger.info("Stopped benchmarking....");
        }
    }

    /**
     * Custom window for Stock measurements per key for Q1 processing
     */
    private static class StockCustomWindow extends KeyedProcessFunction<String, StockMeasurement, EmaStream> {

        private final long durationMilliSeconds;
        private transient ValueState<Ema> emaValue38State;
        private transient ValueState<Ema> emaValue100State;
        private transient MapState<Long, StockMeasurement> lastWindowEvent;

        /**
         * Defining a custom window of passed milliseconds
         * @param durationMilliSeconds
         */
        public StockCustomWindow(long durationMilliSeconds) {
            this.durationMilliSeconds = durationMilliSeconds;
        }

        /**
         * On operator creation the steps to be run
         * @param configuration
         */
        @Override
        public void open(Configuration configuration) {
            MapStateDescriptor<Long, StockMeasurement> windowDesc =
                    new MapStateDescriptor<>("windowEvents", Long.class, StockMeasurement.class);
            lastWindowEvent = getRuntimeContext().getMapState(windowDesc);
            ValueStateDescriptor<Ema> emaValue38StateDescriptor = new ValueStateDescriptor<>(
                    "ema38", Ema.class);
            emaValue38State = getRuntimeContext().getState(emaValue38StateDescriptor);
            ValueStateDescriptor<Ema> emaValue100StateDescriptor = new ValueStateDescriptor<>(
                    "ema100", Ema.class);
            emaValue100State = getRuntimeContext().getState(emaValue100StateDescriptor);
        }

        /**
         * On each element added to the window check if it is an end of batch event
         * @param stockMeasurement
         * @param context
         * @param collector
         * @throws Exception
         */
        @Override
        public void processElement(StockMeasurement stockMeasurement, KeyedProcessFunction<String, StockMeasurement, EmaStream>.Context context, Collector<EmaStream> collector) throws Exception {
            long eventTime = stockMeasurement.getTimestamp();
            TimerService timerService = context.timerService();

            if (eventTime <= timerService.currentWatermark()) {
                logger.error("BatchId: {} Symbol: {} Windows has already been Diff: {} Watermark: {} EventTime: {}",stockMeasurement.getBatchSeqId(),stockMeasurement.getSymbol(),timerService.currentWatermark()-eventTime, Instant.ofEpochMilli(timerService.currentWatermark()).toString(),Instant.ofEpochMilli(eventTime).toString());
            } else {
                // Round up eventTime to the end of the window containing this event.
                long endOfWindow = (eventTime - (eventTime % this.durationMilliSeconds) + this.durationMilliSeconds - 1);

                // Schedule a callback for when the window has been completed.
                timerService.registerEventTimeTimer(endOfWindow);

                if(stockMeasurement.getLastEvent()) {
                    if (stockMeasurement.getSendBenchMark()) {
                        String symbolState = stockMeasurement.getSymbol();
                        Float ema38LastState = Objects.isNull(emaValue38State.value())?0: emaValue38State.value().getEmaValue();
                        Float ema100LastState = Objects.isNull(emaValue100State.value())?0: emaValue100State.value().getEmaValue();
                        logger.info("BatchId: {} Symbol {} Send result to sideoutput", stockMeasurement.getBatchSeqId(), symbolState);
                        logger.debug("This is the last event in batch number {} for the symbol {} lastBatch: {}", stockMeasurement.getBatchSeqId(), symbolState, stockMeasurement.getLastBatch());
                        context.output(benchMarkQ1, new ResultQ1Wrapper(symbolState, ema38LastState, ema100LastState, stockMeasurement.getBatchSeqId(), stockMeasurement.getTotalBatchBenchmarkEvents(), stockMeasurement.getLastBatch(), true));
                    }
                    //Handling last Batch event
                    if (stockMeasurement.getLastBatch() && !stockMeasurement.getSendBenchMark()) {
                        logger.debug("BatchId: {} Is the last batch event and the last event of the batch but not subscribe", stockMeasurement.getBatchSeqId());
                        context.output(benchMarkQ1, new ResultQ1Wrapper(stockMeasurement.getSymbol(), null, null, stockMeasurement.getBatchSeqId(), stockMeasurement.getTotalBatchBenchmarkEvents(), stockMeasurement.getLastBatch(), false));
                    }
                    EmaStream emaStream = new EmaStream(stockMeasurement.getSymbol(),emaValue38State.value(), emaValue100State.value(), stockMeasurement.getLastTradeTime(),
                            context.timerService().currentProcessingTime(), stockMeasurement.getSecurityTypeValue());
                    emaStream.setCalculate(false);
                    emaStream.setBatchEnd(stockMeasurement.getLastBatch());
                    emaStream.setBenchMarkSend(stockMeasurement.getSendBenchMark());
                    emaStream.setTotalBenchMarkEvents(stockMeasurement.getTotalBatchBenchmarkEvents());
                    emaStream.setBatchSeqId(stockMeasurement.getBatchSeqId());
                    collector.collect(emaStream);
                }
                StockMeasurement lastEvent = lastWindowEvent.get(endOfWindow);
                if(Objects.isNull(lastEvent) || lastEvent.getTimestamp() < stockMeasurement.getTimestamp()) {
                    lastEvent = stockMeasurement;
                }
                lastWindowEvent.put(endOfWindow,lastEvent);
            }
        }

        /**
         * On timer being triggered calculate the new EMA values for the symbol
         * @param timestamp
         * @param context
         * @param out
         * @throws Exception
         */
        @Override
        public void onTimer(long timestamp, KeyedProcessFunction<String, StockMeasurement, EmaStream>.OnTimerContext context, Collector<EmaStream> out) throws Exception {
            StockMeasurement lastStockMeasurement = lastWindowEvent.get(timestamp);
            String symbol = lastStockMeasurement.getSymbol();
            Ema ema38 = new Ema(symbol, 38, lastStockMeasurement.getLastTradeTime(), emaValue38State.value(), lastStockMeasurement.getLastTradePrice());
            ema38.calculateEMA();
            Ema ema100 = new Ema(symbol, 100, lastStockMeasurement.getLastTradeTime(), emaValue100State.value(), lastStockMeasurement.getLastTradePrice());
            ema100.calculateEMA();
            EmaStream emaStream = new EmaStream(lastStockMeasurement.getSymbol(),ema38, ema100, lastStockMeasurement.getLastTradeTime(),
                    context.timerService().currentProcessingTime(), lastStockMeasurement.getSecurityTypeValue());
            emaValue38State.update(ema38);
            emaValue100State.update(ema100);
            logger.info("Symbol: {} Updating EMA value since the window has ended..... {}",lastStockMeasurement.getSymbol(),new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date(timestamp)));
            out.collect(emaStream);
            lastWindowEvent.remove(timestamp);
            if (visualizationFlag){
                getRuntimeContext().getMetricGroup().addGroup("symbol",emaStream.getSymbol()).addGroup("smoothingFactor","38").gauge("EMA", new Gauge<Float>() {
                    @Override
                    public Float getValue() {
                        return emaStream.getEma38DaysValue();
                    }
                });
                getRuntimeContext().getMetricGroup().addGroup("symbol",emaStream.getSymbol()).addGroup("smoothingFactor","100").gauge("EMA", new Gauge<Float>() {
                    @Override
                    public Float getValue() {
                        return emaStream.getEma100DaysValue();
                    }
                });
            }
        }
    }

    /**
     * Class for generating events from batches
     */
    private static class EventGenerator extends RichFlatMapFunction<IncomingBatch, StockMeasurement> {

        private final Benchmark createdBenchMark;
        private transient ChallengerGrpc.ChallengerBlockingStub resultClient;

        /**
         * Create an operator with benchmark as a parameter
         * @param createdBenchMark
         */
        public EventGenerator(Benchmark createdBenchMark) {
            this.createdBenchMark = createdBenchMark;
        }

        /**
         * Initialize the operator
         * @param parameters
         */
        @Override
        public void open(Configuration parameters) {
            if(Objects.nonNull(client)) {
                this.resultClient = client;
            } else {
                logger.warn("Client not found....creating a new one.....");
                this.resultClient = new GrpcClient(createdBenchMark).getChallengeClient();
            }
        }

        /**
         * Flat map operator to map incoming batch to a list of stock measurement events
         * @param incomingBatch
         * @param output
         * @throws Exception
         */
        @Override
        public void flatMap(IncomingBatch incomingBatch, Collector<StockMeasurement> output) throws Exception {
            Map<String, StockMeasurement> lastKnownEvent = new HashMap<>();
            long batchSeq = incomingBatch.getBatchSeqId();
            logger.debug("BatchId: {} Batch symbols: {} length: {}", batchSeq, incomingBatch.getLookUpSymbolList(),incomingBatch.getLookUpSymbolList().size());

            for (Event event : incomingBatch.getEvents()) {
                String symbol = event.getSymbol();
                float last_price = event.getLastTradePrice();
                Integer securityType = event.getSecurityTypeValue();
                Timestamp lastTradeTime = event.getLastTrade();
                Boolean isSubscribed = false;
                String source = event.getSymbol().split("\\.")[1];
                // Check for subscript symbols to flag them
                if (incomingBatch.getLookUpSymbolList().contains(symbol)) {
                    isSubscribed = true;
                }
                if (lastKnownEvent.containsKey(event.getSymbol())) {
                    output.collect(lastKnownEvent.get(event.getSymbol()));
                }
                lastKnownEvent.put(event.getSymbol(), new StockMeasurement(symbol, source, last_price, lastTradeTime, securityType, isSubscribed, batchSeq,incomingBatch.getLastBatch()));
            }
            //Check the last events for all symbols and flag them before send them to next operator.
            boolean noSubscribtableEvents = true;
            int benchMarkCount = 0;
            for(String symbol:lastKnownEvent.keySet()) {
                lastKnownEvent.get(symbol).setLastEvent(true);
                if(Boolean.TRUE.equals(lastKnownEvent.get(symbol).getSendBenchMark())){
                    noSubscribtableEvents = false;
                    benchMarkCount++;
                }
            }
            for(StockMeasurement stockMeasurement: lastKnownEvent.values()) {
                stockMeasurement.setTotalBatchBenchmarkEvents(benchMarkCount);
                //Handling outlier case that last batch count is 0
                if(incomingBatch.getLastBatch() && noSubscribtableEvents) {
                    logger.debug("Last Batch has no events setting total benchmarks to {}",lastKnownEvent.size());
                    stockMeasurement.setTotalBatchBenchmarkEvents(lastKnownEvent.size());
                }
                output.collect(stockMeasurement);
            }

            //If no lookup symbols in batch, return empty results
            if(noSubscribtableEvents){
                Indicator emptyIndicator = Indicator.newBuilder()
                        .setSymbol("")
                        .setEma38(0)
                        .setEma100(0)
                        .build();
                ResultQ1 submitDataQ1 = ResultQ1.newBuilder()
                        .setBenchmarkId(benchmark.getId())
                        .setBatchSeqId(batchSeq)
                        .addIndicators(emptyIndicator)
                        .build();
                resultClient.resultQ1(submitDataQ1);

                ResultQ2 submitDataQ2 = ResultQ2.newBuilder()
                        .setBenchmarkId(benchmark.getId())
                        .setBatchSeqId(batchSeq)
                        .addAllCrossoverEvents(Collections.emptyList())
                        .build();
                resultClient.resultQ2(submitDataQ2);
            }
            logger.debug("BatchId: {} Completed Event Generation. Count of last events to send: {} Total: {}",batchSeq,benchMarkCount,incomingBatch.getEvents().size());
        }
    }

    /**
     * Window Trigger class for Q1 Results per batch
     */
    private static class ResultQ1Trigger extends Trigger<ResultQ1Wrapper,GlobalWindow> {

        private final ValueStateDescriptor<Integer> eventCount = new ValueStateDescriptor<>("count", Integer.class);

        /**
         * On each element that comes in
         * @param resultQ1Wrapper
         * @param l
         * @param globalWindow
         * @param triggerContext
         * @return
         * @throws Exception
         */
        @Override
        public TriggerResult onElement(ResultQ1Wrapper resultQ1Wrapper, long l, GlobalWindow globalWindow, TriggerContext triggerContext) throws Exception {
            ValueState<Integer> eventCountState = triggerContext.getPartitionedState(eventCount);
            if(Objects.isNull(eventCountState.value())) {
                eventCountState.update(0);
            }
            eventCountState.update(eventCountState.value()+1);
            Integer currentValue = eventCountState.value();
            if(currentValue.equals(resultQ1Wrapper.getTotalBatchBenchmarkEvents())) {
                logger.debug("Q1: Triggering BatchId:{} {} {}",resultQ1Wrapper.getBatchSeqId(),currentValue,resultQ1Wrapper.getTotalBatchBenchmarkEvents());
                return TriggerResult.FIRE_AND_PURGE;
            }
            return TriggerResult.CONTINUE;
        }

        @Override
        public TriggerResult onProcessingTime(long l, GlobalWindow globalWindow, TriggerContext triggerContext) throws Exception {
            return null;
        }

        @Override
        public TriggerResult onEventTime(long l, GlobalWindow globalWindow, TriggerContext triggerContext) throws Exception {
            return null;
        }

        @Override
        public void clear(GlobalWindow globalWindow, TriggerContext triggerContext) throws Exception {

        }
    }

    /**
     * Window Trigger class for Q2 Results per batch
     */
    private static class ResultQ2Trigger extends Trigger<ResultQ2Wrapper,GlobalWindow> {

        private final ValueStateDescriptor<Integer> eventCount = new ValueStateDescriptor<>("count", Integer.class);

        /**
         * On each element that comes in
         * @param resultQ2Wrapper
         * @param l
         * @param globalWindow
         * @param triggerContext
         * @return
         * @throws Exception
         */
        @Override
        public TriggerResult onElement(ResultQ2Wrapper resultQ2Wrapper, long l, GlobalWindow globalWindow, TriggerContext triggerContext) throws Exception {
            ValueState<Integer> eventCountState = triggerContext.getPartitionedState(eventCount);
            if(Objects.isNull(eventCountState.value())) {
                eventCountState.update(0);
            }
            eventCountState.update(eventCountState.value()+1);
            Integer currentValue = eventCountState.value();
            if(currentValue.equals(resultQ2Wrapper.getTotalBatchBenchmarkEvents())) {
                logger.debug("Q2: Triggering BatchId:{} {} {}",resultQ2Wrapper.getBatchSeqId(),currentValue,resultQ2Wrapper.getTotalBatchBenchmarkEvents());
                return TriggerResult.FIRE_AND_PURGE;
            }
            return TriggerResult.CONTINUE;
        }

        @Override
        public TriggerResult onProcessingTime(long l, GlobalWindow globalWindow, TriggerContext triggerContext) throws Exception {
            return null;
        }

        @Override
        public TriggerResult onEventTime(long l, GlobalWindow globalWindow, TriggerContext triggerContext) throws Exception {
            return null;
        }

        @Override
        public void clear(GlobalWindow globalWindow, TriggerContext triggerContext) throws Exception {

        }
    }

    /**
     * Count window process function for Q1 Results
     */
    private static class ResultQ1ProcessWindow extends ProcessWindowFunction<ResultQ1Wrapper,Boolean,Long,GlobalWindow> {

        private transient ChallengerGrpc.ChallengerBlockingStub resultClient;
        private final Benchmark benchmark;

        /**
         * Create a Result Q1 window with benchmark
         * @param benchmark
         */
        public ResultQ1ProcessWindow(Benchmark benchmark) {
            this.benchmark = benchmark;
        }

        /**
         * Initialize variables on operator creation
         * @param parameters
         */
        @Override
        public void open(Configuration parameters) {
            if(Objects.nonNull(client)) {
                this.resultClient = client;
            } else {
                logger.warn("Client not found....creating a new one.....");
                this.resultClient = new GrpcClient(benchmark).getChallengeClient();
            }
        }

        /***
         * Process on batch of benchmarks for Q1
         * @param batchId
         * @param context
         * @param iterable
         * @param collector
         * @throws Exception
         */
        @Override
        public void process(Long batchId, ProcessWindowFunction<ResultQ1Wrapper, Boolean, Long, GlobalWindow>.Context context, Iterable<ResultQ1Wrapper> iterable, Collector<Boolean> collector) throws Exception {
            List<Indicator> indicators = new ArrayList<>();
            Boolean isLastBatch = false;
            Boolean isSendBenchmark = true;
            //Create list of indicators
            for (ResultQ1Wrapper resultQ1 : iterable) {
                if(resultQ1.getSendBenchmark()) {
                    Indicator currentIndicator = Indicator.newBuilder()
                            .setSymbol(resultQ1.getSymbol())
                            .setEma38(resultQ1.getEma38Value())
                            .setEma100(resultQ1.getEma100Value())
                            .build();
                    indicators.add(currentIndicator);
                }
                isLastBatch = resultQ1.getLastBatch();
                isSendBenchmark = resultQ1.getSendBenchmark();
            }
            //Send benchmark
            if(isSendBenchmark) {
                logger.debug("Trigger: Sending Q1 result Batch: {}", batchId);

                // Send results to GRPC client benchmark for Query 1
                ResultQ1 submitData = ResultQ1.newBuilder()
                        .setBenchmarkId(benchId)
                        .setBatchSeqId(batchId)
                        .addAllIndicators(indicators)
                        .build();
                try {
                    resultClient.resultQ1(submitData);
                    logger.info("BatchId: {} Completed sending benchmark Q1", batchId);
                } catch (Exception e) {
                    logger.error("Error: {}", e.getMessage());
                }
            }
            if(isLastBatch) {
                logger.debug("Q1: Finished processing the last batch..... {}",batchId);
                collector.collect(true);
            }
        }
    }

    /**
     * Count window process function for Q2 Results
     */
    private static class ResultQ2ProcessWindow extends ProcessWindowFunction<ResultQ2Wrapper,Boolean,Long,GlobalWindow> {

        private transient ChallengerGrpc.ChallengerBlockingStub resultClient;
        private final Benchmark benchmark;

        /**
         * Create a Result Q2 window with benchmark
         * @param benchmark
         */
        public ResultQ2ProcessWindow(Benchmark benchmark) {
            this.benchmark = benchmark;
        }

        /**
         * Initialize variables on operator creation
         * @param config
         */
        @Override
        public void open(Configuration config) {
            if(Objects.nonNull(client)) {
                this.resultClient = client;
            } else {
                logger.warn("Client not found....creating a new one.....");
                this.resultClient = new GrpcClient(benchmark).getChallengeClient();
            }
        }

        /**
         * Process on batch of benchmarks for Q2
         * @param batchId
         * @param context
         * @param iterable
         * @param collector
         * @throws Exception
         */
        @Override
        public void process(Long batchId, ProcessWindowFunction<ResultQ2Wrapper, Boolean, Long, GlobalWindow>.Context context, Iterable<ResultQ2Wrapper> iterable, Collector<Boolean> collector) throws Exception {
            List<CrossoverEvent> crossOverEvents = new ArrayList<>();
            Boolean isLastBatch = false;
            Boolean isSendBenchmark = true;
            //Create list of crossover events
            for (ResultQ2Wrapper resultQ2 : iterable) {
                if(resultQ2.getSendBenchmark()) {
                    crossOverEvents.addAll(resultQ2.getCrossoverEventList());
                }
                isLastBatch = resultQ2.getLastBatch();
                isSendBenchmark = resultQ2.getSendBenchmark();
            }
            //Send benchmarks
            if(isSendBenchmark) {
                logger.debug("Trigger: Sending Q2 result Batch: {}", batchId);
                ResultQ2 submitData = ResultQ2.newBuilder()
                        .setBenchmarkId(benchId)
                        .setBatchSeqId(batchId)
                        .addAllCrossoverEvents(crossOverEvents)
                        .build();
                try {
                    resultClient.resultQ2(submitData);
                    logger.info("BatchId: {} Completed sending benchmark Q2", batchId);
                } catch (Exception e) {
                    logger.error("Error: {}", e.getMessage());
                }
            }
            if(isLastBatch) {
                logger.debug("Q2: Finished processing the last batch.....{}",batchId);
                collector.collect(true);
            }
            if (visualizationFlag){
                for (ResultQ2Wrapper resultQ2 : iterable) {
                    crossOverEvents.clear();
                    if(Objects.nonNull(resultQ2.getCrossoverEventList())) {
                        crossOverEvents.addAll(resultQ2.getCrossoverEventList());
                    }
                    getRuntimeContext().getMetricGroup().addGroup("q2").gauge("numberCrossoverEvents", new Gauge<Integer>() {
                        @Override
                        public Integer getValue() {
                            return crossOverEvents.size();
                        }
                    });
                }
            }
        }
    }

    /**
     * Class for Q2 finding breakout patterns
     */
    private static class FindBreakPattern extends KeyedProcessFunction<String, EmaStream, String> {

        private transient ValueState<Tuple2<Float, Float>> EMAState;
        private transient ValueState<ArrayList<CrossoverEvent>> last3CrossoversState;

        /**
         * Initialize variables in the operator
         * @param config
         */
        @Override
        public void open(Configuration config) {
            ValueStateDescriptor<Tuple2<Float, Float>> EMAStateDescriptor = new ValueStateDescriptor<>(
                    "EMAState", // the state name
                    TypeInformation.of(new TypeHint<Tuple2<Float, Float>>() {
                    }), // type information
                    Tuple2.of(-1.0F, -1.0F)); // default value of the state, if nothing was set
            EMAState = getRuntimeContext().getState(EMAStateDescriptor);

            ValueStateDescriptor<ArrayList<CrossoverEvent>> last3CrossoversStateDescriptor = new ValueStateDescriptor<>(
                    "last3Crossovers", // the state name
                    TypeInformation.of(new TypeHint<ArrayList<CrossoverEvent>>() {
                    }), // type information
                    new ArrayList()); // default value of the state, if nothing was set
            last3CrossoversState = getRuntimeContext().getState(last3CrossoversStateDescriptor);
        }

        /**
         * Process each element of Emastream object to calculate new crossover events per symbol
         * @param emaStream
         * @param context
         * @param out
         * @throws Exception
         */
        @Override
        public void processElement(EmaStream emaStream, KeyedProcessFunction<String, EmaStream, String>.Context context, Collector<String> out) throws Exception {

            ArrayList<CrossoverEvent> currentList = last3CrossoversState.value();

            //If event is a calculate event
            if(emaStream.getCalculate()) {

                Float EMA38 = emaStream.getEma38DaysValue();
                Float EMA100 = emaStream.getEma100DaysValue();
                Tuple2<Float, Float> currentEMA = Tuple2.of(EMA38, EMA100);
                String advice = null;
                // Check the first time not to check for breakout pattern since there is no stored EMA value yet.
                if (EMAState.value().f0 != -1.0F) {
                    // Check for Bullish & Bearish breakout patterns and if not found pass null.
                    advice = checkBullish(currentEMA);
                    if (advice == null) {
                        advice = checkBearish(currentEMA);
                    }
                }

                EMAState.update(currentEMA);

                //Prepare for a possible CrossoverEvent instance
                Timestamp timestamp = emaStream.getLastTradeTime();
                String symbol = emaStream.getSymbol();
                Integer securityType = emaStream.getSecurityTypeValue();

                // Create a CrossoverEvent only when you have a 'sell' or 'buy' advice and your symbol is subscriptable
                if (advice != null) {
                    CrossoverEvent item = createCrossOverEvent(symbol, advice, securityType, timestamp);
                    // Check if list has already 3 crossover values and remove the oldest one before adding the current one.
                    if (currentList.size() == 3) {
                        currentList.remove(0);
                        currentList.add(item);
                    } else {
                        currentList.add(item);
                    }
                    // Update the state with the list
                    last3CrossoversState.update(currentList);
                }
            } else {
                // Check if batch has finished so Query 2 results should be sent to GRPC server per symbol
                // Check if symbol belong to the subscription list so Query 2 results should be sent to GRPC server
                logger.info("BatchId: {} Sending Q2 Benchmark for Symbol:{}",emaStream.getBatchSeqId(),emaStream.getSymbol());
                if (emaStream.getBatchEnd() && !emaStream.getBenchMarkSend()) {
                    context.output(benchMarkQ2, new ResultQ2Wrapper(emaStream.getBatchSeqId(), null, emaStream.getTotalBenchMarkEvents(), emaStream.getBatchEnd(), false));

                } else if(emaStream.getBenchMarkSend()) {
                    context.output(benchMarkQ2, new ResultQ2Wrapper(emaStream.getBatchSeqId(), currentList, emaStream.getTotalBenchMarkEvents(), emaStream.getBatchEnd(), true));
                }

                if (currentList.size() > 0) {
                    currentList.clear();
                    last3CrossoversState.update(currentList);
                }
            }
        }

        /**
         * Function to check if the current ema is of bullish type
         * @param currentEMA
         * @return
         * @throws IOException
         */
        public String checkBullish(Tuple2<Float, Float> currentEMA) throws IOException {
            String advice = null;
            if (currentEMA.f0 > currentEMA.f1 && EMAState.value().f0 <= EMAState.value().f1) {
                advice = "Buy";
            }
            return advice;
        }

        /**
         * Check if the current EMA is of bearish type
         * @param currentEMA
         * @return
         * @throws IOException
         */
        public String checkBearish(Tuple2<Float, Float> currentEMA) throws IOException {
            String advice = null;
            if (currentEMA.f0 < currentEMA.f1 && EMAState.value().f0 >= EMAState.value().f1) {
                advice = "Sell";
            }
            return advice;
        }

        /**
         * Create cross over events for the supplied parameters
         * @param symbol
         * @param advice
         * @param securityType
         * @param timestamp
         * @return
         */
        public CrossoverEvent createCrossOverEvent(String symbol, String advice, Integer securityType, Timestamp timestamp) {
            return CrossoverEvent.newBuilder()
                    .setTs(timestamp)
                    .setSymbol(symbol)
                    .setSignalType((advice.equals("Sell")) ? CrossoverEvent.SignalType.Sell : CrossoverEvent.SignalType.Buy)
                    .setSecurityType((securityType == 0) ? SecurityType.Equity : SecurityType.Index)
                    .build();
        }
    }

}
