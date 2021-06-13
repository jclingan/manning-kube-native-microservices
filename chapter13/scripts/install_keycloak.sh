#!/bin/bash


SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
OPERATOR_HOME=$SCRIPT_DIR/keycloak-operator

echo "**********************************************"
echo "Setup Ingress (proxy local traffic to cluster)"
echo "**********************************************"

minikube addons enable ingress

echo "**********************************************"
echo "Prepare for Keycloak installation"
echo "**********************************************"


# Install Keycloak Kubernetes Custom Resource Definitions

kubectl apply -f $OPERATOR_HOME/deploy/crds/

# Create keycloak Kubernetes namespace and switch to it

kubectl create namespace keycloak
kubectl config set-context --current --namespace=keycloak

# Define Keycloak Kubernetes role, permissions, and service account

kubectl apply -f $OPERATOR_HOME/deploy/role.yaml
kubectl apply -f $OPERATOR_HOME/deploy/role_binding.yaml
kubectl apply -f $OPERATOR_HOME/deploy/service_account.yaml

# Install the Keycloak Operator, which manages the Keycloack lifecycle 
# and simplifies keycloak configuration using custom resources

kubectl apply -f $OPERATOR_HOME/deploy/operator.yaml

# Wait for operator to start

echo "**********************************"
echo "Waiting for operator to start"
echo "**********************************"

kubectl rollout status deployment keycloak-operator

# Create a keycloak instance

kubectl apply -f $SCRIPT_DIR/keycloak_install.yml

echo "**************************************"
echo "Waiting for keycloak instance to start"
echo "**************************************"

status="false"
while [ "true" != "$status" ];
do
   sleep 2
   status=`kubectl get pod/keycloak-0 -o jsonpath='{.status.containerStatuses[0].ready}'`
done

echo "**************************************"
echo "Setting up keycloak ingress"
echo "**************************************"

kubectl apply -f $SCRIPT_DIR/keycloak_ingress.yml

# Configure the keycloak "Bank" realm, which contains users, roles, and client
# applications used to secure the banking application.

echo "**********************************"
echo "Configuring keycloak \"Bank\" realm"
echo "**********************************"

kubectl apply -f $SCRIPT_DIR/bank_realm.yml

status="false"
while [ "true" != "$status" ];
do
   sleep 2
   status=`kubectl get keycloakrealms/bank -o jsonpath='{.status.ready}'`
done

# Create and configure a "banking client" to access realm.
# The Bank Service and Transaction Service will use the "banking client" to
# access the Bank realm.
# Typically a client would need a credential (ex: secret or certificate)
# to connect to a keycloak realm, but the bank client does not for simplicity

echo "**********************************"
echo "Configuring keycloak bank client"
echo "**********************************"

kubectl apply -f $SCRIPT_DIR/bank_client.yml

# Create "duke" user in the "customer" role in the "Bank" realm,
# and the "quarkus" user in the "teller" role

echo "*************************************"
echo "Create users admin, duke, and quarkus"
echo "*************************************"

kubectl apply -f $SCRIPT_DIR/bank_user.yml

# Switch back to default namespace

kubectl config set-context --current --namespace=default
