del *.class
javac -encoding UTF-8 -cp .;json-20250107.jar ollama.java
jar cfe ollama.jar ollama *.class json-20250107.jar
del *.class
java -jar ollama.jar