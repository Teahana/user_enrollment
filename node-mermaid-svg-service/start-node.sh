#!/bin/bash

# Load nvm and use Node
export NVM_DIR="$HOME/.nvm"
source "$NVM_DIR/nvm.sh"
nvm use default

# Navigate to your service and run it
cd "$HOME/node-mermaid-svg-service"
node server.js
