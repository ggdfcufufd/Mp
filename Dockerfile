FROM gradle:8.12-jdk21

WORKDIR /app

COPY . .

RUN gradle shadowJar --no-daemon
RUN echo "=== Содержимое JAR ===" && jar tf build/libs/Mp-1.0.0.jar | head -30
RUN echo "=== Полный путь к классу ===" && jar tf build/libs/Mp-1.0.0.jar | grep Bot

CMD sh -c "jar tf build/libs/Mp-1.0.0.jar | head -30 && java -jar build/libs/Mp-1.0.0.jar"
