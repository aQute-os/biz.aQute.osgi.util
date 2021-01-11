# biz.aQute.gogo.commands.provider

## Links

* [Documentation](https://aQute.biz)
* [Source Code](https://github.com/aQute-os/biz.aQute.osgi.util) (clone with `scm:git:git@github.com/aQute-os/biz.aQute.osgi.util.git`)

## Coordinates

### Maven

```xml
<dependency>
    <groupId>biz.aQute</groupId>
    <artifactId>biz.aQute.gogo.commands.provider</artifactId>
    <version>1.3.0-SNAPSHOT</version>
</dependency>
```

### OSGi

```
Bundle Symbolic Name: biz.aQute.gogo.commands.provider
Version             : 1.3.0.202101071841
```

### Feature-Coordinate

```
"bundles": [
   {
    "id": "biz.aQute:biz.aQute.gogo.commands.provider:1.3.0-SNAPSHOT"
   }
]
```

## Gogo Commands

### logadmin:levels

**Synopsis**

`logadmin:levels ARG`

**Description**

Show the levels for a given bundle

**Arguments**

* `ARG`  The logger context bundle

---

**Synopsis**

`logadmin:levels`

**Description**

Show all log levels

### logadmin:defaultlevel

**Synopsis**

`logadmin:defaultlevel`

**Description**

Show the default level

---

**Synopsis**

`logadmin:defaultlevel ARG`

**Description**

Set the default level

**Arguments**

* `ARG`  The log level to set (DEBUG,INFO,WARN,ERROR)

### logadmin:addlevel

**Synopsis**

`logadmin:addlevel ARG1 ARG2 ARG3`

**Description**

Add a logger prefix to the context of the given bundle

**Arguments**

* `ARG1`  The logger context bundle
* `ARG2`  The name of the logger prefix or ROOT for all
* `ARG3`  The log level to set (DEBUG,INFO,WARN,ERROR)

---

**Synopsis**

`logadmin:addlevel ARG1 ARG2`

**Description**

Add a log name prefix to the root logger

**Arguments**

* `ARG1`  The logger name prefix
* `ARG2`  The log level to set (DEBUG,INFO,WARN,ERROR)

### logadmin:rmlevel

**Synopsis**

`logadmin:rmlevel ARG1 ARG2`

**Description**

Remove a log level from the given bundle

**Arguments**

* `ARG1`  The logger context bundle
* `ARG2`  The name of the logger prefix or ROOT for all

---

**Synopsis**

`logadmin:rmlevel ARG`

**Description**

Remove a log level from the root context

**Arguments**

* `ARG`  

### logadmin:slf4jdebug

**Synopsis**

`logadmin:slf4jdebug ARG`

**Description**

Create an SLF4J debug entry (for testing)

**Arguments**

* `ARG`  The message to log

### logadmin:slf4jinfo

**Synopsis**

`logadmin:slf4jinfo ARG`

**Description**

Create an SLF4J info entry

**Arguments**

* `ARG`  The message to log

### logadmin:slf4jwarn

**Synopsis**

`logadmin:slf4jwarn ARG`

**Description**

Create an SLF4J warn entry

**Arguments**

* `ARG`  The message to log

### logadmin:slf4jerror

**Synopsis**

`logadmin:slf4jerror ARG`

**Description**

Create an SLF4J error entry

**Arguments**

* `ARG`  The message to log

### logadmin:tail

**Synopsis**

`logadmin:tail ARG1 ARG2`

**Description**

Continuously show the log messages

**Arguments**

* `ARG1`  
* `ARG2`  The minimum log level (DEBUG,INFO,WARN,ERROR)

---

**Synopsis**

`logadmin:tail ARG`

**Description**

Continuously show all the log messages

**Arguments**

* `ARG`  

## Components

### biz.aQute.gogo.commands.provider.LoggerAdminCommands - *state = enabled, activation = immediate*

#### Description

#### Services - *scope = singleton*

|Interface name |
|--- |
|biz.aQute.gogo.commands.provider.LoggerAdminCommands |

#### Properties

|Name |Type |Value |
|--- |--- |--- |
|osgi.command.scope |String |"logadmin" |
|osgi.command.function |String[] |["levels", "defaultlevel", "addlevel", "rmlevel", "slf4jdebug", "slf4jinfo", "slf4jwarn", "slf4jerror", "tail"] |

#### Configuration - *policy = optional*

##### Pid: `biz.aQute.gogo.commands.provider.LoggerAdminCommands`

No information available.

#### Reference bindings

|Attribute |Value |
|--- |--- |
|name |$000 |
|interfaceName |org.osgi.service.log.admin.LoggerAdmin |
|target | |
|cardinality |1..1 |
|policy |static |
|policyOption |reluctant |
|scope |bundle ||Attribute |Value |
|--- |--- |
|name |$001 |
|interfaceName |org.osgi.service.log.LogReaderService |
|target | |
|cardinality |1..1 |
|policy |static |
|policyOption |reluctant |
|scope |bundle |

#### OSGi-Configurator


```
/*
 * Component: biz.aQute.gogo.commands.provider.LoggerAdminCommands
 * policy:    optional
 */
"biz.aQute.gogo.commands.provider.LoggerAdminCommands":{
        //# Component properties
        /*
         * Type = String
         * Default = "logadmin"
         */
         // "osgi.command.scope": null,

        /*
         * Type = String[]
         * Default = ["levels", "defaultlevel", "addlevel", "rmlevel", "slf4jdebug", "slf4jinfo", "slf4jwarn", "slf4jerror", "tail"]
         */
         // "osgi.command.function": null,


        //# Reference bindings
        // "$000.target": "(component.pid=*)",
        // "$001.target": "(component.pid=*)"


        //# ObjectClassDefinition - Attributes
        // (No PidOcd available.)
}
```

## Developers

* **Peter Kriens** (osgi) / [info@osgi.org](mailto:info@osgi.org) @ OSGi Alliance

## Licenses

**http://opensource.org/licenses/apache2.0.php**
  > Apache License, Version 2.0
  >
  > For more information see [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Copyright

aQute SARL All Rights Reserved

---
aQute SARL