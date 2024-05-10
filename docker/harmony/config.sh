#!/usr/bin/env bash
set -e
cd $(dirname $0)

environment=$1
env_file="$environment/.env"

base64Path() {
  #OSX
  if [[ "$OSTYPE" == "darwin"* ]]; then
    echo $(cat $1 | base64 --break 0)
  else
    echo $(cat $1 | base64 --wrap 0)
  fi
}

rm -f "$env_file"
echo "EFTI_PMODE_BASE64=""$(base64Path $environment/pmode.xml)""" > "$env_file"
echo "EFTI_AP_KEYSTORE_BASE64=""$(base64Path $environment/stores/ap-keystore.p12)""" >> "$env_file"
echo "EFTI_AP_TRUSTSTORE_BASE64=""$(base64Path $environment/stores/ap-truststore.p12)""" >> "$env_file"
echo "EFTI_TLS_KEYSTORE_BASE64=""$(base64Path $environment/stores/tls-keystore.p12)""" >> "$env_file"
echo "EFTI_TLS_TRUSTSTORE_BASE64=""$(base64Path $environment/stores/tls-truststore.p12)""" >> "$env_file"
echo "EFTI_WSPLUGIN_PROPERTIES_BASE64=""$(base64Path $environment/wsplugin.properties)""" >> "$env_file"
