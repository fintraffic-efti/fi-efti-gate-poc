# Efti gate database

## Prerequisites
* GNU bash
* Java 21 - https://openjdk.org/install/
* Clojure - https://clojure.org/guides/install_clojure
* See [docker](../docker/README.md)

## Usage

Start development environment: ```../docker/start.sh```.
See more from [docker](../docker/README.md)

Available commands:
- **clean** - drop database schemas
- **migrate** - run migrations
- **migrate test** - run migrations with test data

### Bash
Start migration for development environment database `efti_dev`:
```
env $(cat .env | xargs) EFTI_DB_DATABASE_NAME="efti_dev" ./db.sh <command>
```

### Docker

Build dev image: ```./build-docker-image.sh```.
Run dev image and connect to local development network:
```
docker run --rm \
--network efti_default \
--env-file .env \
-e EFTI_DB_HOST="db" \
-e EFTI_DB_DATABASE_NAME="efti_dev" \
-e EFTI_DB_PORT=5432 \
efti/db <command>
```


