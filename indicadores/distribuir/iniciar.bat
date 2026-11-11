@echo off
setlocal
title Indicadores de Cadastro
cd /d "%~dp0"

rem 1) Java portatil dentro da pasta do programa (pasta "java" ou "jdk-17..." extraida do zip)
set "JAVA="
if exist "%~dp0java\bin\java.exe" set "JAVA=%~dp0java\bin\java.exe"
if not defined JAVA for /d %%D in ("%~dp0jdk*" "%~dp0jre*" "%~dp0zulu*" "%~dp0OpenJDK*") do if exist "%%~D\bin\java.exe" set "JAVA=%%~D\bin\java.exe"
rem 2) senao, o Java instalado no computador
if not defined JAVA (
  where java >nul 2>nul && set "JAVA=java"
)
if not defined JAVA goto semjava

rem Java 8 ou mais antigo mostra a versao como "1.x"
"%JAVA%" -version 2>&1 | findstr /r /c:"version \"1\." >nul && goto javaantigo

echo Iniciando... o navegador vai abrir sozinho em alguns segundos.
echo Endereco: http://localhost:8765
echo Para encerrar o programa, feche esta janela.
echo.
"%JAVA%" -Xmx512m -jar app.jar %*
pause
exit /b

:javaantigo
echo.
echo  O Java deste computador e antigo:
"%JAVA%" -version 2>&1 | findstr /c:"version"
echo  O programa precisa do Java 17 ou mais novo.
goto instrucoes

:semjava
echo.
echo  Nenhum Java foi encontrado neste computador.

:instrucoes
echo.
echo  SOLUCAO SEM INSTALAR NADA (nao precisa de administrador):
echo   1. Acesse https://adoptium.net/temurin/releases/?version=17^&os=windows^&arch=x64^&package=jre
echo   2. Baixe o arquivo .ZIP (nao o .msi) do JRE 17 para Windows x64.
echo   3. Extraia o zip DENTRO desta pasta:
echo      %~dp0
echo      (vai aparecer uma pasta parecida com "jdk-17.0.x+y-jre")
echo   4. Rode este iniciar.bat de novo.
echo.
pause
exit /b 1
