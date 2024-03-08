FROM openjdk:21
WORKDIR /app
COPY target/chess-engine-service.jar /app/chess-engine-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "chess-engine-service.jar"]