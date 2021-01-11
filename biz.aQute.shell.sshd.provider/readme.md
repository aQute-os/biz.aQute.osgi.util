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
Version             : 1.3.0.202101071841
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

### biz.aQute.shell.sshd.provider.GogoSshd - *state = enabled, activation = immediate*

#### Description

#### Services

No services.

#### Properties

|Name |Type |Value |
|--- |--- |--- |
|port |Integer |8061 |
|address |String |"localhost" |

#### Configuration - *policy = optional*

##### Pid: `biz.aQute.shell.sshd`

No information available.

#### Reference bindings

|Attribute |Value |
|--- |--- |
|name |$000 |
|interfaceName |org.apache.felix.service.command.CommandProcessor |
|target | |
|cardinality |1..1 |
|policy |static |
|policyOption |reluctant |
|scope |bundle |

#### OSGi-Configurator


```
/*
 * Component: biz.aQute.shell.sshd.provider.GogoSshd
 * policy:    optional
 */
"biz.aQute.shell.sshd":{
        //# Component properties
        /*
         * Type = Integer
         * Default = 8061
         */
         // "port": null,

        /*
         * Type = String
         * Default = "localhost"
         */
         // "address": null,


        //# Reference bindings
        // "$000.target": "(component.pid=*)"


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