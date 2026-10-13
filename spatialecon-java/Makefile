# spatialecon — thesis models, refactored
#
# Plain javac. The core has no external dependencies at all; only the viz module
# needs the Processing jars in lib/.
#
# JDK 17 is required and its path is pinned. Do not use a newer JDK: Processing 1.x
# extends java.applet.Applet, which JDK 25+ removed. See CLAUDE.md.

JAVA_HOME := /opt/homebrew/opt/openjdk@17
JAVAC     := $(JAVA_HOME)/bin/javac
JAVA      := $(JAVA_HOME)/bin/java

SRC   := src
BUILD := build
LIB   := lib

CORE_SRC := $(shell find $(SRC)/spatialecon/core -name '*.java')
VIZ_SRC  := $(shell find $(SRC)/spatialecon/viz  -name '*.java' 2>/dev/null)
TEST_SRC := $(shell find test -name '*.java' 2>/dev/null)

VIZ_CP  := $(LIB)/core.jar:$(LIB)/controlP5.jar:$(LIB)/peasycam.jar
TEST_CP := $(BUILD):$(LIB)/junit-4.5.jar

JFLAGS := -Xlint:all -d $(BUILD)

.PHONY: all core viz test run list clean check-jdk

all: core

check-jdk:
	@test -x $(JAVAC) || { \
	  echo "JDK 17 not found at $(JAVA_HOME)."; \
	  echo "Install with: brew install openjdk@17"; exit 1; }

## core — the engine. No external jars.
core: check-jdk
	@mkdir -p $(BUILD)
	@$(JAVAC) $(JFLAGS) $(CORE_SRC)
	@echo "core built -> $(BUILD)"

## viz — Processing renderers. Needs lib/*.jar.
viz: core
	@test -n "$(VIZ_SRC)" || { echo "no viz sources yet"; exit 0; }
	@$(JAVAC) $(JFLAGS) -cp $(BUILD):$(VIZ_CP) $(VIZ_SRC)
	@echo "viz built -> $(BUILD)"

## test — JUnit 4 against the econ functions. Test classes discovered from test/.
TEST_CLASSES := $(shell find test -name '*Test.java' 2>/dev/null \
                  | sed 's|^test/||; s|\.java$$||; s|/|.|g')

test: core
	@test -n "$(TEST_SRC)" || { echo "no tests yet"; exit 0; }
	@$(JAVAC) $(JFLAGS) -cp $(TEST_CP) $(TEST_SRC)
	@$(JAVA) -cp $(TEST_CP) org.junit.runner.JUnitCore $(TEST_CLASSES)

## list — show available scenarios.
list: core
	@$(JAVA) -cp $(BUILD) spatialecon.core.Runner --list

## run — e.g. make run ARGS="smoke --days 100 --out data"
run: core
	@$(JAVA) -cp $(BUILD) spatialecon.core.Runner $(ARGS)

clean:
	@rm -rf $(BUILD)
	@echo "cleaned"
