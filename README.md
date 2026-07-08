# build image

```
docker build -t lynn513/student-management-project:latest .
```



# 推送镜像到 Docker Hub

执行：

```
docker push lynn513/student-management-project:latest
```



然后在 EC2 上：

```
docker rm -f student-management-project

docker pull lynn513/student-management-project:latest
```



# 要先在 EC2 上重新创建应用容器

```
docker run -d \
  --name student-management-project \
  --network student-network \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://student-postgres:5432/studentdb' \
  -e SPRING_DATASOURCE_USERNAME='appuser' \
  -e SPRING_DATASOURCE_PASSWORD='YourStrongPassword123!' \
  -e NAME_AGGREGATION_SERVICE_NAME='April' \
  lynn513/student-management-project:latest
```

然后查看：

```
docker logs -f student-management-project
```



在 EC2 里执行：

```
curl -X POST "http://localhost:8080/name/aggregation" \
  -H "Content-Type: application/json" \
  -H "X-Downstream-Url: http://3.128.166.190:8080/name/aggregation" \
  -d '{"name":["Jessica","Jocelyn","Simon","Suzy"]}'
```

因为你就在这台 EC2 里面测试，所以用：

```
http://localhost:8080
```

最直接。

预期返回：

```
{
  "name": ["Jessica", "Jocelyn", "Simon", "Suzy", "April"]
}
```









# API 设计

现在这个 API 是链式 name aggregation：

```
POST /name/aggregation
Content-Type: application/json
```

Request body 必须是：

```
{
  "name": ["Jessica", "Jocelyn", "Simon", "Suzy"]
}
```

`name` 是 `List<String>`，不能是空 list，里面也不能有空字符串或 blank name。

可选 header：

```
X-Downstream-Url: http://3.128.166.190:8080/name/aggregation
```

这个 header 用来指定“下一个同学”的服务地址。它必须是完整 endpoint URL，不要只写 IP，也不要只写 base URL。

**执行流程**

你的服务收到请求后：

```
1. 读取 body.name
2. copy 一份 name list
3. append 自己的名字：April
4. 如果有 X-Downstream-Url，就调用这个 header 指定的下游
5. 如果没有 X-Downstream-Url，就使用配置里的 DOWNSTREAM_NAME_AGGREGATION_URL
6. 如果下游调用成功，返回下游最终 response
7. 如果下游失败、超时、不可达、返回错误，Circuit Breaker fallback 返回当前结果
```

例如你收到：

```
{
  "name": ["Jessica", "Jocelyn", "Simon", "Suzy"]
}
```

你的服务先变成：

```
{
  "name": ["Jessica", "Jocelyn", "Simon", "Suzy", "April"]
}
```

然后调用 header 或环境变量里的下游地址。

如果 Allen 成功返回：

```
{
  "name": ["Jessica", "Jocelyn", "Simon", "Suzy", "April", "Allen"]
}
```

# Kafka local cluster

This project includes a local Kafka cluster for development and validation.

Start three Kafka brokers:

```
docker compose up -d kafka-1 kafka-2 kafka-3 kafka-init
```

The compose file starts three brokers and creates this topic:

```
student-events
```

Topic settings:

```
partitions=3
replication-factor=3
```

Spring Boot connects to the local cluster with:

```
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:19092,localhost:19093,localhost:19094
APP_KAFKA_TOPIC_NAME=student-events
APP_KAFKA_CONSUMER_GROUP_ID=student-management-consumers
APP_KAFKA_LISTENER_CONCURRENCY=3
```

Produce a message through the backend:

```
curl -X POST "http://localhost:8080/api/kafka/messages" \
  -H "Content-Type: application/json" \
  -d '{"key":"student-1","value":"student-created"}'
```

Check consumed messages:

```
curl "http://localhost:8080/api/kafka/messages/consumed"
```

Describe the topic from Docker:

```
docker exec student-kafka-1 /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka-1:9092 \
  --describe \
  --topic student-events
```

# Local PostgreSQL

Start the local PostgreSQL database:

```
docker compose up -d student-postgres
```

The local database environment variables are:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/studentdb
SPRING_DATASOURCE_USERNAME=appuser
SPRING_DATASOURCE_PASSWORD=YourStrongPassword123!
```

Check database health:

```
docker exec student-postgres pg_isready -U appuser -d studentdb
```

你的服务就把这个完整结果返回给 Suzy。

如果 Allen 调用失败，fallback 返回：

```
{
  "name": ["Jessica", "Jocelyn", "Simon", "Suzy", "April"]
}
```

**优先级**

下游地址优先级是：

```
1. Request header: X-Downstream-Url
2. Environment variable: DOWNSTREAM_NAME_AGGREGATION_URL
3. application.properties 默认值
```

你当前默认值是 Allen：

```
http://3.128.166.190:8080/name/aggregation
```

**curl 示例**

```
curl -X POST "http://localhost:8080/name/aggregation" \
  -H "Content-Type: application/json" \
  -H "X-Downstream-Url: http://3.128.166.190:8080/name/aggregation" \
  -d '{"name":["Jessica","Jocelyn","Simon","Suzy"]}'
```

成功调用 Allen 时应该看到：

```
{
  "name": ["Jessica", "Jocelyn", "Simon", "Suzy", "April", "Allen"]
}
```
