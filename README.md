# bundler-cleanup
This project consists of a single JEE Timer Bean that is used to manage disk space associated with the staging area used by the bundler project.  

## Download and Build the Source
* Minimum requirements:
    * Java Development Kit (v1.8.0 or higher)
    * GIT (v1.7 or higher)
    * Maven (v3.3 or higher)
* Download source
```
# cd /var/local
# git clone https://github.com/carpenlc/bundler-cleaup.git
# cd ./bundler-cleanup/parent
# mvn clean package
```
* Invoke the build process.
```
# cd /var/local/bundler-cleanup/parent
# mvn clean package
```
* The pom.xml includes the Wildfly plugin definitions (see file ./parent/BundlerCleanup/pom.xml).  If you want to deploy it during the build process use the following Maven directive.
```
# cd /var/local/bundler-cleanup/parent
# mvn clean package wildfly:deploy
```
