FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /a
COPY . .
RUN mvn -q package -DskipTests
FROM eclipse-temurin:17-jre
COPY --from=build /a/target/app.jar /app.jar
CMD ["java","-jar","/app.jar"]
