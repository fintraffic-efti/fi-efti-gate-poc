# Fintraffic efti POC

Fintraffic efti proof of concept for efti gate. More information about efti: 
- https://efti.fi 
- https://efti4eu.eu/

An objective is to implement efti gate main features using the best available tools for rapid and agile development.

:exclamation: This is not intended for production use and this is not an official reference implementation. 
The main goal is to provide the simplest possible gate implementation for pilot projects.

Official reference implementation is here: https://github.com/EFTI4EU/reference-implementation

## Modules

- frontend                     - frontend applications (TODO?)
- [backend](backend/README.md) - efti gate backend
- [db](db/README.md)           - efti gate database migration tool
- [docker](docker/README.md)   - common services for local development environment e.g. psql database and edelivery aps
- docs                         - documentation
- .github                      - github actions

## Start

Get prerequisites for local environment and gate backend see:
- [docker](docker/README.md#prerequisites)
- [backend](backend/README.md#prerequisites) 

Start development environment and efti gate:

```bash
docker/start.sh && env $(cat backend/.env | xargs) backend/start.sh
```

Add client certificates (*.p12 files) to browser from: `docker/alb/certificates`. Password: `efti`.

Gate services are available using swagger ui from:
* Gate FI1 (dev gate on host)
  * [Platform API](https://platform.gate.efti.fi1.localhost:8888/api/documentation)
  * [Competent authority (CA) API](https://aap.gate.efti.fi1.localhost:8888/api/documentation)
* Gate FI2 (other gate instance in docker environment)
  * [Platform API](https://platform.gate.efti.fi2.localhost:8888/api/documentation)
  * [Competent authority (CA) API](https://aap.gate.efti.fi2.localhost:8888/api/documentation)

From domain `platform.gate.efti.fiN.localhost` platform api end points are available using platform client certificates
and from `aap.gate.efti.fiN.localhost` the CA API endpoints are accessible using aap.p12 client certificate.

For eFTI platforms requiring client certificate authentication, add
the certificate base64 encoded to environment parameter
`EFTI_GATE_CLIENT_CERTIFICATE` and the password to
`EFTI_GATE_CLIENT_CERTIFICATE_PASSWORD`.

Gate FI1 and FI2 form gate network using eDelivery.

## License

Distributed under the EUPL 1.2 License. See [EUPL-1.2](EUPL-1.2-EN.txt) for more information.
