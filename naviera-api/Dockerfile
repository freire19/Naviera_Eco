# ── Stage 1: Build ──
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn package -DskipTests -B

# ── Stage 2: Run ──
FROM eclipse-temurin:17-jre-alpine
# #DS5-402: container nao roda mais como root
RUN addgroup -S naviera && adduser -S naviera -G naviera
WORKDIR /app
COPY --from=build --chown=naviera:naviera /app/target/*.jar app.jar
USER naviera
# #DS5-403: app Spring Boot escuta em 8081 (application.properties) — EXPOSE alinhado
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
