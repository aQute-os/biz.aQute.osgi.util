# biz.aQute.authenticator.useradmin.provider

## Links

* [Documentation](https://aQute.biz)
* [Source Code](https://github.com/aQute-os/biz.aQute.osgi.util) (clone with `scm:git:git@github.com/aQute-os/biz.aQute.osgi.util.git`)

## Coordinates

### Maven

```xml
<dependency>
    <groupId>biz.aQute</groupId>
    <artifactId>biz.aQute.authenticator.useradmin.provider</artifactId>
    <version>1.3.0-SNAPSHOT</version>
</dependency>
```

### OSGi

```
Bundle Symbolic Name: biz.aQute.authenticator.useradmin.provider
Version             : 1.3.0.202101120018
```

### Feature-Coordinate

```
"bundles": [
   {
    "id": "biz.aQute:biz.aQute.authenticator.useradmin.provider:1.3.0-SNAPSHOT"
   }
]
```

## Gogo Commands

### user:hash

**Synopsis**

`user:hash ARG`

**Arguments**

* `ARG`  

### user:passwd

**Synopsis**

`user:passwd ARG1 ARG2`

**Arguments**

* `ARG1`  
* `ARG2`  

### user:adduser

**Synopsis**

`user:adduser ARG`

**Arguments**

* `ARG`  

### user:rmrole

**Synopsis**

`user:rmrole ARG...`

**Arguments**

* `ARG`  

### user:role

**Synopsis**

`user:role ARG...`

**Arguments**

* `ARG`  

### user:user

**Synopsis**

`user:user`

## Components

### biz.aQute.authenticator.useradmin.provider.UserAdminAuthenticator - *state = enabled, activation = delayed*

#### Description

#### Services - *scope = singleton*

|Interface name |
|--- |
|biz.aQute.authentication.api.Authenticator |

#### Properties

|Name |Type |Value |
|--- |--- |--- |
|salt |Byte[] |[47, 104, -53, 117, 108, -15, 116, -124, 42, -17] |
|iterations |Integer |997 |
|algorithm |String |"PBKDF2WithHmacSHA1" |
|.root |String |"" |
|osgi.command.scope |String |"user" |
|osgi.command.function |String[] |["hash", "passwd", "adduser", "rmrole", "role", "user"] |

#### Configuration - *policy = optional*

##### Pid: `biz.aQute.authenticator.useradmin.provider.UserAdminAuthenticator`

No information available.

#### Reference bindings

|Attribute |Value |
|--- |--- |
|name |userAdmin |
|interfaceName |org.osgi.service.useradmin.UserAdmin |
|target | |
|cardinality |1..1 |
|policy |static |
|policyOption |reluctant |
|scope |bundle |

#### OSGi-Configurator


```
/*
 * Component: biz.aQute.authenticator.useradmin.provider.UserAdminAuthenticator
 * policy:    optional
 */
"biz.aQute.authenticator.useradmin.provider.UserAdminAuthenticator":{
        //# Component properties
        /*
         * Type = Byte[]
         * Default = [47, 104, -53, 117, 108, -15, 116, -124, 42, -17]
         */
         // "salt": null,

        /*
         * Type = Integer
         * Default = 997
         */
         // "iterations": null,

        /*
         * Type = String
         * Default = "PBKDF2WithHmacSHA1"
         */
         // "algorithm": null,

        /*
         * Type = String
         * Default = ""
         */
         // ".root": null,

        /*
         * Type = String
         * Default = "user"
         */
         // "osgi.command.scope": null,

        /*
         * Type = String[]
         * Default = ["hash", "passwd", "adduser", "rmrole", "role", "user"]
         */
         // "osgi.command.function": null,


        //# Reference bindings
        // "userAdmin.target": "(component.pid=*)"


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