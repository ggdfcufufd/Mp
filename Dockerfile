FROM gradle:8.12-jdk21

WORKDIR /app

COPY . .

RUN gradle shadowJar --no-daemon && ls -la build/libs/

CMD ["java", "-jar", "build/libs/Mp-1.0.0.jar"]
