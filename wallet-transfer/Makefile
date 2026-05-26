PG_JAR    := lib/postgresql-42.7.4.jar
JUNIT_JAR := lib/junit-platform-console-standalone-1.11.4.jar
MAIN_SRC  := $(shell find src -name "*.java" 2>/dev/null)
TEST_SRC  := $(shell find test -name "*.java" 2>/dev/null)

.PHONY: deps compile test-compile test run clean

deps:
	mkdir -p lib
	@[ -f $(PG_JAR) ] || curl -fsSL -o $(PG_JAR) \
		"https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar"
	@[ -f $(JUNIT_JAR) ] || curl -fsSL -o $(JUNIT_JAR) \
		"https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"

compile: deps
	mkdir -p out/main
	javac --release 21 -cp "$(PG_JAR)" -d out/main $(MAIN_SRC)

test-compile: compile
	mkdir -p out/test
	javac --release 21 -cp "$(PG_JAR):$(JUNIT_JAR):out/main" -d out/test $(TEST_SRC)

test: test-compile
	java -cp "$(PG_JAR):$(JUNIT_JAR):out/main:out/test" \
		org.junit.platform.console.ConsoleLauncher --scan-classpath

run: compile
	java -cp "$(PG_JAR):out/main" com.wallet.Main

clean:
	rm -rf out/
