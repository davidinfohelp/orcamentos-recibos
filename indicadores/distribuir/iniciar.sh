#!/bin/sh
# Mac / Linux: dê permissão uma vez com  chmod +x iniciar.sh  e rode  ./iniciar.sh
cd "$(dirname "$0")"
command -v java >/dev/null 2>&1 || { echo "Java não encontrado. Instale o Java 17+ em https://adoptium.net/"; exit 1; }
echo "Iniciando... abra http://localhost:8765 se o navegador não abrir sozinho. Ctrl+C encerra."
exec java -Xmx512m -jar app.jar "$@"
