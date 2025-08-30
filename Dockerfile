FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy the source code and dependencies
COPY src/ ./src/
COPY lib/ ./lib/

# Compile all Java files
RUN javac -cp "lib/*" -d build $(find src -name "*.java")

# Expose the port
EXPOSE 8080

# Run the webhook server
CMD ["java", "-cp", "build:lib/*", "com.example.webhook.PayPalWebhookServer"]