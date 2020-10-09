INFO=./target/project-info.json
POM=./bin/pom
CLOJURE=clojure
POM_FILE=pom.xml

if [ -f "$INFO" ]; then
  VERSION=$(jq -r ".version" "$INFO")
  GROUP_ID=$(jq -r ".groupId" "$INFO")
  ARTIFACT_ID=$(jq -r ".artifactId" "$INFO")
fi

function check-file {
  if [ ! -f "$1" ]; then
    echo "No such file: $1" >&2
    exit 2
  fi
}
