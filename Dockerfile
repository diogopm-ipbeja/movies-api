FROM eclipse-temurin:17-jre

WORKDIR /app

COPY build/libs/movies.jar app.jar
COPY build/resources/main/application.yaml application.yaml
COPY build/resources/main/openapi openapi
COPY src/main/resources/openapi/documentation.yaml openapi/documentation.yaml


EXPOSE 8080

CMD ["java", "-jar", "app.jar", "-config", "application.yaml"]
