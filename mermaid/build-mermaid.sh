#!/bin/bash
# Go to the script's directory
cd "$(dirname "$0")"

echo "[Build Script] Checking for 'mermaid' Docker image..."
if ! docker image inspect mermaid > /dev/null 2>&1; then
    echo "[Build Script] Docker image not found. Building now — this may take a while and also IGNORE the output color being RED, it's fine unless you see a big ERROR"
    docker build -t mermaid .
else
    echo "[Build Script] Docker image 'mermaid' already exists ✅"
fi
