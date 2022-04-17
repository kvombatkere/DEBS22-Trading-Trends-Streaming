package application;

import com.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stream.IncomingBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static application.MainApplication.benchmark;

/**
 * Class for GRPC functions
 */
public class GrpcClient implements SourceFunction<IncomingBatch> {

    private static ChallengerGrpc.ChallengerBlockingStub challengeClient;
    public Benchmark newBenchmark;

    private static final Logger logger = LoggerFactory.getLogger(GrpcClient.class);

    /**
     * Create a GRPC client for a specific benchmark
     * @param benchmark
     */
    public GrpcClient(Benchmark benchmark) {
        newBenchmark = benchmark;
        configure();
    }

    /**
     * Default constructor
     */
    public GrpcClient() {
    }


    /**
     * Get the GRPC client
     * @return grpcClient
     */
    public ChallengerGrpc.ChallengerBlockingStub getChallengeClient() {
        return challengeClient;
    }

    /**
     * Configure the GRPC client based on provided paramters
     * @return
     */
    public Benchmark configure() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("challenge.msrg.in.tum.de", 5023)
                .usePlaintext()
                .build();

        // Create a blocking stub because we need to make sure this response is recieved
        challengeClient = ChallengerGrpc.newBlockingStub(channel) //for demo, we show the blocking stub
                .withMaxInboundMessageSize(100 * 1024 * 1024)
                .withMaxOutboundMessageSize(100 * 1024 * 1024);
        MainApplication.client = challengeClient;
        if(Objects.isNull(newBenchmark)) {
            BenchmarkConfiguration bc = BenchmarkConfiguration.newBuilder()
                    .setBenchmarkName("DEBS22-Final-Code-Visualization " + new Date())
                    .addQueries(Query.Q1)
                    .addQueries(Query.Q2)
                    .setToken("azxmuatebdepfrzxbpvusigcnzxuyavi") //go to: https://challenge.msrg.in.tum.de/profile
                    .setBenchmarkType("evaluation") //Benchmark Type for evaluation
                    .build();

            //Create a new Benchmark
            newBenchmark = challengeClient.createNewBenchmark(bc);
        }
        benchmark = newBenchmark;
        logger.debug("Benchmark ID: {}",benchmark.getId());
        MainApplication.benchId = benchmark.getId();
        //Start the benchmark
        challengeClient.startBenchmark(newBenchmark);
        return newBenchmark;
    }

    /**
     * Run the grpc client to get batches of events
     * @param ctx
     * @throws Exception
     */
    @Override
    public void run(SourceContext<IncomingBatch> ctx) throws Exception {
        // Create channel to call to API servers
        if(Objects.isNull(challengeClient)) {
            logger.warn("Challenge client is null...attempting to reconfigure with benchmark id: {}",newBenchmark.getId());
            configure();
        }

        //Process events
        int batch_cnt = 0;
        while (true) {
            Batch batch = challengeClient.nextBatch(newBenchmark);
            Boolean isLastBatch = batch.getLast();
            List<String> lookUpList = new ArrayList<>(batch.getLookupSymbolsList());
            ctx.collect(new IncomingBatch(batch.getEventsList(),batch.getSeqId(),lookUpList,isLastBatch));
            logger.debug("BatchId: {} Started",batch.getSeqId());

            //Stop when we get the last batch
            if (isLastBatch) {
                logger.info("Received last batch, finished processing!");
                break;
            }
            batch_cnt++;
        }
        logger.debug("Number of batches received: {}",batch_cnt);
    }

    @Override
    public void cancel() {
        logger.error("CANCEL CALLED");
    }
}
