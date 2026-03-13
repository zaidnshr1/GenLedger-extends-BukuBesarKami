FROM eclipse-temurin:21-jre-alpine AS preparator
WORKDIR /app
COPY target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=preparator --chown=spring:spring /app/dependencies/           ./
COPY --from=preparator --chown=spring:spring /app/spring-boot-loader/     ./
COPY --from=preparator --chown=spring:spring /app/snapshot-dependencies/  ./
COPY --from=preparator --chown=spring:spring /app/application/            ./

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]