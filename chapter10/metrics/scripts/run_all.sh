#!/bin/bash

# A simple script to run all tests in
# an infinite loop to generate a "busy"
# dashboard
# Press CTRL-C to exit script

ACCOUNT_URL=`minikube service --url account-service`

while [ 1 ];
do
  metrics/scripts/concurrent.sh
  metrics/scripts/force_multiple_fallbacks.sh
  metrics/scripts/invoke_deposit_endpoints.sh
  metrics/scripts/overload_bulkhead.sh

  count=0

  while (( count++ < 15 )); do
  curl -i $ACCOUNT_URL/accounts/234/balance
  done
done
