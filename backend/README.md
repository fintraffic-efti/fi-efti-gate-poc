# Efti gate backend

Efti gate consists of:
- backend
  - provides platform and competent authority rest apis
  - process edelivery messages from edelivery access point
- edelivery access point

## Prerequisites
* GNU bash
* Java 21 - https://openjdk.org/install/
* Clojure - https://clojure.org/guides/install_clojure
* See [docker](../docker/README.md)

## Usage
* Start development environment: ```../docker/start.sh```
* See more from [docker](../docker/README.md)

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
--env-file .env \
-e EFTI_DB_HOST=db \
-e EFTI_DB_PORT=5432 \
efti/backend
```

Stop backend: `ctrl-c` (container is removed)

### Edelivery schemas
The directory `main/resources/xsd` contains schemas for the edelivery
payloads. If you need to update those, new versions can be copied to
that directory.
