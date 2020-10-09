PROJECT_INFO=./target/project-info.json
POM_FILE=pom.xml
GIT=git
POM=pom.xml

$(PROJECT_INFO): $(POM_FILE)
	test -d ./target || mkdir ./target
	./bin/pom json > $(PROJECT_INFO)

test:
	clojure -M:test

check-git-clean:
	$(GIT) diff-index --quiet HEAD

clean:
	@rm -rf target .cpcache .nrepl-port

.PHONY: clean check-git-clean test
