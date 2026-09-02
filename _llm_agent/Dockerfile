FROM python:3.11-slim

RUN apt update -y && apt install git curl zstd -y

RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash && . "$HOME/.nvm/nvm.sh" && nvm install 24

RUN curl -fsSL https://ollama.com/install.sh | sh
RUN curl -fsSL https://claude.ai/install.sh | bash
RUN curl -fsSL https://qwen-code-assets.oss-cn-hangzhou.aliyuncs.com/installation/install-qwen-standalone.sh | bash

RUN apt install openjdk-21-jdk -y

ENV PATH="/root/.local/bin:${PATH}"

WORKDIR /project

# Set default command
CMD ["bash"]