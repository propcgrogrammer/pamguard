import requests
from datetime import datetime, timezone
from time import time


def sendTimeStamp():
    url = "http://127.0.0.1:8000/raw_data/"
    Time_Stamp = {'time_stamp':datetime.now(timezone.utc).isoformat("T", "milliseconds")}
    r = requests.get(url, Time_Stamp)
    if r != -1:
        print("return value = ", r.text)
        print("========================================")

if __name__ == "__main__":
    sendTimeStamp()
