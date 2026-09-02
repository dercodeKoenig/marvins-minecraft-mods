FROM python:3.11-slim

RUN apt update -y && apt install git curl zstd -y

ENV NVM_DIR="/root/.nvm"
ENV NODE_VERSION="24"
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash \
    && . "$NVM_DIR/nvm.sh" \
    && nvm install ${NODE_VERSION} \
    && nvm use ${NODE_VERSION}
ENV PATH="${NVM_DIR}/versions/node/v${NODE_VERSION}/bin:/root/.local/bin:${PATH}"


RUN curl -fsSL https://ollama.com/install.sh | sh
RUN curl -fsSL https://claude.ai/install.sh | bash
RUN curl -fsSL https://qwen-code-assets.oss-cn-hangzhou.aliyuncs.com/installation/install-qwen-standalone.sh | bash

RUN apt install openjdk-21-jdk -y

ENV PATH="/root/.local/bin:${PATH}"

WORKDIR /project

# Set default command
CMD ["bash"]