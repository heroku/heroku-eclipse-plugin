## Eclipse plugin for Heroku

###  Building
To get the tycho build running locally you need maven 3.x. The parent
pom for the build is found in an eclipse project called "releng" for
release engineering.

In a local setup I've configured it to resolve the eclipse plugins it
needs from the local filesystem. You see this in the pom.xml in the
repositories section.

You are free to change the path in the pom.xml. If you don't want to use local repositories you
can comment the 2 entries and use the indigo repo (this way the
dependencies are fetch from eclipse.org which naturally takes longer).

The 2 important goals are:
* package => results in an Eclipse Updatesite
* integration-tests => runs the junit-tests

