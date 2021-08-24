# Reactive Messaging with Tracing example

## Kubernetes Setup

```shell script
minikube start --memory 6144
```

### Apache Kafka

Create a namespace:

```shell script
kubectl create namespace kafka
```

Install the  [Strimzi](https://strimzi.io/) operator:

```shell script
kubectl apply -f 'strimzi-cluster-operator-0.25.0.yaml' -n kafka
```

Create an Apache Kafka cluster:

```shell script
kubectl apply -f kafka_cluster.yml -n kafka
```

Wait for it to be ready:

```shell script
kubectl wait kafka/my-cluster --for=condition=Ready --timeout=300s -n kafka
```

Create the necessary topics:

```shell script
kubectl apply -f kafka_topics.yml -n kafka
```

### PostgreSQL database

Create PostgreSQL database:

```shell script
kubectl apply -f postgresql_kubernetes.yml
```

### Jaeger

Install Jaeger operator:

```shell script
kubectl create namespace observability
kubectl create -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/master/deploy/crds/jaegertracing.io_jaegers_crd.yaml
kubectl create -n observability -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/master/deploy/service_account.yaml
kubectl create -n observability -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/master/deploy/role.yaml
kubectl create -n observability -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/master/deploy/role_binding.yaml
kubectl create -n observability -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/master/deploy/operator.yaml
```

Create All-in-One Jaeger instance:

```shell script
kubectl apply -n observability -f - <<EOF
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: simplest
EOF
```

Install Minikube Ingress for Jaeger UI:

```shell script
minikube addons enable ingress 
```

Retrieve the Jaeger UI URL:

```shell script
kubectl get -n observability ingress
```

## Deploy Application

Utilize Docker inside Minikube:

```shell script
eval $(minikube -p minikube docker-env)
```

Deploy the Account service:

```shell script
cd account-service
mvn verify -Dquarkus.kubernetes.deploy=true
```

Deploy the Overdraft service:

```shell script
cd overdraft-service
mvn verify -Dquarkus.kubernetes.deploy=true
```

Deploy the Transaction service:

```shell script
cd transaction-service
mvn verify -Dquarkus.kubernetes.deploy=true
```

### Delete deployment

Delete a service for redeployment change into the directory and run:

```shell script
kubectl delete -f target/kubernetes/minikube.yml
```

## Use Application

Withdraw $600:

```shell script
TRANSACTION_URL=`minikube service --url transaction-service`
curl -H "Content-Type: application/json" -X PUT -d "600.00" ${TRANSACTION_URL}/transactions/123456789/withdrawal
```

View the traces in the Jaeger UI.
