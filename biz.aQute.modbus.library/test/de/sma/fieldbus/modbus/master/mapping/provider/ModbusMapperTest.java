package de.sma.fieldbus.modbus.master.mapping.provider;

import java.net.URL;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import de.sma.fieldbus.metadata.mapper.util.MetadataMapper.Mapper;
import de.sma.fieldbus.modbus.core.provider.ADU;
import de.sma.fieldbus.modbus.core.provider.MBAP;
import de.sma.fieldbus.modbus.core.provider.ADU.WordOrder;
import de.sma.fieldbus.modbus.master.mapping.provider.ModbusMapper;
import de.sma.fieldbus.modbus.master.mapping.provider.ModbusMapper.GroupUpdate;
import de.sma.fieldbus.modbus.master.mapping.provider.ModbusMapper.ModbusMap;
import de.sma.iguana.test.util.DummyFramework;
import de.sma.iguana.test.util.DummyTransaction;

public class ModbusMapperTest extends Assert
{

   final DummyFramework framework = new DummyFramework();
   final ADU adu = new MBAP(WordOrder.bigEndian, null);

   @After
   public void tearDown() throws Exception {
      framework.close();
   }
   
   @Test
   public void testParse() throws BundleException, Exception
   {
      URL url = ModbusMapperTest.class.getResource("mapper-simple-test.json");
      ModbusMap map = ModbusMapper.parse0(url);
      assertNotNull(map);
      
      assertTrue( map.bigEndiannes );
      assertEquals( 13, map.groups.length );
      assertEquals( 1, map.groups[0].samplingGroup );
      assertEquals( 1, map.groups[1].samplingGroup );
      assertEquals( 1, map.groups[2].samplingGroup );
      assertEquals( 1, map.groups[3].samplingGroup );
      assertEquals( 1, map.groups[4].samplingGroup );
      assertEquals( 1, map.groups[5].samplingGroup );
      assertEquals( 1, map.groups[6].samplingGroup );
      assertEquals( 2, map.groups[7].samplingGroup );
      assertEquals( 2, map.groups[8].samplingGroup );
      assertEquals( 2, map.groups[9].samplingGroup );
      assertEquals( 2, map.groups[10].samplingGroup );
      assertEquals( 2, map.groups[11].samplingGroup );
      assertEquals( 2, map.groups[12].samplingGroup );
      
      assertEquals( 40036, map.groups[0].startingRegister );
      assertEquals( 32, map.groups[0].quantity );
      assertEquals( "C001", map.groups[0].name );
      assertEquals( 3, map.groups[0].items.size() );

   }
   
   @Test
   public void testBasicAccess() throws BundleException, Exception
   {
      URL url = ModbusMapperTest.class.getResource("mapper-access.json");
      ModbusMap map = ModbusMapper.parse0(url);
      assertNotNull(map);
      ByteBuffer buffer = adu.readHoldingRegistersResponse( width("MODEL",16), width("PkgRev",16), width("SerNum",32));
      
      DummyTransaction transaction = new DummyTransaction("test:device");
      map.groups[0].update(buffer, transaction);
      assertFalse( transaction.isEmpty());
      
      assertEquals( "MODEL___|_______", transaction.getValue("Parameter.Nameplate.Model", "PV", String.class));
      assertEquals( "PkgRev__|_______", transaction.getValue("Parameter.Nameplate.PkgRev", "PV", String.class));
      assertEquals( "SerNum__|_______|_______|_______", transaction.getValue("Parameter.Nameplate.SerNum", "PV", String.class));
   }
   
   @Test
   public void testAccessTypes() throws BundleException, Exception
   {
      URL url = ModbusMapperTest.class.getResource("mapper-access-types.json");
      ModbusMap map = ModbusMapper.parse0(url);
      assertNotNull(map);

      // ipv6
      // ipv4
      
      testAccess( map, "int32", check("int32", 1, 1));
      testAccess( map, "int16", check("int16", (short) 1, 1));
      testAccess( map, "int16", check("int16", (short) -1, -1));
      testAccess( map, "uint16", check("uint16", (short) 0xAAAA, 0xAAAA));
      testAccess( map, "uint16", check("uint16", (short) -1, 0xFFFF));
      testAccess( map, "uint16", check("uint16", (short) 1, 1));
      testAccess( map, "uint32", check("uint32", (int) 1, 1L));
     // testAccess( map, "uint32", check("uint32", (int) 0xFFFF, (long)0xFFFF));
   }
   



   private void testAccess(ModbusMap map, String group, Check ... check) throws IllegalAccessException
   {
      Object [] objects = new Object[ check.length];
      for ( int i=0; i<check.length; i++)
         objects[i]= check[i].input;
      
      for ( GroupUpdate x : map.groups) {
         if ( x.name.equals(group)) {
            ByteBuffer buffer = adu.readHoldingRegistersResponse(objects);
            DummyTransaction dt = new DummyTransaction("test:device");
            x.update(buffer, dt);
            
            for ( Check c : check) {
               assertEquals( c.name, c.output, dt.getValue(c.name, "PV"));
            }
            return;
         }
      }
      fail("Could not find group " + group);
   }




   static class Check {
      String name;
      Object output;
      Object input;
   }
   
   Check check( String name, Object input, Object output) {
      Check c = new Check();
      c.name = name;
      c.input = input;
      c.output = output;
      return c;
   }
   
   private String width(String string, int length)
   {
      StringBuilder sb = new StringBuilder(string);
      for ( int i=sb.length(); i<length; i++) {
         boolean isDivider = (sb.length() % 8 )== 0;
         if ( isDivider)
            sb.append("|");
         else
            sb.append("_");
      }
      return sb.toString();
   }

   @Test
   public void testSimple() throws BundleException, Exception
   {
      ModbusMapper mm = new ModbusMapper(framework.context);
      URL url = ModbusMapperTest.class.getResource("mapper-simple-test.json");
      Bundle bundle = framework.
         bundle().
         addResource(ModbusMapper.SMA_METADATA_MODBUS + "/test.json",
                  url).
         install();

      bundle.start();
      assertEquals( Bundle.ACTIVE, bundle.getState());
      
      Thread.sleep(400);
      
      Mapper<ModbusMap> mapper = mm.getMapper("test");
      assertNotNull(mapper);
      assertNotNull(mapper.get());
   }
}
