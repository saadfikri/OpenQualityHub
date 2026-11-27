# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S oqh && adduser -S oqh -G oqh
COPY --from=build /app/target/*.jar app.jar
RUN chown -R oqh:oqh /app

USER oqh

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
