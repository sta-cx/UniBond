FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /build

# 复制阿里云 Maven 镜像配置
COPY maven-settings.xml /root/.m2/settings.xml

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -q -s /root/.m2/settings.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/unibond-server-*.jar app.jar
ENV JAVA_OPTS="-Xmx384m -Xms256m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
