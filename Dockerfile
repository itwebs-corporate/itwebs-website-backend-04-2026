FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build --chown=app:app /app/target/site-cms-0.1.0.jar app.jar
USER app
EXPOSE 9999
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
