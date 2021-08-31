# Database access with Panache

## Deploying to Kubernetes

### PostgreSQL

Create a Secret for the PostgreSQL database user credentials:

```shell script
kubectl create secret generic db-credentials \
  --from-literal=username=quarkus_banking \
  --from-literal=password=quarkus_banking
```

Install PostgreSQL into Kubernetes:

```shell script
kubectl apply -f postgresql_kubernetes.yml
```

### Deploy application

Choose one of the versions to deploy (jpa, active-record, or data-repository) and change into the directory.
Then run the following to deploy the application:

```shell script
eval $(minikube -p minikube docker-env)
mvn clean package -Dquarkus.kubernetes.deploy=true
```

Use `minikube service list` to find the URL for the `account-service` to access it in a browser.
