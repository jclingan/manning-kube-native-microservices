# Reactive Messaging example

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

### Delete deployment

Delete a service for redeployment change into the directory and run:

```shell script
kubectl delete -f target/kubernetes/minikube.yml
```

## Use Application

Withdraw $600:

```shell script
ACCOUNT_URL=`minikube service --url account-service`
curl -H "Content-Type: application/json" -X PUT -d "600.00" ${ACCOUNT_URL}/accounts/123456789/withdrawal
```

Check the account-fee topic for the generated message:

```shell script
kubectl -n kafka run kafka-consumer -it \
  --image=quay.io/strimzi/kafka:0.25.0-kafka-2.8.0 \
  --rm=true --restart=Never \
  -- bin/kafka-console-consumer.sh \
  --bootstrap-server my-cluster-kafka-bootstrap.kafka:9092 \
  --topic account-fee \
  --from-beginning
```

Verify the updated account balance:

```shell script
curl -X GET ${ACCOUNT_URL}/accounts/123456789
```
