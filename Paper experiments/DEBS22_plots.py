import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import warnings
warnings.filterwarnings('ignore')

# Setting up the font size
font = {'family': 'normal',
        'weight': 'bold',
        'size': 12}

plt.rc('font', **font)


def latency_plot():
    latency_tumbling_window = np.array([806.879, 795.4322, 843.84, 930.7, 1034.33, 1116.73])
    latency_custom_window = np.array([300.59, 316.01, 336.16, 356.26, 352.5, 368.22])
    name = np.array(["3", "6", "9", "12", "15", "18"])
    df_latency_tw = pd.DataFrame(latency_tumbling_window)
    df_latency_cw = pd.DataFrame(latency_custom_window)
    df_name = pd.DataFrame(name)
    columns = ['Tumbling window', 'Custom window', 'Operator Parallelism']
    df = pd.concat([df_latency_tw, df_latency_cw, df_name], axis=1, ignore_index=True)
    df.columns = columns
    ax = df.plot(x="Operator Parallelism", y=["Tumbling window", "Custom window"], kind='bar')
    bars = ax.patches
    for i in range(len(bars)):
        if i < 6:
            bars[i].set_hatch('//')

    plt.xticks(rotation='horizontal')
    plt.legend(columns, fontsize=12)
    plt.ylabel('Time (ms)', fontsize=14)
    plt.xlabel('Operator Parallelism', fontsize=14)
    plt.ylim([0, 1200])
    plt.savefig("Operator_Parallelism_Latency.png")
    plt.show()


def throughput_plot():
    throughput_tumbling_window = np.array([29.18, 40.473, 42.64, 42.3, 40.812, 40.513])
    throughput_custom_window = np.array([29.97, 41.122, 40.63, 38.43, 37.93, 37.4])
    name = np.array(["3", "6", "9", "12", "15", "18"])
    df_throughput_tw = pd.DataFrame(throughput_tumbling_window)
    df_throughput_cw = pd.DataFrame(throughput_custom_window)
    df_name = pd.DataFrame(name)
    columns = ['Tumbling window', 'Custom window', 'Operator Parallelism']
    df = pd.concat([df_throughput_tw, df_throughput_cw, df_name], axis=1, ignore_index=True)
    df.columns = columns
    ax = df.plot(x="Operator Parallelism", y=["Tumbling window", "Custom window"], kind='bar')
    bars = ax.patches
    for i in range(len(bars)):
        if i < 6:
            bars[i].set_hatch('//')
    plt.xticks(rotation='horizontal')
    plt.legend(columns, fontsize=12)
    plt.ylabel('Throughput \n (batches/sec)', fontsize=14)
    plt.xlabel('Operator Parallelism', fontsize=14)
    plt.ylim([0, 55])
    plt.savefig("Operator_Parallelism_Throughput.png")
    plt.show()


def buffer_timeout_latency_plot():
    latency_buffer_timeout = np.array([147.04, 134.76, 180.33, 231.537, 343.23, 459.84, 578.64])
    name = np.array(["5", "10", "25", "50", "100", "150", "200"])
    df_latency_buffer_timeout = pd.DataFrame(latency_buffer_timeout)
    df_name = pd.DataFrame(name)
    df = pd.concat([df_latency_buffer_timeout, df_name], axis=1, ignore_index=True)
    columns = ['Latency (ms)', 'Timeout (ms)']
    df.columns = columns
    ax = df.plot(x='Timeout (ms)', y='Latency (ms)', kind='bar', legend=False)

    plt.xticks(rotation='horizontal')
    plt.ylabel('Latency (ms)', fontsize=14)
    plt.xlabel('Timeout (ms)', fontsize=14)
    plt.savefig("Latency_buffer_Timeout.png")
    plt.show()


if __name__ == '__main__':
    # Experiment to compare custom vs tumbling window
    latency_plot()
    throughput_plot()
    # Experiment to explore buffer timeout hyperparameter
    buffer_timeout_latency_plot()
