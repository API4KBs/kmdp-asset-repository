# kmdp-asset-repository
##Release Instructions

Affected variables:
* project.parent.version
* project.version (SELF)

Affected properties:
* kmdp.artifact.repo.version
* kmdp.language.version


### Release Branch
1. Set root POM's version and parent.version to desired fixed version
  * The parent.version MUST match the ${kmdp.impl.version} variable in the BOM
2. Set the properties
  * Set kmdp.artifact.repo.version to the desired release version
  * Set kmdp.language.version to the desired release version
3. Update the sub-modules's parent version accordingly 

### Nex Dev Branch
1. Set root POM's parent and project to the next desired version
