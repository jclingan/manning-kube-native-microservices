#!/bin/bash

###
### Cause TransactionService BulkheadExceptions. Not 100% gauranted,
### but likely to cause them.
###

TRANSACTION_URL=${1:-"http://localhost:8088"}
num_requests=${2:-100}

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

# Deposit funds (default) 200 times to the MicroProfile REST Client
# api endpoint
# Run the first 100 in the background to cause a number of
# BulkheadExceptions (more than one simultaneous request)

post_deposit $TRANSACTION_URL/transactions/api/444666 $num_requests &
post_deposit $TRANSACTION_URL/transactions/api/444666 $num_requests &

# Wait for the background processes to stop.

wait
