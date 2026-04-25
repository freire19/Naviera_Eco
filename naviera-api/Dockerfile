# ── Stage 1: Build ──
# #DS5-431: pin minor.
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn package -DskipTests -B

# ── Stage 2: Run ──
# #DS5-431: pin minor (17.0.13) — temurin 17-jre-alpine flutua a cada release de patch.
FROM eclipse-temurin:17.0.13_11-jre-alpine
# #315: tini como PID 1 garante que SIGTERM chegue no Java e zombies sejam colhidos.
RUN apk add --no-cache tini \
    && addgroup -S naviera && adduser -S naviera -G naviera
WORKDIR /app
COPY --from=build --chown=naviera:naviera /app/target/*.jar app.jar
USER naviera
# #DS5-403: app Spring Boot escuta em 8081 (application.properties) — EXPOSE alinhado
EXPOSE 8081
# #316: Spring Boot faz graceful shutdown em SIGTERM; deixar explicito.
STOPSIGNAL SIGTERM
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "app.jar"]
