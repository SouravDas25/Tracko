#!/bin/sh
set -e

JAVA_OPTS=${JAVA_OPTS:-""}
SPRING_ARGS=${SPRING_ARGS:-""}

# Start Spring Boot backend in background
java $JAVA_OPTS -jar /app/app.jar --server.port=8080 $SPRING_ARGS &

# Run nginx in foreground
exec nginx -g 'daemon off;'
