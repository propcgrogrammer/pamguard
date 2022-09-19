# os_buoy_simple
## Enivornment
- Windows10 or MAC OS
- python 3.7.6 above

## Install package
Window  

    $ pip install numpy fastapi uvicorn[standard] requests pydantic soundfile matplotlib

Mac OS

    $ pip install numpy fastapi uvicorn requests


FastAPI and uvicorn are used as the frame  
FastAPI web: https://fastapi.tiangolo.com/zh/  
uvicorn web: https://www.uvicorn.org/


## Run and test
- First Terminal

    $ python simplebuoy_server_db_test.py 

- Second Terminal

    $ python simplebuoy_client.py (example)

- Second Terminal (plot time series data)   
  
    $ python raw_data_client.py
