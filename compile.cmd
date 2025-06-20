@echo off
set JAVA_HOME=c:\Program Files\Java\jdk-21
@chcp -q -- "%~dp0" "%~dpnx0" >nul 2>&1
@chcp 65001 >nul 2>&1
rmdir /s /q temp
"%JAVA_HOME%\bin\javac" -encoding UTF-8 -cp .;json-20250107.jar;gson-2.8.9.jar app\ollama.java
"%JAVA_HOME%\bin\jar" --create --file=ollama.jar --main-class=app.ollama -C . app
mkdir temp
cd temp
"%JAVA_HOME%\bin\jar" xf ../gson-2.8.9.jar
"%JAVA_HOME%\bin\jar" xf ../json-20250107.jar
"%JAVA_HOME%\bin\jar" xf ../ollama.jar
"%JAVA_HOME%\bin\jar" --create --file=../ollama-fat.jar --main-class=app.ollama -C . app -C . com -C . META-INF
cd ..
rmdir /s /q temp
del *.class
"%JAVA_HOME%\bin\java" -jar ollama-fat.jar