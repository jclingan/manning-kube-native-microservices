#!/bin/bash

# "http://keycloak.local/auth/realms/bank" is a publicly available endpoint
# that makes the realm public key publicly available.

# The "cut" command prints out text between the 8th and ninth quote,
# which is the public key. Using "cut" is convenient way to parse the JSON
# without installing specil tooling. Good enough for this purpose!

BEGIN_STRING="-----BEGIN PUBLIC KEY-----"
PUBLIC_KEY=`curl -s http://keycloak.local/auth/realms/bank | cut -d\" -f8`
END_STRING="-----END PUBLIC KEY-----"

echo $BEGIN_STRING
echo $PUBLIC_KEY
echo $END_STRING
