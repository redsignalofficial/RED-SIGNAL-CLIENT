#!/bin/bash
cd ~/RED-SIGNAL-CLIENT

# 1. Rename string literals and class references in all Java/CSS/txt files
find . -type f \( -name "*.java" -o -name "*.css" -o -name "*.txt" \) | while read -r f; do
  sed -i \
    -e 's/XynisClient/RedSignalClient/g' \
    -e 's/XynisProxy/RedSignalProxy/g' \
    -e 's/XynisProtocol/RedSignalProtocol/g' \
    -e 's/XynisUser/RedSignalUser/g' \
    -e 's/xynisProtocol/redSignalProtocol/g' \
    -e 's/xynisStyle/redSignalStyle/g' \
    -e 's/XYNIS/RED SIGNAL/g' \
    -e 's/Xynis/Red Signal/g' \
    -e 's/"xynis"/"redsignal"/g' \
    -e "s/'xynis'/'redsignal'/g" \
    -e 's/xynis-viaver/redsignal-viaver/g' \
    -e 's/{text:xynis/{text:redsignal/g' \
    -e 's/youtube\.com\/@xynis/youtube.com\/@redsignal/g' \
    -e 's/Hello XynisClient/Hello RedSignalClient/g' \
    -e 's/0WhiteDev/Rin/g' \
    -e 's/0whitedev/Rin/g' \
    -e 's/"madeq"/"Rin"/g' \
    -e "s/§7madeq x 0WhiteDev/§7Rin/g" \
    -e 's/§7madeq x Rin/§7Rin/g' \
    -e 's/Authors of the client is: §fRin §7and §fRin/Author of the client is: §fRin/g' \
    "$f"
done

# 2. Rename Java files with Xynis in the name
mv src/main/java/us/whitedev/XynisClient.java src/main/java/us/whitedev/RedSignalClient.java 2>/dev/null
mv src/main/java/us/whitedev/protocol/XynisProtocol.java src/main/java/us/whitedev/protocol/RedSignalProtocol.java 2>/dev/null
mv src/main/java/us/whitedev/proxy/XynisProxy.java src/main/java/us/whitedev/proxy/RedSignalProxy.java 2>/dev/null

echo "Done!"
