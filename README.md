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

### Version Incrementing (WIP)
To set the version of this project, run:

    ant set-version -f releng/build.xml -DnewVersion=[version number without -SNAPSHOT]

This task calls `tycho-versions:set-version` and also updates the version in the update site. Note, the versions in the project itself are appended with `-SNAPSHOT`/`.qualifier`, but the update site uses raw versions. This is so that multiple builds of the same version still force an update. The version should be incremented immediately after releasing  the previous version.

**Note, this does not yet update versions dependencies. This still must be done manually.**
