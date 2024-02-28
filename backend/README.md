# Backend service for xxxxx xxxx

## Prerequisites
* GNU bash
* Java 21 - https://openjdk.org/install/
* Clojure - https://clojure.org/guides/install_clojure
* See [docker](../docker/README.md)

## Usage

Start development environment: ```../docker/start.sh```.
See more from [docker](../docker/README.md)

### Bash
Start backend for development environment: 
```
env $(cat .env | xargs) ./start.sh
```

Stop backend: `ctrl-c`

### Repl

Start development repl: ```clj -A:dev```

Start backend: ```(start!)```

Stop backend: ```(stop!)```

Exit repl: `ctrl-c` 

### Docker

Build dev image: ```./build-docker-image.sh```.
Run dev image and connect to local development network: 
```
docker run --rm \
--network efti_default \
-p 127.0.0.1:8080:8080 \
-e EFTI_DB_HOST=db \
-e EFTI_DB_USERNAME=efti_gateway \
-e EFTI_DB_PASSWORD=efti \
-e EFTI_DB_DATABASE_NAME=efti_dev \
-e EFTI_GATE_URL=asdf \
efti/backend
```

