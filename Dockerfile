FROM eclipse-temurin:21-jdk

WORKDIR /app

# Копируем файлы
COPY . .

# Даём права и запускаем сборку через Gradle wrapper
RUN chmod +x gradlew && ./gradlew shadowJar --no-daemon

CMD ["java", "-jar", "build/libs/Mp-1.0.0-all.jar"]
