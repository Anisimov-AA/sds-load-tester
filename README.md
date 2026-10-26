# sds-load-tester

A multithreaded Java load-testing client and the minimal product service on AWS ECS it runs against.
One generator thread feeds a queue, N sender threads post 200,000 products, and the client measures throughput, latency percentiles and how close the scaling is to linear.

**Group members:** Aleksei Anisimov

## Structure

| Folder | What it is |
|---|---|
| `server/` | Spring Boot service with `POST /api/product`. Accepts a product and returns `201 Created`. |
| `terraform/` | ECR repository and ECS Fargate service (region `us-west-2`). |
| `client-single/` | Single-thread test: 10,000 requests one after another, measures the latency of one request. |
| `client1/` | Multithreaded client: 1 generator thread + N sender threads, 200,000 requests. |
| `client2/` | Client 1 plus a latency record for every request, CSV output and statistics. |
| `plot/` | Script that draws throughput over time from the Client 2 CSV. |

## Requirements

Java 17+, Docker, AWS CLI with Learner Lab credentials, Terraform. Python with `matplotlib` for the plot.
Maven is not needed: every Java project has its own `mvnw` wrapper.

## Server

### Run locally

```
cd server
./mvnw spring-boot:run
```

### Deploy to AWS

```
cd terraform
terraform init
terraform apply -target="aws_ecr_repository.app_repo"     # create the image repository first

cd ../server
docker build -t product-service .
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin <account>.dkr.ecr.us-west-2.amazonaws.com
docker tag product-service:latest <account>.dkr.ecr.us-west-2.amazonaws.com/product-service:latest
docker push <account>.dkr.ecr.us-west-2.amazonaws.com/product-service:latest

cd ../terraform
terraform apply
```

### Find the IP and test

The server's public IP: AWS console → ECS → Clusters → `product-service-cluster` → Tasks → the running task → Public IP.

```
curl -i -X POST http://<server-ip>:8080/api/product \
  -H "Content-Type: application/json" \
  -d '{"product_id":42,"sku":"K7Q2M9X1BZ","manufacturer":"Acme Tools","category_id":812,"weight":2350,"some_other_id":9001}'
```

Expected: `201 Created` and `{"product_id":42}`.

### Pause and start again

Stop the running server between sessions but keep everything else (cluster, image, settings).
Ending the Learner Lab does **not** stop a server that is already running.

```
aws ecs update-service --cluster product-service-cluster --service product-service --desired-count 0 --region us-west-2
```

Start it again (with fresh Learner Lab credentials):

```
aws ecs update-service --cluster product-service-cluster --service product-service --desired-count 1 --region us-west-2
```

Each new task gets a new public IP: take it from the console after the task is Running.

### Delete everything from AWS

Removes the ECR repository with the image, the cluster, the service, the security group and the logs.

```
cd terraform
terraform destroy
```

Type `yes` when asked. Expected: `Destroy complete! Resources: 6 destroyed.`

## Clients

All clients take the server address as the first argument (default `http://localhost:8080`).
Client 1 and Client 2 take the number of sender threads as the second argument (default `32`).

```
cd client-single
./mvnw compile exec:java "-Dexec.args=http://<server-ip>:8080"

cd client1
./mvnw compile exec:java "-Dexec.args=http://<server-ip>:8080 32"

cd client2
./mvnw compile exec:java "-Dexec.args=http://<server-ip>:8080 32"
```

On Windows use `.\mvnw` instead of `./mvnw`.

Client 2 writes `latencies.csv` (`start_ms,request_type,latency_ms,status_code`) to its folder and prints mean, median, p99, min and max latency.

## Plot

```
cd plot
pip install matplotlib
python plot.py ../client2/latencies.csv
```

Saves `throughput.png`: requests started in each second of the run.

Result for Client 2 with 32 threads (200,000 requests, about 768 req/s):

![Throughput over time](plot/throughput.png)
