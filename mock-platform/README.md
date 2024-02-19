# Mock Platform


## Usage
```
cd ../docker
./start.sh
```


## Data for api operations that return files

Some files are served from the directory [data](./data).


## Developing mock-platform


### Setup 

Requires python 3.x.

```
cd mock-platform
virtualenv .python
.python/bin/pip install -r ./requirements.txt
```


### Start
```
.python/bin/python src/main/python/mock_platform.py
```
