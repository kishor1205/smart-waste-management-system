FROM openjdk:17-jdk-slim

WORKDIR /app

COPY backend/target/*.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java","-jar","app.jar"]