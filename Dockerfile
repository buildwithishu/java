FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests -P headless

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app
RUN groupadd --system eventportal \
    && useradd --system --gid eventportal --home-dir /app --shell /usr/sbin/nologin eventportal \
    && mkdir -p /app/certificates \
    && chown -R eventportal:eventportal /app
COPY --from=builder /app/target/college-event-portal-all.jar app.jar
RUN chown eventportal:eventportal app.jar

USER eventportal

ENTRYPOINT ["java", "-jar", "app.jar"]
