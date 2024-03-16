#!/usr/bin/env bash
set -e
cd $(dirname $0)

certificate_dir='certificates'
pwd='efti'


if [ -d "$certificate_dir" ]; then
  echo "Certificates are already created in docker/alb/$certificate_dir."
  echo "Remove directory if you want to recreate certificates."
  exit 0
fi

mkdir -p "$certificate_dir"
cd "$certificate_dir"

echo "Create CA private keys"
openssl genrsa -aes256 -passout pass:$pwd -out root-ca.key 4096
openssl genrsa -aes256 -passout pass:$pwd -out platform-ca.key 4096

echo "Create CA certificates"
openssl req -new -x509 -days 365 -key platform-ca.key -out platform-ca.crt -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=Platform CA' -passin pass:$pwd
openssl req -new -x509 -days 365 -key root-ca.key -out root-ca.crt -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=Gate CA' -passin pass:$pwd

echo "Create test private keys"
openssl genrsa -out gate-efti-localhost.key 4096
openssl genrsa -aes256 -passout pass:$pwd -out test-platform.key 4096

echo "Create certificate requests"
openssl req -new -key test-platform.key -out test-platform.csr -passin pass:$pwd -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=Test platform'
openssl req -new -key gate-efti-localhost.key -out gate-efti-localhost.csr -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=Test platform'

echo "Create certificates from requests"
openssl x509 -req -days 365 -in test-platform.csr -CA platform-ca.crt -CAkey platform-ca.key -CAcreateserial \
  -out test-platform.crt -extfile ../test-platform.cnf -passin pass:$pwd
openssl x509 -req -days 365 -in gate-efti-localhost.csr -CA root-ca.crt -CAkey root-ca.key -CAcreateserial \
  -out gate-efti-localhost.crt -extfile ../gate-efti-localhost.cnf -passin pass:$pwd

echo "Create p12 file for chrome client certificate"
openssl pkcs12 -export -inkey test-platform.key -in test-platform.crt -out test-platform.p12 -passin pass:$pwd -passout pass:$pwd
