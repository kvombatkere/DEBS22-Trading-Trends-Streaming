# DEBS22-Group11
Code repository for DEBS 2022 Grand Challenge Submission for Group 11.

# Pre-requisites
* Java OpenJDK 11
* Apache Maven 
* Apache Flink 1.14.4
 
# Instructions to run

1. Run the maven command `clean package` to generate the required jar files. Two jar files: GrpcClient-.jar and GrpcClient-jar-with-dependencies.jar will be generated in the _target_ folder
2. Navigate to the _flink-1.14.3/bin_ directory and start the cluster using the `./start-cluster.sh` command. Once the Flink cluster is running, access the Flink dashboard via localhost:8081 in a browser window
3. Upload the GrpcClient-.jar to Flink dashboard and submit the job to run.
4. The progress of the job can be tracked in the **Running Jobs** tab on the Flink dashboard.
