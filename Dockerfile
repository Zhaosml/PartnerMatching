# Docker 镜像构建
# @author <a href="https://github.com/liyupi">程序员鱼皮</a>
# @from <a href="https://yupi.icu">编程导航知识星球</a>
###要下载maven镜像
FROM maven:3.5-jdk-8-alpine as builder

# Copy local code to the container image.
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build a release artifact.
RUN mvn package -DskipTests

# Run the web service on container startup.
CMD ["java","-jar","/app/target/user-center-backend-0.0.1-SNAPSHOT.jar","--spring.profiles.active=prod"]

##方式2
FROM openjdk:8
WORKDIR /app
COPY user-center-0.0.1-SNAPSHOT.jar /app
ENTRYPOINT ["java","-jar","user-center-0.0.1-SNAPSHOT.jar"]
EXPOSE 8999