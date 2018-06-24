if [ "$1" == "" ]; then
  echo "Usage : $0 <version>"
  exit
fi

echo Tag version $1

mvn versions:set -DnewVersion="$1"
mvn versions:commit

git commit -am "Version $1"
git tag "v$1"

newVersionMajor=$(echo $1 | rev | cut -d "." -f 2- | rev)
newVersionMinor=$(($(echo $1 | rev | cut -d "." -f 1 | rev)+1))
newVersion=$newVersionMajor.$newVersionMinor-SNAPSHOT

echo New version is $newVersion

mvn versions:set -DnewVersion="$newVersion"
mvn versions:commit

git commit -am "New version is $newVersion"
