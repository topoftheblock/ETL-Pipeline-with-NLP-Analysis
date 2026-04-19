# Use Maven with Eclipse Temurin Java 21 as the base image
FROM maven:3.9.6-eclipse-temurin-21

# Install Python, FFmpeg (required for Whisper), and pip
RUN apt-get update && \
    apt-get install -y python3 python3-pip python3-venv ffmpeg && \
    rm -rf /var/lib/apt/lists/*

# Create a virtual environment and install OpenAI Whisper CLI
# A venv is used to avoid PEP 668 'externally-managed-environment' errors on Debian
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
RUN pip install -U openai-whisper

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies to utilize Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the rest of the project source code
COPY src ./src

# Expose the Javalin web server port (default 7070)
EXPOSE 7070

# Compile the project and run it using the exec-maven-plugin configured in your pom.xml
CMD ["mvn", "clean", "compile", "exec:exec"]