# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1 — build the executable Spring Boot jar with the Maven wrapper.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy the wrapper and POM first so dependency resolution is cached when only
# source files change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Copy sources and build. Tests are skipped in the image build; the jar is the
# only artifact we need here.
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 — minimal runtime image with just the JRE and the jar.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN useradd --system --uid 10001 --home /app appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

# Render (and most hosts) inject PORT; the app binds to it via application.yml
# (server.port=${PORT:8080}). Document it here for local runs.
ENV PORT=8080
EXPOSE 8080

# Keep container memory within the free tier (512 MB on Render): cap the heap
# at a fraction of the container limit.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
