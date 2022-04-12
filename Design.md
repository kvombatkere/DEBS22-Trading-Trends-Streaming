# 1. Problem Statement
In modern world, the trading on financial markets vary sensitively by precise real time event data accompanied with reliable background information. High-volume streams of event nofitications report demand,supply and other metadata which all contribute to market data. 
The project will address the problem of detecting price variation pattern from the given query and prodvide buy/sell advise for another query given the previous pattern. 

# 2. Proposed Solution
For our solution, we will implement the two queries as outlined by the DEBS Grand Challenge. The first query calculates two exponential moving averages (EMA) on the input com.stream of financial data. The EMA is calculated for 2 different parameters (j=38 and j=100 days) as described in the following equation, where $`w_i`$ corresponds to the time window, and $`j`$ is the smoothing factor for the EMA.

$`EMA_{w_i}^j = \Big[ Close_{w_i}\cdot\big(\frac{2}{1+j}\big)\Big] + EMA_{w_{i-1}}^j  \Big[1 - \big(\frac{2}{1+j}\big)\Big]`$

To implement this query, we will use a five-minute window along with the last observed price from the input data com.stream to calculate streaming EMA values for each five-minute window. Essentially, Query 1 will output a com.stream of EMA data: one for j=38 and j=100 in a form of tuples. The output com.stream of tuples $` t=({EMA}_{w_i}^{38}, {EMA}_{w_i}^{100}) `$ will contain the calculated EMA values of the data streams as per the 5 minute window requirement outlined by the DEBS challenge.

The second query uses the results of query 1 to find bullish and bearish breakout patterns via crossovers. These crossovers are detected by checking inequality relationships between two sequential tuples $` t=({EMA}_{w_i}^{38}, {EMA}_{w_i}^{100}) `$ and $` t'=({EMA}_{w_{i+1}}^{38}, {EMA}_{w_{i+1}}^{100}) `$. The exact specifications of the inequalities the second query must check for, are specified in the DEBS challenge website.

We plan to use Java and Apache Flink to design and implement our solution. The 3 main sequential components of our solution that will interact in a streaming system setting are briefly outlined below:
1. **gRPC Client.** The DEBS challenge provides a protocol buffer and client code template that we will use to implement the gRPC client. This client will be used to access data from the source via API calls, and will also include all the necessary functions needed to directly access elements from the source data com.stream.
2. **Query 1.** The implementation of Query 1 will in the form of an custom Keyed window operator in Flink, and will ingest the data com.stream input from the gRPC client using the Java API. Given the input datastream, we need to keep the close(latest) value per symbol so when the window is fired (a time frame of 5 minutes), the Query 1 calculates the 2 streaming EMA values ($`{EMA}_{w_i}^{38}, {EMA}_{w_i}^{100}`$) as specified by the DEBS challenge.After each time frame the closest value is no longer useful. These 2 EMA values then be sequentially output in a form of a com.stream of tuples $` t=({EMA}_{w_i}^{38}, {EMA}_{w_i}^{100}) `$ to the operator for Query 2.
3. **Query 2.** The implementation of Query 2 will in the form of an KeyedProcessFunction in Flink, and will use the EMA data com.stream from Query 1. In particular, the output com.stream of tuples from Query 1 will we parallelized per symbol and through the KeyedProcessFunction will be executed in order one element(tuple) at a time (a time frame of 5 minutes). For each incoming element $`t_i = (EMA_{w_i}^{38}, EMA_{w_i}^{100})`$, we will calculate the bullish and bearish breakouts based on the incoming $`t_i`$ and the previous element $`t_{i-1}`$, which we would have stored locally. In case of having a crossover, a list with the last 3 crossovers will be updated otherwise it will remain the same. Last but not least, the incoming $`t_{i}`$ element will be stored locally in order to be used for the next upcoming element $`t_{i+1}`$. As output, the updated list with the last 3 crossovers will be given as outlined by the DEBS challenge.

Based on our current understanding of the problem, we can parallelized the computation of EMA values for query 1 and the calculations of the bullish and bearish breakouts for query 2 only per symbol. Since the queries are sequential and the computation of EMA values takes place at the end of the window then any partial results could not be sent across the queries.

# 3. Expectations
The expected results of the proposed solution is to generate the pattern of trading price trends and implement it to suggest whether there will be a buy/sell advise. The **first query** deals with _identifying trends in price movements for individual equities using event aggregation over tumbling windows_ and the **second query** includes _triggering buy/sell advises using complex event processing upon detecting specific patterns_. By processing the Query 1, the output of the first operator will be sent to the second operator as the input, which is a datastream of tuples including both EMA values of 38 and 100 respectively describing each 5 time window frame. The purpose of processing Query 2 is to make conclusion for each tumbling window by comparing the EMA values of the current window to those of the previous window. Based on that, we give advise whether to buy or sell on that price. 


# 4. Experimental Plan
The experimental plan for this project involves first understanding the data format, the requirements of the queries, and a high-level overview of the system design. We then plan to proceed as follows:
- Implement the gRPC client outline and start testing the API with streaming data.
- Access the event price data using the Java API. 
- Understand how to integrate system and results with DEBS challenge benchmarking system.
- Provision Flink resources on DEBS provided VMs.
- Design a high-level implementation of the 3 sequential components of the solution.
- Design and write pseudocode for operators for Query 1 and Query 2 using Flink.
- Start implementing Query 1 and Query 2 in parallel (in pairs outlined in Task Assignment), with the goal of having a MVP system that can run end to end and output results.
- Test and check system for correctness and accuracy end-to-end.
- Iterate and improve on solution in terms of correctness and accuracy metrics as outlined by DEBS challenge.

We plan to integrate this rough experimental plan with the course requirements by having an MVP completed by the midterm demo date. This will help us align timelines and give us time to iterate and improve on our solution.


# 5. Success Indicators
The expected outcome of our solution is the implementation of the two specified queries that run correctly and efficiently. Query 1 includes identifying trends in price movements for individual equities using event aggregation over tumbling windows. Query 2 includes triggering buy/sell advises using complex event processing upon detecting specific patterns. Success will be measured by the tests the DEBS Grand Challenge provides for us to utilize as well as our ranking compared to other teams. Our intermediate milestones include getting data from the API provided by the DEBS Grand Challenge by configuring Java gRPC to query the data from this API, completing operators for Query 1 and Query 2, running the Flink application with no congestion or crashes, and getting our solution accepted by the competition.

### Intermediate milestones
1. Understanding what we need to do, process the results.
2. Communicating with the API and streaming the data through gRPC and respond back a barebone list of crossovers (Until 14.03.22 DEMO deadline)
3. Get a working solution running. 
4. Having good accuracy, completing a rudimentary MVP Flink application. (Until 05.04.22 Final deadline)


# 6. Task assignment
We plan to use a pair-programming method to split up the project tasks and work in pairs on Query 1 and Query 2. The goal is to fully understand and think through the requirements of each query, and how they relate to the streaming data, which will be accessed by the gRPC API. Thus each pair will work on both the Operator design and implementation for one query.

- **Query 1.** Shengyao Luo, Vivek Unnikrishnan
- **Query 2.** Emmanouil Kritharakis, Karan Vombatkere

We plan to work through the gRPC client development and understanding the source ingestion framework together, so that each member of the team has a good understanding of the data flowing in through the gRPC source. We will then follow the rough steps outlined in the experimental plan section to implement and test the system end-to-end.
