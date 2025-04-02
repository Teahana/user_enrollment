#!/bin/bash

echo "✅ Running start-node.sh"

# Use absolute path to Node
NODE_PATH="/home/teahana/.nvm/versions/node/v16.20.2/bin/node"

# Navigate to this script’s directory
cd "$(dirname "$0")" || exit 1

echo "🗂 Current directory: $(pwd)"

# Start the Node server
"$NODE_PATH" server.js
