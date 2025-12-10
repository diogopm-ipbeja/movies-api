FROM amazoncorretto:21-alpine3.21 AS runtime


WORKDIR /app

COPY build/libs/movies-all.jar app.jar
COPY build/resources/main/application.yaml application.yaml
#COPY build/resources/main/openapi openapi
COPY src/main/resources/openapi/doc.yaml openapi/documentation.yaml


EXPOSE 8080

CMD ["java", "-jar", "app.jar", "-config", "application.yaml"]
