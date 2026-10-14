# syntax=docker/dockerfile:1

# ---- build: compila o jar com o Maven Wrapper do projeto ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Baixa as dependências numa camada própria: só refaz quando o pom.xml muda.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B package -DskipTests \
    && cp target/*.jar app.jar

# ---- runtime: só o JRE e o jar ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 accessmap
USER accessmap

COPY --from=build /app/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
