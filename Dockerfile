FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY src src
# Debug helper: print the build context to help remote builders show what was copied
RUN echo "---- Build context at /app (top-level) ----" && ls -la /app || true
RUN echo "---- src tree (shallow) ----" && find src -maxdepth 4 -type f -print || true
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh
ENTRYPOINT ["/app/docker-entrypoint.sh"]
