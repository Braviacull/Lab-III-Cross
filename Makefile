# Variabili
JAVAC = javac
JAVA_FILES = cross/Server.java cross/Client.java
TARGET_DIR = cross

# Regola di default
all: compile

# Regola per compilare i file Java
compile:
	$(JAVAC) $(JAVA_FILES)

# Regola per pulire i file compilati
clean:
	rm -f $(TARGET_DIR)/*.class

