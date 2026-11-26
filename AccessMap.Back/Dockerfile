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

# A imagem e a de producao: perfil prod (application-prod.yaml), que exige as variaveis do banco
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
# Liveness (#132): o processo responde. Fora do Render (docker run, compose), o Docker marca o container
# como unhealthy se travar. O Render usa o caminho configurado no painel (docs/deploy.md).
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS "http://localhost:${PORT:-8080}/actuator/health/liveness" || exit 1
# Heap proporcional a memoria do container, para nao ser encerrado por falta de memoria em planos pequenos
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
