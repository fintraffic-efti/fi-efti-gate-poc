# Development environment

## Prerequisites
* GNU bash
* Java 21 - https://openjdk.org/install/
* Clojure - https://clojure.org/guides/install_clojure
* Docker / Podman
  * https://docs.docker.com/engine/install/
  * https://podman.io/
* Docker compose v2 - https://docs.docker.com/compose/
* eDelivery harmony - https://github.com/fintraffic-efti/fi-efti-harmony
  * Build docker image: efti/harmony:latest

### Build efti/harmony
```
git clone git@github.com:fintraffic-efti/fi-efti-harmony.git
./fi-efti-harmony/build.sh
``` 

### Firewall

If you have firewall then you may need to allow connection from alb and harmony node to development gate process on host e.g.
```
sudo ufw allow in on br-xxxx to 172.17.0.1 from 172.25.0.0/24
```
where `br-xxxx` is efti network and `172.17.0.1` host ip in docker bridge network,
alb and harmony are in range `172.25.0.0/24`.

## Usage
**Start** environment (from scratch): 
```
./start.sh
```

Start all the services and migrate the databases. 
Do not use this if you have an existing database and breaking migrations see [databases](#Databases).

Environment **down** - remove the environment: 
```
./down.sh
```
This removes everything including the database volumes and networks. 
Note: if you want just stop services see [services](#services).

### Services
* Start services: ```docker compose up --detach```
* Stop services: ```docker compose stop```
* Status: ```docker compose ps```
* Service logs e.g. db: ```docker compose logs db```
* Rebuild mock platform: ```docker compose up --detach --build mock-platform```
* Update fi2 gate: ```../backend/build-docker-image.sh;docker compose down gate-fi2;docker compose up --detach gate-fi2```
* Open bash shell to service: ```docker compose exec alb bash```

### Databases
* Clean databases: ```./flyway.sh clean```
* Migrate databases: ```./flyway.sh migrate```
* Run a breaking migration (clean + migrate): ```./flyway.sh clean; ./flyway.sh migrate```

## Database
* **efti_dev** - An efti gate database for local development
* **efti_template** - An efti gate database for integration test and e2e test template
* **efti_fi2** - An efti gate database for local fi2 gate