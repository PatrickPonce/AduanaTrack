# --- Etapa 1: compilación ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package && cp target/aduanatrack-*.jar app.jar

# --- Etapa 2: ejecución (solo JRE, usuario sin privilegios) ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system aduana && useradd --system --gid aduana --no-create-home aduana \
    && mkdir -p /data/cartas-poder /data/documentos && chown -R aduana:aduana /data
COPY --from=build --chown=aduana:aduana /app/app.jar app.jar
USER aduana
EXPOSE 8080
# Responde cuando el puerto del backend acepta conexiones (la imagen no trae curl ni wget).
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080"]
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
