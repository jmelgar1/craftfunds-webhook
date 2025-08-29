FROM openjdk:17-jre-slim

WORKDIR /app

# Copy the source code and dependencies
COPY src/ ./src/
COPY lib/ ./lib/

# Compile the webhook server
RUN javac -cp lib/*.jar -d build src/main/java/com/example/webhook/PayPalWebhookServer.java

# Expose the port
EXPOSE 8080

# Run the webhook server
CMD ["java", "-cp", "build:lib/*", "com.example.webhook.PayPalWebhookServer"]