# biz.aQute.trace.gui

## Links

* [Documentation](https://aQute.biz)
* [Source Code](https://github.com/aQute-os/biz.aQute.osgi.util) (clone with `scm:git:git@github.com/aQute-os/biz.aQute.osgi.util.git`)

## Coordinates

### Maven

```xml
<dependency>
    <groupId>biz.aQute</groupId>
    <artifactId>biz.aQute.trace.gui</artifactId>
    <version>1.3.0-SNAPSHOT</version>
</dependency>
```

### OSGi

```
Bundle Symbolic Name: biz.aQute.trace.gui
Version             : 1.3.0.202101120015
```

### Feature-Coordinate

```
"bundles": [
   {
    "id": "biz.aQute:biz.aQute.trace.gui:1.3.0-SNAPSHOT"
   }
]
```

## Gogo Commands

### trace:trace

**Synopsis**

`trace:trace ARG`

**Description**

Trace a method in a class

**Arguments**

* `ARG`  <class-fqn>:<method>:<action>

---

**Synopsis**

`trace:trace`

---

**Synopsis**

`trace:trace ARG`

**Description**

Trace a method in a class

**Arguments**

* `ARG`  <class-fqn>:<method>:<action>

---

**Synopsis**

`trace:trace`

### trace:clear

**Synopsis**

`trace:clear`

**Description**

Clear the monitor event queue

### trace:traces

**Synopsis**

`trace:traces`

**Description**

Show the monitor queue

### trace:dump

**Synopsis**

`trace:dump ARG`

**Description**

Show the monitor queue in specific format: json html or text (default)

**Arguments**

* `ARG`  

### trace:untrace

**Synopsis**

`trace:untrace ARG`

**Description**

Trace a method in a class, format: trace <fqn>:<method>:<action> or trace <fqn>

**Arguments**

* `ARG`  <class-fqn>:<method>:<action> or 

### trace:debug

**Synopsis**

`trace:debug`

### trace:man

**Synopsis**

`trace:man`

## Components

### biz.aQute.trace.gui.TraceMonitor - *state = enabled, activation = delayed*

#### Description

#### Services - *scope = singleton*

|Interface name |
|--- |
|org.apache.felix.inventory.InventoryPrinter |

#### Properties

|Name |Type |Value |
|--- |--- |--- |
|osgi.command.scope |String |"trace" |
|osgi.command.function |String[] |["trace", "clear", "traces", "dump", "trace", "untrace", "debug", "man"] |
|felix.inventory.printer.name |String |"biz-aQute-trace" |
|felix.inventory.printer.title |String |"Tracer" |
|felix.inventory.printer.format |String[] |["TEXT", "HTML", "JSON"] |

#### Configuration - *policy = optional*

##### Pid: `biz.aQute.trace.gui.TraceMonitor`

No information available.

#### Reference bindings

No bindings.

#### OSGi-Configurator


```
/*
 * Component: biz.aQute.trace.gui.TraceMonitor
 * policy:    optional
 */
"biz.aQute.trace.gui.TraceMonitor":{
        //# Component properties
        /*
         * Type = String
         * Default = "trace"
         */
         // "osgi.command.scope": null,

        /*
         * Type = String[]
         * Default = ["trace", "clear", "traces", "dump", "trace", "untrace", "debug", "man"]
         */
         // "osgi.command.function": null,

        /*
         * Type = String
         * Default = "biz-aQute-trace"
         */
         // "felix.inventory.printer.name": null,

        /*
         * Type = String
         * Default = "Tracer"
         */
         // "felix.inventory.printer.title": null,

        /*
         * Type = String[]
         * Default = ["TEXT", "HTML", "JSON"]
         */
         // "felix.inventory.printer.format": null,


        //# Reference bindings
        // none

        //# ObjectClassDefinition - Attributes
        // (No PidOcd available.)
}
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