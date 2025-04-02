#!/bin/bash
# Make sure to use LF (not CRLF) if editing on Windows

echo "✅ Running start-node.sh"

# Load NVM and use Node 
export NVM_DIR="$HOME/.nvm"
source "$NVM_DIR/nvm.sh"
nvm use 16

# Navigate to this script’s directory
cd "$(dirname "$0")" || exit 1

# Print working directory for debug
echo "🗂 Current directory: $(pwd)"

# Run the Node server
node server.js
