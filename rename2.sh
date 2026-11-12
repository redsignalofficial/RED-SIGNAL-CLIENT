#!/bin/bash
cd ~/RED-SIGNAL-CLIENT

# 1. Replace all package/import references in Java files
find . -type f -name "*.java" | xargs sed -i 's/us\.whitedev/us.rin/g'

# 2. Move the actual directory
mkdir -p src/main/java/us/rin
cp -r src/main/java/us/whitedev/. src/main/java/us/rin/
rm -rf src/main/java/us/whitedev

echo "Package rename done"
