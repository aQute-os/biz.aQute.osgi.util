
# biz.aQute.osgi.configuration.util

This project contains a helper to configure Configuration Admin in a type
safe way. It allows you create and update configurations without ever using
property keys as strings.

## Example

    @interface FooConfig {
        int port() default 10;

        String host() default "localhost";
    }

    @Test
    public void testSimple() throws Exception {
        ConfigHelper<FooConfig> ch = new ConfigHelper<>(FooConfig.class, cm);
        Map<String, Object> read = ch.read("foo.bar");
        assertEquals(0, read.size());
        
        assertEquals( 10, ch.d().port());
        assertEquals( "localhost", ch.d().host());
        
        ch.set( ch.d().port(), 3400);
        ch.set( ch.d().host(), "example.com");
        ch.update();

        Configuration c = cm.getConfiguration("foo.bar");
        Dictionary<String,Object> properties = c.getProperties();
        assertEquals( 3400, properties.get("port"));
        assertEquals( "example.com", properties.get("host"));
        
    }
    
## References

