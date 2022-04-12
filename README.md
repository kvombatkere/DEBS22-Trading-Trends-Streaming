# DEBS22-Group11
Code repository for DEBS 2022 Grand Challenge Submission for Group 11.

# Prerequisites
* Java OpenJDK 11
* Apache Maven 
* Apache Flink 1.14.4

# Design

![Operator Design](design.jpeg?raw=true "Operator Design")

The following operators have been designed to handle the stream of input batches
1. **Source API**: Gets new batches from the Challenge Client
2. **Event Generator**: Consume each batch and generates StockMeasurement Events
3. **Query 1: EMA Calculation**: A custom Window operator which emits new EMA values for every 5 minute window for each symbol
4. **Query 1: Benchmark Sideoutput**: A sink for collecting benchmarks for every batch and publishing query 1 results
5. **Query 2: Crossover check**: An operator to check Query 2 requirements of bullish or bearish patterns and find crossover events
6. **Query 2: Benchmark Sideoutput**: A sink for collecting benchmarks for every batch and publishing query 2 results
7. **Close Benchmark**: An operator which waits for Query 1 and Query 2 to complete before ending the benchmark
 
# Instructions to run

1. Run the maven command `clean package` to generate the required jar files. Two jar files: GrpcClient-.jar and GrpcClient-jar-with-dependencies.jar will be generated in the _target_ folder
2. Navigate to the _flink-1.14.3/bin_ directory and start the cluster using the `./start-cluster.sh` command. Once the Flink cluster is running, access the Flink dashboard via localhost:8081 in a browser window
3. Upload the GrpcClient-.jar to Flink dashboard and submit the job to run.
4. The progress of the job can be tracked in the **Running Jobs** tab on the Flink dashboard.
