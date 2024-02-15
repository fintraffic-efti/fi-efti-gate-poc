# Development environment

## Prerequisites
* Java 21 - https://openjdk.org/install/
* Clojure - https://clojure.org/guides/install_clojure
* Docker / Podman
  * https://docs.docker.com/engine/install/
  * https://podman.io/
* Docker compose v2 - https://docs.docker.com/compose/

## Usage
**Start** environment (from scratch): 
```
./start.sh
```

Start all the services and migrate the databases. 
This cannot be used if you have an existing database and breaking migrations.

Environment **down** - remove environment: 
```
./down.sh
```
This removes everything including the database volumes and networks. 
Note: if you want just stop services see [services](#services).

### Services
* Start services: ```docker compose up --detach```
* Stop services: ```docker compose stop```

### Databases
* Clean databases: ```./flyway.sh clean```
* Migrate databases: ```./flyway.sh migrate```
* Run a breaking migration (clean + migrate): ```./flyway.sh clean; ./flyway.sh migrate```
