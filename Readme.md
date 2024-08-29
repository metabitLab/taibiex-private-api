# Taibiex Private API

## Introduction

## Contribution

We use the DGS framework to build the GraphQL API. The DGS framework is a GraphQL framework for Spring Boot. It is a lightweight, modular framework that allows you to build GraphQL servers with ease.

There are two parts. The first part is the graphql API to provide data to the frontend. The second part is the subgraphs client to fetch data from the subgraphs services.

### Deployment Environment

- JDK 17
- Maven 3.8.6
- MySQL 8.4.2
- AWS MemoryDB for Redis 6(Just production)
- [Redis-stack 6.2.6-v15 (Just development)](https://github.com/redis-stack/redis-stack/releases?q=6.2.6-v15&expanded=true). The Redis-stack includes Redis.

#### 初始化 MySQL 环境

```shell
# 创建配置目录
mkdir -p mysql.conf.d
# 创建数据存储目录
mkdir -p mysql-data

# 创建 my.cnf 文件
cat << EOL > mysql.conf.d/my.cnf
[mysqld]
max_connections = 20
sql_mode = STRICT_TRANS_TABLES
innodb_buffer_pool_size = 1G
EOL
```

```shell
# 启动 MySQL 容器
docker run \
  -d \
  --name taibiex-mysql-container \
  -v ${PWD}/mysql.conf.d/my.cnf:/etc/mysql/my.cnf \
  -v ${PWD}/mysql-data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -p 3306:3306 \
  mysql:8.4.2
```

```sql
# 初始化 Database
CREATE DATABASE IF NOT EXISTS taibiex_private_api;
```

启动服务后会自动创建表结构。在确认表结构创建成功后，可以导入测试数据。从 mysql client 连接到数据库，执行以下命令导入测试数据。

```shell
source /path/to/initial.sql;
```

### 初始化 Redis 环境

```shell
# 创建配置目录
mkdir -p redis.conf.d
# 创建数据存储目录
mkdir -p redis-data

# 创建 redis-stack.conf 文件
cat << EOL > redis.conf.d/redis-stack.conf
EOL
```

```shell
docker run \
  -d \
  --name taibiex-redis-stack-container \
  -v ${PWD}/redis.conf.d/redis-stack.conf:/redis-stack.conf \
  -v ${PWD}/redis-data:/data \
  -p 6379:6379 \
  -p 8001:8001 \
  redis/redis-stack:6.2.6-v15
```
