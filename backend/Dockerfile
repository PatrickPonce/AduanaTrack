# --- Etapa 1: compilación ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package && cp target/aduanatrack-*.jar app.jar

# --- Etapa 2: ejecución (solo JRE, usuario sin privilegios) ---
FROM eclipse-temurin:24-jre
WORKDIR /app
RUN groupadd --system aduana && useradd --system --gid aduana --no-create-home aduana \
    && mkdir -p /data/cartas-poder && chown -R aduana:aduana /data
COPY --from=build --chown=aduana:aduana /app/app.jar app.jar
USER aduana
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
