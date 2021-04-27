#!/bin/bash

TRANSACTION_URL=${1}
num_requests=${2:-1000}

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

function create_many_blocking_calls() {
  base_url=$1
  requests_per_process=$2
# Deposit funds to the MicroProfile REST Client api endpoint
# Run the many instances in the background to cause the
# concurrent gauge to count simultaneous calls

   post_deposit $base_url/transactions/444666 $requests_per_process &
   post_deposit $base_url/transactions/444666 $requests_per_process &
   post_deposit $base_url/transactions/444666 $requests_per_process &
   post_deposit $base_url/transactions/444666 $requests_per_process &
   post_deposit $base_url/transactions/444666 $requests_per_process &
   post_deposit $base_url/transactions/444666 $requests_per_process &
   post_deposit $base_url/transactions/444666 $requests_per_process &
   post_deposit $base_url/transactions/444666 $requests_per_process &

# Wait for the background processes to stop.

   wait
}

if [ "" == "${TRANSACTION_URL}" ]
then
    # Get the transaction service URL from minikube
    export TRANSACTION_URL=`minikube service --url transaction-service`
fi

echo "Scaling to two transaction-service replicas"

kubectl scale \
    --replicas=2 \
    deployment/transaction-service

echo "Scaling to two transaction-service replicas"
sleep 2

create_many_blocking_calls $TRANSACTION_URL $num_requests

echo "Scaling back to one transaction-service replica"
sleep 2

kubectl scale \
    --replicas=1 \
    deployment/transaction-service

create_many_blocking_calls $TRANSACTION_URL $num_requests
