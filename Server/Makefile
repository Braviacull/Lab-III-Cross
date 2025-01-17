# Variabili
JAVAC = javac
JAR = lib/gson-2.11.0.jar
JAVA_FILES = server/ServerMain.java
TARGET_DIR = server
CLASSPATH = .;$(JAR)

# Regola di default
all: server

# Regola per compilare i file Java
compile:
	$(JAVAC) -cp $(CLASSPATH) $(JAVA_FILES)

# Regola per pulire i file compilati
clean:
ifeq ($(OS),Windows_NT)
	del /Q $(TARGET_DIR)\*.class
else
	rm -f $(TARGET_DIR)/*.class
endif

# Regola per avviare il ServerMain
server: compile
	java -cp $(CLASSPATH) server.ServerMain
