# Build Spring Boot backend
FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./
COPY backend/src ./src
RUN mvn -DskipTests -Pprod package

# Generate static self-signed TLS cert for nginx
FROM alpine:3.20 AS certs
RUN apk add --no-cache openssl
RUN mkdir -p /certs \
  && openssl req -x509 -nodes -newkey rsa:2048 -days 3650 \
    -subj "/CN=localhost" \
    -keyout /certs/tls.key \
    -out /certs/tls.crt

# Build Flutter web UI
FROM ghcr.io/cirruslabs/flutter:stable AS frontend-build
WORKDIR /workspace/frontend

RUN git config --global --add safe.directory /sdks/flutter

COPY frontend/pubspec.yaml frontend/pubspec.lock* ./
RUN flutter pub get
COPY frontend/ ./
RUN flutter build web --release --pwa-strategy=none

# Runtime image: nginx + Java
FROM eclipse-temurin:17-jre

RUN apt-get update \
  && apt-get install -y --no-install-recommends nginx ca-certificates \
  && rm -rf /var/lib/apt/lists/* \
  && rm -rf /etc/nginx/sites-enabled/default \
  && rm -rf /etc/nginx/sites-available/default

# Backend
WORKDIR /app
COPY --from=backend-build /workspace/backend/target/expensemanager.jar /app/app.jar

# TLS certs
RUN mkdir -p /etc/nginx/certs
COPY --from=certs /certs/tls.crt /etc/nginx/certs/tls.crt
COPY --from=certs /certs/tls.key /etc/nginx/certs/tls.key

# Frontend
COPY --from=frontend-build /workspace/frontend/build/web /usr/share/nginx/html

# Nginx config + entrypoint
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 80 443

ENTRYPOINT ["/entrypoint.sh"]
