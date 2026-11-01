# Imagen para desplegar el backend (Render, Railway, Fly.io, etc.).
# Las credenciales NO van en la imagen: se configuran como variables de entorno
# en el proveedor (SUPABASE_DB_URL, SUPABASE_DB_USER, SUPABASE_DB_PASSWORD,
# JWT_SECRET, CORS_ALLOWED_ORIGINS, MAIL_USERNAME, MAIL_PASSWORD).

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Render/Railway inyectan PORT; application.yml lee SERVER_PORT
ENV JAVA_OPTS="-Xmx384m"
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
