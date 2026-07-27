FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S paycore && adduser -S paycore -G paycore
WORKDIR /app
COPY --from=build /workspace/target/kunturpay-event-platform-*.jar app.jar
USER paycore
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -q -O - http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
