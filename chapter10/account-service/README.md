```
docker run --ulimit memlock=-1:-1 -it --rm=true --memory-swappiness=0 --name quarkus_banking -e POSTGRES_USER=quarkus_banking -e POSTGRES_PASSWORD=quarkus_banking -e POSTGRES_DB=quarkus_banking -p 5432:5432 postgres:10.5
```