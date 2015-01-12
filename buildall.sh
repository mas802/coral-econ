
#
# all of this might actually happen in maven anyway
#

hg pull
hg update
mvn package

cp target/coral-*-with-dependencies.jar ~/Dropbox/Public/coral-dep.jar
cp target/coral-*-beta.jar ~/Dropbox/Public/coral.jar

cp target/coral-*-with-dependencies.jar examplePG/coral-dep.jar

cp servervm/*.* examplePG/servervm/
zip -r target/examplePG.zip examplePG/

cp target/examplePG.zip ~/Dropbox/Public/examplePG.zip
