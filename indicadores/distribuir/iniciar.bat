@echo off
title Indicadores de Cadastro
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo.
  echo  Java nao encontrado. Instale o Java 17 ou mais novo:
  echo  https://adoptium.net/  ^(Temurin 17 ou 21, instalador .msi^)
  echo.
  pause
  exit /b 1
)
echo Iniciando... o navegador vai abrir sozinho em alguns segundos.
echo Para encerrar o programa, feche esta janela.
java -Xmx512m -jar app.jar
pause
