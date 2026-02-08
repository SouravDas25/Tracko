# Build Spring Boot backend
FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./
COPY backend/src ./src
RUN mvn -DskipTests -Pprod package

# Build Flutter web UI
FROM ghcr.io/cirruslabs/flutter:stable AS frontend-build
WORKDIR /workspace/frontend

RUN git config --global --add safe.directory /sdks/flutter

COPY frontend/pubspec.yaml frontend/pubspec.lock* ./
RUN flutter pub get
COPY frontend/ ./
RUN flutter build web --release

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

# Frontend
COPY --from=frontend-build /workspace/frontend/build/web /usr/share/nginx/html

# Nginx config + entrypoint
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/entrypoint.sh"]
