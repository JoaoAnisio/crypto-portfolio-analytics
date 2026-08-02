# ---------- Estágio 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copia apenas o pom primeiro: se as dependências não mudaram,
# o Docker reaproveita esta camada e pula o download.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Estágio 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuário sem privilégios: container rodando como root é risco desnecessário.
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /build/target/*.jar app.jar

RUN chown -R app:app /app
USER app

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]