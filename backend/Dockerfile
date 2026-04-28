FROM openjdk:17-jdk-slim

WORKDIR /app

COPY backend/target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java","-jar","app.jar"]