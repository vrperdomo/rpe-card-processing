# syntax=docker/dockerfile:1
#
# Dockerfile multi-stage compartilhado pelos 3 microserviços do monorepo.
# O serviço é escolhido via --build-arg SERVICE_MODULE=<produto|portador|cartao>-service
# (ver docker-compose.yml). O contexto de build é a raiz do repositório porque o
# reactor Maven é multi-módulo e precisa do pom.xml agregador e dos poms irmãos.

FROM eclipse-temurin:22-jdk-jammy AS build
ARG SERVICE_MODULE
WORKDIR /workspace

# Copia primeiro apenas os poms para aproveitar o cache de camadas do Docker
# quando só o código-fonte muda.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY services/produto-service/pom.xml services/produto-service/pom.xml
COPY services/portador-service/pom.xml services/portador-service/pom.xml
COPY services/cartao-service/pom.xml services/cartao-service/pom.xml
RUN ./mvnw -q -pl services/${SERVICE_MODULE} -am dependency:go-offline

COPY services/${SERVICE_MODULE}/src services/${SERVICE_MODULE}/src
RUN ./mvnw -q -pl services/${SERVICE_MODULE} -am -DskipTests package \
    && cp services/${SERVICE_MODULE}/target/*.jar /workspace/app.jar

FROM eclipse-temurin:22-jre-jammy AS runtime
ARG SERVICE_PORT=8080
ENV SERVICE_PORT=${SERVICE_PORT}

# curl é usado apenas pelo HEALTHCHECK (chama o Actuator do próprio processo).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 1000 rpe \
    && useradd --system --uid 1000 --gid rpe --shell /usr/sbin/nologin rpe

WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
RUN chown rpe:rpe app.jar
USER rpe

EXPOSE ${SERVICE_PORT}

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
    CMD curl -fsS "http://localhost:${SERVICE_PORT}/actuator/health/liveness" || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
