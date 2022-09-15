"""
Arthur: PK-Chen
Purpuse: buoy server simple version
Date: 2022/4/27
"""
import uvicorn
from fastapi import FastAPI
import logging
from datetime import datetime
import numpy as np
from typing import Optional
from datetime import datetime, timezone, timedelta
import random

logger = logging.getLogger(__name__)
app = FastAPI()
id_number = 0
url = "127.0.0.1"
return_time_stamp = datetime.now(timezone.utc)

@app.get("/connect_pamguard/")
async def getConnection():#連接測試
    fs = 51200
    return {'status':'success','fs':fs}

@app.post("/raw_data/")
async def getPamGuardData(time_stamp: Optional[datetime]=None):#存進來的資料
    print("[RECEIVE] time_stamp : ", time_stamp);
    global id_number, return_time_stamp
    fs = 51200
    t = np.array(range(0, fs))/fs
    freq = 10
    amp = 0.5
    data = np.sin(2*np.pi*freq*t)*amp

    record = int(random.uniform(0, 4))
    print("[SENT] Amount of return values: ", record)
    if record == 0:
        return [{'record':record}]
    else:
        returnList = [{'record':record}]
        for i in range(record):
            return_time_stamp += timedelta(seconds=1)
            id_number += 1
            returnValue = {'id': id_number, 'time_stamp':return_time_stamp.isoformat("T", "milliseconds"), 'fs': fs, 'name':'test_data', 'data':data.tolist()}
            print("Return List => ", returnValue['time_stamp'])
            returnList.append(returnValue)
        # print("last time stamp: ", return_time_stamp.isoformat("T", "milliseconds"))
       
        return returnList
        


if __name__ == "__main__":
    uvicorn.run(app=app, host="127.0.0.1", port=8000, log_level="info")