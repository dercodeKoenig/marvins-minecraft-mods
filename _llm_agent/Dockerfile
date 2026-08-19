FROM python:3.11-slim

# Install system dependencies, git, and basic dev tools
RUN apt update -y && apt install git curl zstd -y
RUN curl -fsSL https://ollama.com/install.sh | sh
RUN curl -fsSL https://claude.ai/install.sh | bash

RUN apt install openjdk-21-jdk -y

ENV PATH="/root/.local/bin:${PATH}"

WORKDIR /project

# Set default command
CMD ["bash"]