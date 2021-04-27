#!/bin/bash

###
### Cause TransactionService BulkheadExceptions and
### CircuitBreaker-related exceptions.
###

TRANSACTION_URL=${1}
num_deposit_bulkhead=${2:-100}
num_deposit_circuit_breaker=${3:-200}

# Bash method to deposit random funds to the specified URL
# a specified number of times

function post_deposit() {
# URL to deposit endpoint
   url=$1

# Number of times to deposit
   num_deposits=$2

   count=0

   while (( count++ < $num_deposits )); do
     curl -s -i \
        -H "Content-Type:application/json" \
        -X POST \
        -d "\""$((1 + $RANDOM % 1000))"\"" \
        $url
   done
   echo
}

if [ "" == "${TRANSACTION_URL}" ]
then
    # Get the transaction service URL from minikube
    export TRANSACTION_URL=`minikube service --url transaction-service`
fi

# Deposit funds (default) 200 times to the MicroProfile REST Client api endpoint
# Run the first 100 in the background to cause a number of
# BulkheadExceptions (more than one simultaneous request)

post_deposit $TRANSACTION_URL/transactions/api/444666 $num_deposit_bulkhead &
post_deposit $TRANSACTION_URL/transactions/api/444666 $num_deposit_bulkhead &

# Wait for the background processes to stop. This ends forcing the
# BulkheadException failures

wait

# Scale account service to no instances to cause CircuitBreakerException
# in the transaction service

echo "******* DISABLING ACCOUNT SERVICE ******"
kubectl scale --replicas=0 deployment/account-service

# While the service is scaling down, invoke (defult) 200 instances which
# should generate WebApplicationException and CircuitBreakerExceptions
# at some point

post_deposit $TRANSACTION_URL/transactions/api/444666 $num_deposit_circuit_breaker

# Scale the account service back to one repica to bring things
# back to normal. Sleep a bit to give time for kubernetes to
# schedule the account service and for it to start

echo "******* RESTARTING ACCOUNT SERVICE ******"
kubectl scale --replicas=1 deployment/account-service

CONTAINER_RUNNING=""

while [ "true" != "$CONTAINER_RUNNING" ]
do
  CONTAINER_RUNNING=`kubectl get pods \
    -lapp.kubernetes.io/name=account-service \
    -o=jsonpath='{.items[*].status.containerStatuses[0].ready}'`
  echo Waiting for new account-service pod to start
  sleep 1
done

# Invoke the MicroProfile REST Client API endpoint 200 times
# Once the transaction service is up and running, these invocations
# should return "200 OK" results

post_deposit $TRANSACTION_URL/transactions/api/444666 $num_deposit_circuit_breaker

