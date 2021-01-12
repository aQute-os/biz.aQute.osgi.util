# biz.aQute.shell.sshd.provider

An SSH daemon that interacts with Gogo. Since many Linux distributions have removed telnet,there is a need to interact with Gogo using ssh,which is still approved. This ssh provider can be used without authentication when registered only on localhost. Otherwise the user should register an biz.aQute.authentication.api.Authenticator service. The configuration by default requires this optional service to be there.

## Links

* [Documentation](https://aQute.biz)
* [Source Code](https://github.com/aQute-os/biz.aQute.osgi.util) (clone with `scm:git:git@github.com/aQute-os/biz.aQute.osgi.util.git`)

## Coordinates

### Maven

```xml
<dependency>
    <groupId>biz.aQute</groupId>
    <artifactId>biz.aQute.shell.sshd.provider</artifactId>
    <version>1.3.0-SNAPSHOT</version>
</dependency>
```

### OSGi

```
Bundle Symbolic Name: biz.aQute.shell.sshd.provider
Version             : 1.3.0.202101120015
```

### Feature-Coordinate

```
"bundles": [
   {
    "id": "biz.aQute:biz.aQute.shell.sshd.provider:1.3.0-SNAPSHOT"
   }
]
```

## Components

### biz.aQute.shell.sshd.provider.GogoSshdSecure - *state = enabled, activation = immediate*

#### Description

#### Services

No services.

#### Properties

|Name |Type |Value |
|--- |--- |--- |
|port |Integer |8062 |
|address |String |"0.0.0.0" |
|hostkey |String |"target/hostkey.ser" |
|passwords |Boolean |false |
|permission |String |"gogo.command:none" |

#### Configuration - *policy = require*

##### Factory Pid: `biz.aQute.shell.sshd`

|Attribute |Value |
|--- |--- |
|Id |`port` |
|Required |**true** |
|Type |**Integer** |
|Default |8062 |

|Attribute |Value |
|--- |--- |
|Id |`address` |
|Required |**true** |
|Type |**String** |
|Default |"0.0.0.0" |

|Attribute |Value |
|--- |--- |
|Id |`hostkey` |
|Required |**true** |
|Type |**String** |
|Default |"target/hostkey.ser" |

|Attribute |Value |
|--- |--- |
|Id |`passwords` |
|Required |**true** |
|Type |**Boolean** |
|Default |false |

|Attribute |Value |
|--- |--- |
|Id |`permission` |
|Required |**true** |
|Type |**String** |
|Default |"gogo.command:none" |

#### Reference bindings

|Attribute |Value |
|--- |--- |
|name |$001 |
|interfaceName |org.apache.felix.service.command.CommandProcessor |
|target | |
|cardinality |1..1 |
|policy |static |
|policyOption |reluctant |
|scope |bundle ||Attribute |Value |
|--- |--- |
|name |$002 |
|interfaceName |biz.aQute.authentication.api.Authenticator |
|target | |
|cardinality |1..1 |
|policy |static |
|policyOption |reluctant |
|scope |bundle ||Attribute |Value |
|--- |--- |
|name |$003 |
|interfaceName |biz.aQute.authorization.api.Authority |
|target | |
|cardinality |1..1 |
|policy |static |
|policyOption |reluctant |
|scope |bundle ||Attribute |Value |
|--- |--- |
|name |$004 |
|interfaceName |biz.aQute.authorization.api.AuthorityAdmin |
|target | |
|cardinality |1..1 |
|policy |static |
|policyOption |reluctant |
|scope |bundle |

#### OSGi-Configurator


```
/*
 * Component: biz.aQute.shell.sshd.provider.GogoSshdSecure
 * policy:    require
 */
"biz.aQute.shell.sshd~FactoryNameChangeIt":{
        //# Component properties
        /*
         * Type = Integer
         * Default = 8062
         */
         // "port": null,

        /*
         * Type = String
         * Default = "0.0.0.0"
         */
         // "address": null,

        /*
         * Type = String
         * Default = "target/hostkey.ser"
         */
         // "hostkey": null,

        /*
         * Type = Boolean
         * Default = false
         */
         // "passwords": null,

        /*
         * Type = String
         * Default = "gogo.command:none"
         */
         // "permission": null,


        //# Reference bindings
        // "$001.target": "(component.pid=*)",
        // "$002.target": "(component.pid=*)",
        // "$003.target": "(component.pid=*)",
        // "$004.target": "(component.pid=*)",


        //# ObjectClassDefinition - Attributes
        /*
         * Required = true
         * Type = Integer
         * Default = 8062
         */
         "port": null,

        /*
         * Required = true
         * Type = String
         * Default = "0.0.0.0"
         */
         "address": null,

        /*
         * Required = true
         * Type = String
         * Default = "target/hostkey.ser"
         */
         "hostkey": null,

        /*
         * Required = true
         * Type = Boolean
         * Default = false
         */
         "passwords": null,

        /*
         * Required = true
         * Type = String
         * Default = "gogo.command:none"
         */
         "permission": null
}
```

---

### biz.aQute.shell.sshd.provider.GogoSshdInsecure - *state = enabled, activation = immediate*

#### Description

#### Services

No services.

#### Properties

|Name |Type |Value |
|--- |--- |--- |
|port |Integer |8061 |
|hostkey |String |"target/host.ser" |

#### Configuration - *policy = require*

##### Factory Pid: `biz.aQute.shell.sshd.insecure`

|Attribute |Value |
|--- |--- |
|Id |`port` |
|Required |**true** |
|Type |**Integer** |
|Default |8062 |

|Attribute |Value |
|--- |--- |
|Id |`address` |
|Required |**true** |
|Type |**String** |
|Default |"0.0.0.0" |

|Attribute |Value |
|--- |--- |
|Id |`hostkey` |
|Required |**true** |
|Type |**String** |
|Default |"target/hostkey.ser" |

|Attribute |Value |
|--- |--- |
|Id |`passwords` |
|Required |**true** |
|Type |**Boolean** |
|Default |false |

|Attribute |Value |
|--- |--- |
|Id |`permission` |
|Required |**true** |
|Type |**String** |
|Default |"gogo.command:none" |

#### Reference bindings

|Attribute |Value |
|--- |--- |
|name |$001 |
|interfaceName |org.apache.felix.service.command.CommandProcessor |
|target | |
|cardinality |1..1 |
|policy |static |
|policyOption |reluctant |
|scope |bundle |

#### OSGi-Configurator


```
/*
 * Component: biz.aQute.shell.sshd.provider.GogoSshdInsecure
 * policy:    require
 */
"biz.aQute.shell.sshd.insecure~FactoryNameChangeIt":{
        //# Component properties
        /*
         * Type = Integer
         * Default = 8061
         */
         // "port": null,

        /*
         * Type = String
         * Default = "target/host.ser"
         */
         // "hostkey": null,


        //# Reference bindings
        // "$001.target": "(component.pid=*)",


        //# ObjectClassDefinition - Attributes
        /*
         * Required = true
         * Type = Integer
         * Default = 8062
         */
         "port": null,

        /*
         * Required = true
         * Type = String
         * Default = "0.0.0.0"
         */
         "address": null,

        /*
         * Required = true
         * Type = String
         * Default = "target/hostkey.ser"
         */
         "hostkey": null,

        /*
         * Required = true
         * Type = Boolean
         * Default = false
         */
         "passwords": null,

        /*
         * Required = true
         * Type = String
         * Default = "gogo.command:none"
         */
         "permission": null
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