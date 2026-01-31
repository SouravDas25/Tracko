# Alpine Linux with OpenJDK JRE
FROM java:8
# copy WAR into image
COPY ./target/expensemanager.jar /app.jar
# run application with this command line
ENTRYPOINT /usr/bin/java -jar -Dspring.profiles.active=dev,standalone /app.jar