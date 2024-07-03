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
openssl genrsa -aes256 -passout pass:$pwd -out aap-ca.key 4096

echo "Create CA certificates"
openssl req -new -x509 -days 365 -key root-ca.key -out root-ca.crt -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=Gate CA' -passin pass:$pwd
openssl req -new -x509 -days 365 -key platform-ca.key -out platform-ca.crt -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=Platform CA' -passin pass:$pwd
openssl req -new -x509 -days 365 -key aap-ca.key -out aap-ca.crt -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=AAP CA' -passin pass:$pwd

echo "Create test private keys"
openssl genrsa -out gate-efti-localhost.key 4096
openssl genrsa -aes256 -passout pass:$pwd -out test-platform-fi-1.key 4096
openssl genrsa -aes256 -passout pass:$pwd -out test-platform-fi-2.key 4096
openssl genrsa -aes256 -passout pass:$pwd -out mock-platform.key 4096
openssl genrsa -aes256 -passout pass:$pwd -out aap.key 4096

echo "Create certificate requests"
openssl req -new -key gate-efti-localhost.key -out gate-efti-localhost.csr -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=Gate localhost'
openssl req -new -key test-platform-fi-1.key -out test-platform-fi-1.csr -passin pass:$pwd -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=Test platform fi 1'
openssl req -new -key test-platform-fi-2.key -out test-platform-fi-2.csr -passin pass:$pwd -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=Test platform fi 2'
openssl req -new -key mock-platform.key -out mock-platform.csr -passin pass:$pwd -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=Mock platform'
openssl req -new -key aap.key -out aap.csr -passin pass:$pwd -subj '/C=FI/O=Fintraffic/OU=EFTI/CN=AAP'

echo "Create certificates from requests"
openssl x509 -req -days 365 -in gate-efti-localhost.csr -CA root-ca.crt -CAkey root-ca.key -CAcreateserial \
  -out gate-efti-localhost.crt -extfile ../gate-efti-localhost.cnf -passin pass:$pwd
openssl x509 -req -days 365 -in test-platform-fi-1.csr -CA platform-ca.crt -CAkey platform-ca.key -CAcreateserial \
  -out test-platform-fi-1.crt -extfile ../test-platform-fi-1.cnf -passin pass:$pwd
openssl x509 -req -days 365 -in test-platform-fi-2.csr -CA platform-ca.crt -CAkey platform-ca.key -CAcreateserial \
  -out test-platform-fi-2.crt -extfile ../test-platform-fi-2.cnf -passin pass:$pwd
openssl x509 -req -days 365 -in mock-platform.csr -CA platform-ca.crt -CAkey platform-ca.key -CAcreateserial \
  -out mock-platform.crt -extfile ../mock-platform.cnf -passin pass:$pwd
openssl x509 -req -days 365 -in aap.csr -CA aap-ca.crt -CAkey aap-ca.key -CAcreateserial \
  -out aap.crt -extfile ../aap.cnf -passin pass:$pwd

echo "Create p12 files for chrome client certificates"
openssl pkcs12 -export -inkey test-platform-fi-1.key -in test-platform-fi-1.crt -out test-platform-fi-1.p12 -passin pass:$pwd -passout pass:$pwd
openssl pkcs12 -export -inkey test-platform-fi-2.key -in test-platform-fi-2.crt -out test-platform-fi-2.p12 -passin pass:$pwd -passout pass:$pwd
openssl pkcs12 -export -inkey mock-platform.key -in mock-platform.crt -out mock-platform.p12 -passin pass:$pwd -passout pass:$pwd
openssl pkcs12 -export -inkey aap.key -in aap.crt -out aap.p12 -passin pass:$pwd -passout pass:$pwd
openssl pkcs12 -export -inkey gate-efti-localhost.key -in gate-efti-localhost.crt -out gate-efti-localhost.p12 -passin pass:$pwd -passout pass:$pwd
