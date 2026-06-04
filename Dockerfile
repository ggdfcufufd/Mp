FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY . .

RUN ./gradlew shadowJar || gradle shadowJar

CMD ["java", "-jar", "build/libs/Mp-1.0.0-all.jar"]
