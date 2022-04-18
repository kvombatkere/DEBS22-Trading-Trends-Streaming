## DEBS 2022 Grand Challenge - Group 11
### April 15, 2022
Code repository for DEBS 2022 Grand Challenge submission for Group 11.
- DEBS 2022 Grand Challenge information: https://2022.debs.org/call-for-grand-challenge-solutions/
- Evaluation platform and Leaderboard: https://challenge.msrg.in.tum.de/leaderboard
- Short video overview of project: https://youtu.be/UU6BBR7Xo20

### Team Members
Vivek Unnikrishnan, Emmanouil Kritharakis, Karan Vombatkere, Shengyao Luo

## Design Overview
![Operator Design](design.jpeg?raw=true "Operator Design")

The following Flink operators have been designed to handle the stream of input batches. (Please click on the image for an enlarged view of the dataflow and dashboard)
1. **Source API**: Gets new batches from the Challenge Client
2. **Event Generator**: Consume each batch and generates StockMeasurement Events
3. **Query 1: EMA Calculation**: A custom Window operator which emits new EMA values for every 5 minute window for each symbol
4. **Query 1: Benchmark Sideoutput**: A sink for collecting benchmarks for every batch and submitting query 1 results
5. **Query 2: Crossover check**: An operator to check Query 2 requirements of bullish or bearish patterns and find crossover events
6. **Query 2: Benchmark Sideoutput**: A sink for collecting benchmarks for every batch and publishing query 2 results
7. **Close Benchmark**: An operator which waits for Query 1 and Query 2 to complete before ending the benchmark
 
## Requirements
* Java OpenJDK 11
* Apache Maven 
* Apache Flink 1.14.4

## Instructions to Run Code
1. Run the maven command: `clean package` to generate the required jar files: GrpcClient-.jar and GrpcClient-jar-with-dependencies.jar in the _target_ folder.
2. Navigate to the `flink-1.14.3/bin` installation directory and start the Flink cluster by running `./start-cluster.sh` (if running thru the provisioned VMs this will launch a distrbuted flink cluster with 3 worker nodes, and 12 task slots distributed over 12 CPUs)
3. Once the Flink cluster is up and running, access the Flink dashboard via `localhost:8081` in a browser window.
4. Upload the **GrpcClient-.jar** to the Flink dashboard (via the _Submit Job_ tab) and submit the job to run.
5. The progress of the job can be tracked in the **Running Jobs** tab on the Flink dashboard.
