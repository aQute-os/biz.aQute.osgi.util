# biz.aQute.kibana

## Links

* [Documentation](https://aQute.biz)
* [Source Code](https://github.com/aQute-os/biz.aQute.osgi.util) (clone with `scm:git:git@github.com/aQute-os/biz.aQute.osgi.util.git`)

## Coordinates

### Maven

```xml
<dependency>
    <groupId>biz.aQute</groupId>
    <artifactId>biz.aQute.kibana</artifactId>
    <version>1.3.0-SNAPSHOT</version>
</dependency>
```

### OSGi

```
Bundle Symbolic Name: biz.aQute.kibana
Version             : 1.3.0.202101071841
```

### Feature-Coordinate

```
"bundles": [
   {
    "id": "biz.aQute:biz.aQute.kibana:1.3.0-SNAPSHOT"
   }
]
```

## Components

### biz.aQute.kibana.KibanaLogUploader - *state = enabled, activation = immediate*

#### Description

#### Services

No services.

#### Properties

|Name |Type |Value |
|--- |--- |--- |
|delay |Integer |30 |

#### Configuration - *policy = require*

##### Factory Pid: `biz.aQute.kibana`

|Attribute |Value |
|--- |--- |
|Id |`hosts` |
|Required |**true** |
|Type |**String[]** |
|Description |List of URIs to the Elastic search host. This is the scheme + host + port. The path is discarded |

|Attribute |Value |
|--- |--- |
|Id |`password` |
|Required |**true** |
|Type |**String** |
|Description |Password for Elastic search |

|Attribute |Value |
|--- |--- |
|Id |`userid` |
|Required |**true** |
|Type |**String** |
|Description |User id for Elastic search |

|Attribute |Value |
|--- |--- |
|Id |`delay` |
|Required |**true** |
|Type |**Integer** |
|Description |Buffering delay in seconds before records are pushed |
|Default |30 |
|Value range |`min = 5` |

|Attribute |Value |
|--- |--- |
|Id |`index` |
|Required |**true** |
|Type |**String** |
|Description |The log index to use |

#### Reference bindings

|Attribute |Value |
|--- |--- |
|name |$000 |
|interfaceName |org.osgi.service.log.LogReaderService |
|target | |
|cardinality |1..1 |
|policy |static |
|policyOption |reluctant |
|scope |bundle |

#### OSGi-Configurator


```
/*
 * Component: biz.aQute.kibana.KibanaLogUploader
 * policy:    require
 */
"biz.aQute.kibana~FactoryNameChangeIt":{
        //# Component properties
        /*
         * Type = Integer
         * Default = 30
         */
         // "delay": null,


        //# Reference bindings
        // "$000.target": "(component.pid=*)",


        //# ObjectClassDefinition - Attributes
        /*
         * Required = true
         * Type = String[]
         * Description = List of URIs to the Elastic search host. This is the scheme + host + port. The path is discarded
         */
         "hosts": null,

        /*
         * Required = true
         * Type = String
         * Description = Password for Elastic search
         */
         "password": null,

        /*
         * Required = true
         * Type = String
         * Description = User id for Elastic search
         */
         "userid": null,

        /*
         * Required = true
         * Type = Integer
         * Description = Buffering delay in seconds before records are pushed
         * Default = 30
         * Value restriction = `min = 5` / `max = `
         */
         "delay": null,

        /*
         * Required = true
         * Type = String
         * Description = The log index to use
         */
         "index": null
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