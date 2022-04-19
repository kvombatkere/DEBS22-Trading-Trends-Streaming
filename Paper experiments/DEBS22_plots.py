import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

# Setting up the font size
font = {'family': 'normal',
        'weight': 'bold',
        'size': 8}

plt.rc('font', **font)


def latency_plot():
    latency_tumbling_window = np.array([1116.2, 974.91, 829.16, 845.41, 847.51, 884.99, 949.75])
    latency_custom_window = np.array([223.66, 324.4, 330.96, 328.47, 333.05, 336.1, 336.98])
    name = np.array(["1", "2", "4", "6", "8", "10", "12"])
    df_latency_tw = pd.DataFrame(latency_tumbling_window)
    df_latency_cw = pd.DataFrame(latency_custom_window)
    df_name = pd.DataFrame(name)
    columns = ['Tumbling window', 'Custom window', 'Event Generator Parallelism']
    df = pd.concat([df_latency_tw, df_latency_cw, df_name], axis=1, ignore_index=True)
    df.columns = columns
    ax = df.plot(x="Event Generator Parallelism", y=["Tumbling window", "Custom window"], kind='bar')

    plt.xticks(rotation='horizontal')
    plt.legend(columns)
    plt.ylabel('Time (ms)', fontsize=12)
    plt.xlabel('Event Generator Parallelism', fontsize=12)
    plt.ylim([0, 1200])
    plt.savefig("Event_Generator_Parallelism_Latency.png")
    plt.show()


def throughput_plot():
    throughput_tumbling_window = np.array([12.1, 18.38, 32.34, 36.36, 39.92, 41.28, 41.38])
    throughput_custom_window = np.array([15.83, 23.35, 31.98, 38.86, 40.62, 39.7, 39.85])
    name = np.array(["1", "2", "4", "6", "8", "10", "12"])
    df_throughput_tw = pd.DataFrame(throughput_tumbling_window)
    df_throughput_cw = pd.DataFrame(throughput_custom_window)
    df_name = pd.DataFrame(name)
    columns = ['Tumbling window', 'Custom window', 'Event Generator Parallelism']
    df = pd.concat([df_throughput_tw, df_throughput_cw, df_name], axis=1, ignore_index=True)
    df.columns = columns
    ax = df.plot(x="Event Generator Parallelism", y=["Tumbling window", "Custom window"], kind='bar')

    plt.xticks(rotation='horizontal')
    plt.legend(columns)
    plt.ylabel('Throughput (batches/second)', fontsize=12)
    plt.xlabel('Event Generator Parallelism', fontsize=12)
    plt.ylim([0, 50])
    plt.savefig("Event_Generator_Parallelism_Throughput.png")
    plt.show()


def buffer_timeout_latency_plot():
    latency_buffer_timeout = np.array([143.13, 113.38, 164.82, 226.03, 333.05, 450.5, 576.98])
    name = np.array(["5", "10", "25", "50", "100", "150", "200"])
    df_latency_buffer_timeout = pd.DataFrame(latency_buffer_timeout)
    df_name = pd.DataFrame(name)
    df = pd.concat([df_latency_buffer_timeout, df_name], axis=1, ignore_index=True)
    columns = ['Latency (ms)', 'Timeout (ms)']
    df.columns = columns
    ax = df.plot(x='Timeout (ms)', y='Latency (ms)', kind='bar', legend=False)

    plt.xticks(rotation='horizontal')
    plt.ylabel('Latency (ms)', fontsize=12)
    plt.xlabel('Timeout (ms)', fontsize=12)
    plt.savefig("Latency_buffer_Timeout.png")
    plt.show()


if __name__ == '__main__':
    # Experiment to compare custom vs tumbling window
    latency_plot()
    throughput_plot()
    # Experiment to explore buffer timeout hyperparameter
    buffer_timeout_latency_plot()
