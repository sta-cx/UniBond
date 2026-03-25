FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/unibond-server-*.jar app.jar
ENV JAVA_OPTS="-Xmx384m -Xms256m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
