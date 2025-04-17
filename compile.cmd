@echo off
@chcp -q -- "%~dp0" "%~dpnx0" >nul 2>&1
@chcp 65001 >nul 2>&1
rmdir /s /q temp
javac -encoding UTF-8 -cp .;json-20250107.jar;gson-2.8.9.jar ollama.java
jar cfe ollama.jar ollama *.class 
mkdir temp && cd temp && jar xf ../gson-2.8.9.jar && jar xf ../json-20250107.jar && jar xf ../ollama.jar && jar cfe ../ollama-fat.jar ollama * && cd .. && rmdir /s /q temp
del *.class
java -jar ollama-fat.jar