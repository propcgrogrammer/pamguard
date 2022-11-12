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
import soundfile as sf
from pydantic import BaseModel

logger = logging.getLogger(__name__)
app = FastAPI()
id_number = 0
wav_file_i = 0
url = "127.0.0.1"
return_time_stamp = datetime.now(timezone.utc)
fs = 64000

class RawData(BaseModel):
    time_stamp: datetime = None


@app.get("/connect_pamguard/")
async def getConnection():#連接測試
    #fs = 51200
    return {'status':'success','fs':fs}


@app.get("/raw_data/")
async def getPamGuardData(time_stamp: Optional[datetime]=None):#存進來的資料
    print("[RECEIVE] time_stamp : ", time_stamp)
    global id_number, return_time_stamp, wav_file_i

    #fs = 51200
    #t = np.array(range(0, fs))/fs
    #freq = 10
    #amp = 0.5
    #data = np.sin(2*np.pi*freq*t)*amp

    record = int(random.uniform(0, 4))
    record = 4
    print("[SENT] Amount of return values: ", record)
    if record == 0:
        return [{'record':record}]
    else:
        returnList = [{'record':record}]
        for i in range(record):
            return_time_stamp += timedelta(seconds=1)
            id_number += 1
            
            if wav_file_i >= np.size(y)/fs:
                wav_file_i = 0  # restart wav file

            data = np.flip(y[wav_file_i*fs:(wav_file_i+1)*fs]) #output data need to be flipped
            wav_file_i += 1
            returnValue = {'id': id_number, 'time_stamp':return_time_stamp.isoformat("T", "milliseconds"), 'fs': fs, 'name':'test_data', 'data':data.tolist()}
            print("Return List => ", returnValue['time_stamp'])
            returnList.append(returnValue)
            print("Wav i => ", wav_file_i)
        # print("last time stamp: ", return_time_stamp.isoformat("T", "milliseconds"))
       
        return returnList
        


if __name__ == "__main__":
    wav_file = "HY_20210825_123724.wav"
    y, fs = sf.read(wav_file)
    uvicorn.run(app=app, host="127.0.0.1", port=8000, log_level="info")