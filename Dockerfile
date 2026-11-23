# One Dockerfile for every service. Build with: --build-arg MODULE=<module-name>
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
ARG MODULE
RUN mvn -B -q -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /src/${MODULE}/target/${MODULE}-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
