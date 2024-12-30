# Variabili
JAVAC = javac
JAVA_FILES = cross/CROSSServer.java cross/ServerThread.java cross/CROSSClient.java
TARGET_DIR = cross

# Regola di default
all: compile

# Regola per compilare i file Java
compile:
	$(JAVAC) $(JAVA_FILES)

# Regola per pulire i file compilati
clean:
ifeq ($(OS),Windows_NT)
	del /Q $(TARGET_DIR)\*.class
else
	rm -f $(TARGET_DIR)/*.class
endif