FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle settings.gradle gradle.properties ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y tesseract-ocr && rm -rf /var/lib/apt/lists/*
ENV TESSDATA_PATH=/usr/share/tesseract-ocr/4.00/tessdata
ENV TESSERACT_LIB_PATH=
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx400m", "-XX:+UseG1GC", "-jar", "app.jar"]
