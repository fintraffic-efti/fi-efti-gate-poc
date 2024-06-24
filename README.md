# Fintraffic efti POC

Fintraffic efti proof of concept for efti gateway and platform.

An objective is to an implement efti gateway main features using the best available tools for rapid and agile development.

## Modules

- frontend                     - frontend applications (TODO?)
- [backend](backend/README.md) - efti gateway backend
- [db](db/README.md)           - efti gateway database migration tool
- [docker](docker/README.md)   - common services for local development environment e.g. database
- docs                         - documentation
- .github                      - github actions

## Start

The docker environment uses a harmony image that can be built from
https://github.com/fintraffic-efti/fi-efti-harmony

After that, you can start development environment and efti gateway:

```bash
docker/start.sh && env $(cat backend/.env | xargs) backend/start.sh
```
