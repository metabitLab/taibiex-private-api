#!/usr/bin/env bash

# 用法
# 1. 跳转到项目根目录
# 2. 执行命令：sh cicd/deploy.sh

# 该脚本用于构建、上传、部署服务
# 1. 构建制品
# 2. 上传制品
# 3. 部署服务

# 该脚本需要在本地执行，需要本地环境有 maven、scp、ssh 等命令
# 该脚本需要在本地执行，需要在本地有 taibiex-gateway.pem 文件
# 该脚本需要在远程服务器上执行，需要远程服务器有 java 环境
# 该脚本需要在远程服务器上执行，需要远程服务器有 /home/taibiex-private-api 目录

set -ex

# 构建制品
mvn clean -f ./pom.xml
mvn package -f ./pom.xml -DskipTests

## 获取最新的制品的名称
artifact_name=$(ls -t ./target/*.jar | head -n 1 | awk -F '/' '{print $3}')

echo "已构建的制品名称：$artifact_name，是否继续上传制品？(Y/[N])"
read confirm_0
if [ "$confirm_0" != "Y" ]; then
  echo "上传已取消"
  exit 0
fi

# 上传制品的临时目录 /tmp
scp -i taibiex-gateway.pem ./target/taibiex-private-api-*.*.*-SNAPSHOT.jar ubuntu@ec2-54-234-143-228.compute-1.amazonaws.com:/tmp

## 通过 ssh 登录远程服务器后，远程执行命令：将制品从临时目录移动到部署目录
ssh -i taibiex-gateway.pem ubuntu@ec2-54-234-143-228.compute-1.amazonaws.com > /dev/null 2>&1 << remote
sudo su -
mv /tmp/$artifact_name /home/taibiex-private-api
exit
remote

echo "已上传制品：$artifact_name，是否继续部署服务？(Y/[N])"
read confirm_1
if [ "$confirm_1" != "Y" ]; then
  echo "部署已取消"
  exit 0
fi

# Run

## 通过 ssh 登录远程服务器后，远程执行命令：启动服务
ssh -i taibiex-gateway.pem ubuntu@ec2-54-234-143-228.compute-1.amazonaws.com > /dev/null 2>&1 << remote
sudo su -
cd /home/taibiex-private-api
ps -aux | grep jar | grep -v jar | awk '{print $2}' | xargs kill -9
nohup java -jar $artifact_name > nohup.out &
exit
remote

echo "服务已部署完成"