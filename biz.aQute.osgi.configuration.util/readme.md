# biz.aQute.osgi.configuration.util

Implements a Configuration Helper to set the properties in a type safe way.

## Links

* [Documentation](https://aQute.biz)
* [Source Code](https://github.com/aQute-os/biz.aQute.osgi.util) (clone with `scm:git:git@github.com/aQute-os/biz.aQute.osgi.util.git`)

## Coordinates

### Maven

```xml
<dependency>
    <groupId>biz.aQute</groupId>
    <artifactId>biz.aQute.osgi.configuration.util</artifactId>
    <version>1.3.0-SNAPSHOT</version>
</dependency>
```

### OSGi

```
Bundle Symbolic Name: biz.aQute.osgi.configuration.util
Version             : 1.0.0.202101120018
```

### Feature-Coordinate

```
"bundles": [
   {
    "id": "biz.aQute:biz.aQute.osgi.configuration.util:1.3.0-SNAPSHOT"
   }
]
```

## Code Usage

### ConfigHelper

This shows how the ConfigHelper could be used in a Unit-Test.

```java
public class Readme {

  ConfigurationAdmin cm;

  @interface FooConfig {

    int port() default 10;

    String host() default "localhost";
  }

  @Test
  public void testSimple() throws Exception {
    ConfigHelper<FooConfig> ch = new ConfigHelper<>(FooConfig.class, cm);
    Map<String, Object> read = ch.read("foo.bar");
    assertEquals(0, read.size());
    assertEquals(10, ch.d().port());
    assertEquals("localhost", ch.d().host());
    ch.set(ch.d().port(), 3400);
    ch.set(ch.d().host(), "example.com");
    ch.update();
    Configuration c = cm.getConfiguration("foo.bar");
    Dictionary<String, Object> properties = c.getProperties();
    assertEquals(3400, properties.get("port"));
    assertEquals("example.com", properties.get("host"));
  }
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