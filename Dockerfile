FROM eclipse-temurin:21.0.13_9-jdk-noble AS build
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sh mvnw -q -B dependency:go-offline
COPY src src
RUN sh mvnw -q -B package -DskipTests

FROM eclipse-temurin:21.0.13_9-jre-noble
RUN useradd --system --no-create-home app
WORKDIR /app
COPY --from=build /build/target/spring-talent-engine-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-XX:+UseSerialGC", "-jar", "app.jar"]
