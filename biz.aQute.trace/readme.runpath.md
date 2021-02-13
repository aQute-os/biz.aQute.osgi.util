# biz.aQute.trace.runpath

Weaves DS components and Bundle Activtors to record timing of the start/stop/modify methods. This JAR is not a proper bundle but must be placed on the -runpath. It uses the Embedded-Launcher facility of the bnd launchers. The JAR includes Javassist to perform the byte code weaving.

## Links

* [Documentation](https://aQute.biz)
* [Source Code](https://github.com/aQute-os/biz.aQute.osgi.util) (clone with `scm:git:git@github.com/aQute-os/biz.aQute.osgi.util.git`)

## Coordinates

### Maven

```xml
<dependency>
    <groupId>biz.aQute</groupId>
    <artifactId>biz.aQute.trace.runpath</artifactId>
    <version>1.3.0-SNAPSHOT</version>
</dependency>
```

### OSGi

```
Bundle Symbolic Name: biz.aQute.trace.runpath
Version             : 1.3.0.202101120015
```

### Feature-Coordinate

```
"bundles": [
   {
    "id": "biz.aQute:biz.aQute.trace.runpath:1.3.0-SNAPSHOT"
   }
]
```

## Developers

* **Peter Kriens** (aQute) / [info@aQute.biz](mailto:info@aQute.biz) @ aQute SARL

## Licenses

**http://opensource.org/licenses/apache2.0.php**
  > Apache License, Version 2.0
  >
  > For more information see [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Copyright

aQute SARL All Rights Reserved

---
aQute SARL