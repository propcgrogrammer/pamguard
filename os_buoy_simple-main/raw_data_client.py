import matplotlib
from matplotlib.figure import Figure
import matplotlib.animation as animation
import matplotlib.pyplot as plt

#from utils.connect import tryRequestPost
import requests

import argparse
import pandas as pd
import numpy as np
from datetime import datetime

f, ax = plt.subplots()
fs = 96000
data_buffer_sec = 10
data_buffer = np.zeros(fs*data_buffer_sec)
x = np.arange(fs*data_buffer_sec)/fs
payload = {}

def getParser():
    parser = argparse.ArgumentParser(description='my description')
    parser.add_argument('-ip', '--ip', default='127.0.0.1')
    parser.add_argument('--delay', default=1, type=float)
    return parser


def animate(url, delay):
    def update(i):
        global payload
        global data_buffer
        global fs
        global x
        r = requests.get(url, json=payload)
        response_json = r.json()
        response_size = response_json[0]['record']
        print("Get data size:", response_size)
        if response_size != 0:
            data = pd.DataFrame(response_json[1:1+response_size])

            if fs != data['fs'].iloc[-1]:
                fs = data['fs'].iloc[-1]
                data_buffer = np.zeros(fs*data_buffer_sec)
                x = np.arange(fs*data_buffer_sec)/fs

            data_buffer_tmp = np.zeros(fs*response_size)
            for i in range(response_size):
                if i == 0:
                    data_buffer_tmp[-(i+1)*fs:] = data['data'].iloc[i]
                else:
                    data_buffer_tmp[-(i+1)*fs:-i*fs] = data['data'].iloc[i]

            data_buffer[-(data_buffer_sec-response_size)*fs:] = data_buffer[0:(data_buffer_sec-response_size)*fs]
            data_buffer[0:response_size*fs] = data_buffer_tmp
            time_stamp = data['time_stamp'].iloc[-1]
            payload = {'time_stamp':time_stamp}
            print("Last time stamp => ", time_stamp)

        print("----------------------------------------")
        

        ax.clear()
        ax.plot(x, data_buffer, '-')
        ax.set_xlabel("Time (sec)")
        ax.set_ylabel("Volt")


    ani = animation.FuncAnimation(f, update, interval=delay*1000)
    plt.show()

if __name__ == '__main__':
    parser = getParser()
    args = parser.parse_args()
    url = "http://"+args.ip+":8000/raw_data/"
    animate(url, args.delay)


