# biz.aQute.osgi.conditionaltarget

## Links

* [Documentation](https://aQute.biz)
* [Source Code](https://github.com/aQute-os/biz.aQute.osgi.util) (clone with `scm:git:git@github.com/aQute-os/biz.aQute.osgi.util.git`)

## Coordinates

### Maven

```xml
<dependency>
    <groupId>biz.aQute</groupId>
    <artifactId>biz.aQute.osgi.conditionaltarget</artifactId>
    <version>1.3.0-SNAPSHOT</version>
</dependency>
```

### OSGi

```
Bundle Symbolic Name: biz.aQute.osgi.conditionaltarget
Version             : 1.3.0.202101120015
```

### Feature-Coordinate

```
"bundles": [
   {
    "id": "biz.aQute:biz.aQute.osgi.conditionaltarget:1.3.0-SNAPSHOT"
   }
]
```

## Components

### aQute.osgi.conditionaltarget.provider.CTServerComponent$ConditionalTargetDummy - *state = not enabled, activation = delayed*

#### Description

#### Services - *scope = singleton*

|Interface name |
|--- |
|aQute.osgi.conditionaltarget.api.ConditionalTarget |

#### Properties

No properties.

#### Configuration - *policy = optional*

##### Pid: `aQute.osgi.conditionaltarget.provider.CTServerComponent$ConditionalTargetDummy`

No information available.

#### Reference bindings

No bindings.

#### OSGi-Configurator


```
/*
 * Component: aQute.osgi.conditionaltarget.provider.CTServerComponent$ConditionalTargetDummy
 * policy:    optional
 */
"aQute.osgi.conditionaltarget.provider.CTServerComponent$ConditionalTargetDummy":{
        //# Component properties
        // none

        //# Reference bindings
        // none

        //# ObjectClassDefinition - Attributes
        // (No PidOcd available.)
}
```

---

### aQute.osgi.conditionaltarget.provider.CTServerComponent - *state = enabled, activation = immediate*

#### Description

#### Services

No services.

#### Properties

No properties.

#### Configuration - *policy = optional*

##### Pid: `aQute.osgi.conditionaltarget.provider.CTServerComponent`

No information available.

#### Reference bindings

|Attribute |Value |
|--- |--- |
|name |scr |
|interfaceName |org.osgi.service.component.runtime.ServiceComponentRuntime |
|target | |
|cardinality |1..1 |
|policy |static |
|policyOption |reluctant |
|scope |bundle |

#### OSGi-Configurator


```
/*
 * Component: aQute.osgi.conditionaltarget.provider.CTServerComponent
 * policy:    optional
 */
"aQute.osgi.conditionaltarget.provider.CTServerComponent":{
        //# Component properties
        // none

        //# Reference bindings
        // "scr.target": "(component.pid=*)"


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