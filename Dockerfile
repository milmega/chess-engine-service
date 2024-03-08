FROM openjdk:21
VOLUME /tmp
EXPOSE 8080
ARG JAR_FILE=target/chess-engine-service.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]