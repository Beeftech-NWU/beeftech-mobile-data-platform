FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY . .

RUN chmod +x ./gradlew && \
    ./gradlew :backend:api:installDist --no-daemon


FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build \
    /workspace/backend/api/build/install/api/ \
    /app/

RUN mkdir -p /app/data

EXPOSE 8081

CMD ["/app/bin/api"]