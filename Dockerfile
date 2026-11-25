# Etapa 1: compilación con Maven y OpenJDK 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: ejecución ligera con JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Railway (o Render) inyecta la variable PORT; application.properties la lee con ${PORT:8083}.
# Base de datos: DB_URL, DB_USER y DB_PASSWORD. Correo: MAIL_*. Enlace de recuperación: APP_BASE_URL.
# Todas se definen como variables de entorno en la plataforma, nunca en el código.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
