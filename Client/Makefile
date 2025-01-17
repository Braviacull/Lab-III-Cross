# Variabili
JAVAC = javac
JAR = lib/gson-2.11.0.jar
JAVA_FILES = client/ClientMain.java
TARGET_DIR = client
CLASSPATH = .;$(JAR)

# Regola di default
all: client

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

# Regola per avviare il ClientMain
client: compile
	java -cp $(CLASSPATH) client.ClientMain