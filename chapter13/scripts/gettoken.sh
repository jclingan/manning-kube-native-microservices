#!/bin/bash

if [ "$1" = "remote" ];
then

# Run CURL from within the container to obtain a token with "keycloak" issuer,
# which maps to the host name in the container

kubectl run curl \
    -n keycloak \
    -i \
    --image=curlimages/curl:7.76.0 \
    --rm \
    --tty \
    --restart=Never \
    --command -- \
      curl -X POST -s -k https://keycloak:8443/auth/realms/bank/protocol/openid-connect/token \
           --user bank:bank \
           -H 'content-type: application/x-www-form-urlencoded' \
           -d 'username=duke&password=duke&grant_type=password' |\
    cut -d\" -f4    
else

# Run CURL from localhost to obtain a token with "localhost" issuer,
# which maps to the host name in the container

    curl -X POST -s -k http://keycloak.local/auth/realms/bank/protocol/openid-connect/token \
           --user bank:bank \
           -H 'content-type: application/x-www-form-urlencoded' \
           -d 'username=duke&password=duke&grant_type=password' |\
    cut -d\" -f4
fi

