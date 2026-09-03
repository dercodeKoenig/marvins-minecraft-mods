FROM python:3.11-slim

RUN apt update -y && apt install git curl zstd chafa -y

RUN curl -fsSL https://ollama.com/install.sh | sh
RUN curl -fsSL https://claude.ai/install.sh | bash
RUN curl -fsSL https://opencode.ai/install | bash

RUN apt install openjdk-21-jdk -y

ENV PATH="/root/.local/bin:${PATH}"

WORKDIR /project

# Set default command
CMD ["bash"]