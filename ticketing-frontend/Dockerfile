FROM oven/bun:1.2-alpine
WORKDIR /app
COPY package.json bun.lockb* bun.lock* ./
RUN bun install
COPY . .
EXPOSE 5173
CMD ["bun", "run", "dev", "--host", "0.0.0.0", "--port", "5173"]
