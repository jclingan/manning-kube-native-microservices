#!/bin/bash


# Number of calls per endpoint
num_requests=${1:-100}

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

# Get the transaction service URL from minikube
export TRANSACTION_URL=`minikube service --url transaction-service`

# Invoke the imperative, async, and imperative REST Client API endpoints
# (default) 100 times each

post_deposit $TRANSACTION_URL/transactions/444666 $num_requests &
post_deposit $TRANSACTION_URL/transactions/async/444666 $num_requests &
post_deposit $TRANSACTION_URL/transactions/api/444666 $num_requests &

wait
