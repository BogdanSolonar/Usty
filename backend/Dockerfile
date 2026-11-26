# Образ бекенду (потрібен лише для повного запуску в Docker: docker compose --profile full up)
# Крок 1: збираємо jar
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# gradlew міг бути збережений на Windows з CRLF — прибираємо, інакше Linux його не запустить
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null
COPY src src
RUN ./gradlew bootJar --no-daemon -q

# Крок 2: легкий образ лише з Java і готовим jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/backend-0.0.1-SNAPSHOT.jar app.jar
# Фото (аватарки, обкладинки, фото з побачень) — у томі, щоб не зникали при перезбиранні образу
ENV UPLOADS_DIR=/app/uploads
VOLUME /app/uploads
EXPOSE 8080
# secrets.properties монтується в /app при запуску (див. docker-compose.yml), в образ він не потрапляє
ENTRYPOINT ["java", "-jar", "app.jar"]
