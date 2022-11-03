package biz.aQute.modbus.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import biz.aQute.modbus.api.PDU.Bits;
import biz.aQute.modbus.api.PDU.BlockLength;
import biz.aQute.modbus.api.PDU.DataType;
import biz.aQute.modbus.api.PDU.Entry;

public class PDUTest {

	@Test
	public void testInitial() {
		PDU pdu = new PDU(10);
		assertThat(pdu.offset).isEqualTo(0);
		assertThat(pdu.absPosition).isEqualTo(0);
		assertThat(pdu.limit).isEqualTo(10);
		assertThat(pdu.bigByteEndian).isTrue();
		assertThat(pdu.bigWordEndian).isTrue();
		assertThat(pdu.buffer.length).isEqualTo(10);
	}

	@Test
	public void testFlip() {
		for (boolean forByte : new boolean[] {
			false, true
		}) {
			for (boolean forWord : new boolean[] {
				false, true
			}) {
				PDU pdu = new PDU.Builder().bigByteEndian(forByte)
					.bigWordEndian(forWord)
					.size(100)
					.offset(10)
					.capacity(20)
					.build();
				pdu.putU8(0);
				assertThat(pdu.position()).isEqualTo(1);
				assertThat(pdu.absPosition).isEqualTo(11);
				assertThat(pdu.limit).isEqualTo(20);
				pdu.seal();
				assertThat(pdu.position()).isEqualTo(0);
				assertThat(pdu.absPosition).isEqualTo(10);
				assertThat(pdu.limit).isEqualTo(11);
				assertThat(pdu.limit()).isEqualTo(1);

				pdu.reset();
				assertThat(pdu.position()).isEqualTo(0);
				assertThat(pdu.absPosition).isEqualTo(10);
				assertThat(pdu.limit).isEqualTo(20);
			}
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLimit() {
		PDU pdu = new PDU(10);
		pdu.putU8(0);
		pdu.seal();
		pdu.putU16(0);
	}

	@Test
	public void testSignsGetFixed() {
		for (int offset : new int[] {
			10, 0, 3
		}) {
			System.out.println("offset " + offset);
			for (boolean forByte : new boolean[] {
				false, true
			}) {
				for (boolean forWord : new boolean[] {
					false, true
				}) {

					PDU pdu = new PDU.Builder().bigByteEndian(forByte)
						.bigWordEndian(forWord)
						.size(100)
						.offset(offset)
						.build();

					pdu.putU8(255);
					assertThat(pdu.position()).isEqualTo(1);
					assertThat(pdu.getU8(0)).isEqualTo(255);

					pdu.putU8(128);
					assertThat(pdu.position()).isEqualTo(2);
					assertThat(pdu.getU8(1)).isEqualTo(128);

					pdu.putU16(0xFFAA);
					assertThat(pdu.getU16(2)).isEqualTo(0xFFAA);
					assertThat(pdu.position()).isEqualTo(4);

					pdu.putU32(0xF1020304L);
					assertThat(pdu.getU32(4)).isEqualTo(0xF1020304L);
					assertThat(pdu.position()).isEqualTo(8);

					pdu.putI64(0xF102030405060708L);
					long lng = pdu.getI64(8);
					assertThat(lng).isEqualTo(0xF102030405060708L);
					assertThat(pdu.position()).isEqualTo(16);

					pdu.putI32(0xF1020304);
					int tmp = pdu.getI32(16);
					assertThat(tmp).isEqualTo(0xF1020304);

					pdu.putFloat(0.34578e12f);
					assertThat(pdu.getFloat(20)).isEqualTo(0.34578e12f);
					assertThat(pdu.position()).isEqualTo(24);

					pdu.putDouble(0.34578e12);
					assertThat(pdu.getDouble(24)).isEqualTo(0.34578e12);
					assertThat(pdu.position()).isEqualTo(32);

					pdu.putI8(-23);
					assertThat(pdu.getI8(32)).isEqualTo(-23);
					assertThat(pdu.position()).isEqualTo(33);

					pdu.putI16(-15);
					assertThat(pdu.getI16(33)).isEqualTo(-15);
					assertThat(pdu.position()).isEqualTo(35);

					pdu.putI32(-15);
					assertThat(pdu.getI32(35)).isEqualTo(-15);
					assertThat(pdu.position()).isEqualTo(39);

					pdu.putString("abc", 8);
					assertThat(pdu.getString(39, 8)).isEqualTo("abc");
					assertThat(pdu.position()).isEqualTo(47);
				}
			}
		}
	}

	@Test
	public void testSignsPutFixed() {
		for (boolean forByte : new boolean[] {
			false, true
		}) {
			for (boolean forWord : new boolean[] {
				false, true
			}) {

				PDU pdu = new PDU.Builder().bigByteEndian(forByte)
					.bigWordEndian(forWord)
					.size(100)
					.build();

				pdu.putU8(0, 255);
				assertThat(pdu.getU8()).isEqualTo(255);
				assertThat(pdu.position()).isEqualTo(1);

				pdu.putU8(1, 128);
				assertThat(pdu.getU8()).isEqualTo(128);
				assertThat(pdu.position()).isEqualTo(2);

				pdu.putU16(2, 0xFFAA);
				assertThat(pdu.getU16()).isEqualTo(0xFFAA);
				assertThat(pdu.position()).isEqualTo(4);

				pdu.putU32(4, 0xF1020304L);
				assertThat(pdu.getU32()).isEqualTo(0xF1020304L);
				assertThat(pdu.position()).isEqualTo(8);

				pdu.putI64(8, 0xF102030405060708L);
				long lng = pdu.getI64();
				assertThat(lng).isEqualTo(0xF102030405060708L);
				assertThat(pdu.position()).isEqualTo(16);

				pdu.putI32(16, 0xF1020304);
				int tmp = pdu.getI32();
				assertThat(tmp).isEqualTo(0xF1020304);

				pdu.putFloat(20, 0.34578e12f);
				assertThat(pdu.getFloat()).isEqualTo(0.34578e12f);
				assertThat(pdu.position()).isEqualTo(24);

				pdu.putDouble(24, 0.34578e12);
				assertThat(pdu.getDouble()).isEqualTo(0.34578e12);
				assertThat(pdu.position()).isEqualTo(32);

				pdu.putI8(32, -23);
				assertThat(pdu.getI8()).isEqualTo(-23);
				assertThat(pdu.position()).isEqualTo(33);

				pdu.putI16(33, -15);
				assertThat(pdu.getI16()).isEqualTo(-15);
				assertThat(pdu.position()).isEqualTo(35);

				pdu.putI32(35, -15);
				assertThat(pdu.getI32()).isEqualTo(-15);
				assertThat(pdu.position()).isEqualTo(39);

				pdu.putString(39, "abc", 8);
				assertThat(pdu.getString(8)).isEqualTo("abc");
				assertThat(pdu.position()).isEqualTo(47);
			}
		}
	}

	@Test
	public void testBlocklength() {
		PDU pdu = new PDU(100);
		pdu.putI64(0);
		BlockLength b1 = pdu.putBlockLengthU16();
		pdu.putI64(12345);
		pdu.putI64(-3);
		int l = b1.close();
		assertThat(l).isEqualTo(16);

		pdu.seal();

		assertThat(pdu.getI64()).isEqualTo(0L);
		assertThat(pdu.getU16()).isEqualTo(16);
		assertThat(pdu.getI64()).isEqualTo(12345L);
		assertThat(pdu.getI64()).isEqualTo(-3L);

	}

	@Test
	public void testEntryIntAndBooleans() {
		PDU pdu = new PDU(100);
		PDU.Entry i16 = new Entry(DataType.i16, 2, 0);
		PDU.Entry i64 = new Entry(DataType.i64, 8, 0);
		PDU.Entry u32 = new Entry(DataType.u32, 4, 0);
		pdu.put(i16, true);
		pdu.put(i64, false);
		pdu.put(u32, false);
		pdu.seal();

		assertThat(pdu.getI16()).isEqualTo(1);
		assertThat(pdu.getI64()).isEqualTo(0L);
		assertThat(pdu.getU32()).isEqualTo(0L);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBlocklengthTooMuch() {
		PDU pdu = new PDU(300);
		pdu.putI64(0);
		BlockLength bl = pdu.putBlockLengthU8();
		for (int i = 0; i < 33; i++)
			pdu.putI64(12345);
		bl.close();
	}

	@Test
	public void testCopyConstructor() {
		PDU pdu = new PDU.Builder().bigByteEndian(true)
			.bigWordEndian(true)
			.offset(12)
			.capacity(15)
			.size(20)
			.build();
		pdu.putU16(0x0102);
		PDU copy = pdu.duplicate();

		assertThat(pdu.buffer != copy.buffer);
		assertThat(pdu.buffer).isEqualTo(copy.buffer);
		assertThat(pdu.offset).isEqualTo(copy.offset);
		assertThat(pdu.limit).isEqualTo(copy.limit);
		assertThat(pdu.capacity).isEqualTo(copy.capacity);

		copy.seal();
		assertThat(copy.getU16()).isEqualTo(0x0102);
	}

	@Test
	public void testBits() {
		PDU pdu = new PDU(300);
		Bits b = pdu.putBits(30); // 4 bytes

		assertThat(b.bits()).isEqualTo(30);
		assertThat(b.byteWidth()).isEqualTo(4);

		b.put(false)
			.put(true)
			.put(false)
			.put(true);

		b.put(16, true);

		b.flip();
		assertThat(b.get()).isFalse();
		assertThat(b.get()).isTrue();
		assertThat(b.get()).isFalse();
		assertThat(b.get()).isTrue();

		assertThat(b.get(16)).isTrue();
	}

	@Test
	public void testBitsAgain() {
		PDU pdu = new PDU(300);
		Bits b = pdu.putBits(0, 30); // 4 bytes

		assertThat(b.bits()).isEqualTo(30);
		assertThat(b.byteWidth()).isEqualTo(4);

		b.put(false)
			.put(true)
			.put(false)
			.put(true);

		b.put(16, true);

		b.flip();
		assertThat(b.get()).isFalse();
		assertThat(b.get()).isTrue();
		assertThat(b.get()).isFalse();
		assertThat(b.get()).isTrue();

		assertThat(b.get(16)).isTrue();
	}

	@Test
	public void testSealing() {
		PDU pdu = PDU.build(300)
			.offset(1)
			.build();
		pdu.position(2);
		pdu.seal();
		assertThat(pdu.position()).isEqualTo(0);
		assertThat(pdu.limit()).isEqualTo(2);
	}

	@Test
	public void testDoubleSealing() {
		PDU pdu = PDU.build(300)
			.offset(1)
			.build();
		pdu.position(2);
		pdu.seal();

		assertThatThrownBy(pdu::seal).isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void testGet() {
		PDU pdu = new PDU(100);
		pdu.putU32(0xFF00FF00L);
		byte[] bs = pdu.seal()
			.get();
		assertThat(bs.length).isEqualTo(4);
		assertThat(bs[0]).isEqualTo((byte) -1);
		assertThat(bs[1]).isEqualTo((byte) 0);
		assertThat(bs[2]).isEqualTo((byte) -1);
		assertThat(bs[3]).isEqualTo((byte) 0);

		int i8 = pdu.getI8();
		assertThat(i8).isEqualTo(-1);
		bs = pdu.get();
		assertThat(bs.length).isEqualTo(3);
		assertThat(bs[0]).isEqualTo((byte) 0);
		assertThat(bs[1]).isEqualTo((byte) -1);
		assertThat(bs[2]).isEqualTo((byte) 0);

		bs = pdu.get(1, 1);
		assertThat(bs.length).isEqualTo(1);
		assertThat(bs[0]).isEqualTo((byte) 0);
	}

	@Test
	public void testCopy() {
		PDU p1 = new PDU(100);
		p1.putU32(0xFF00FF00L);

		PDU p2 = new PDU(100);
		p2.putU8(1);

		p2.copy(p1, 1, 2);
		assertThat(p2.position()).isEqualTo(3);

		byte[] bs = p2.seal()
			.get();
		assertThat(bs.length).isEqualTo(3);
		assertThat(bs[0]).isEqualTo((byte) 1);
		assertThat(bs[1]).isEqualTo((byte) 0);
		assertThat(bs[2]).isEqualTo((byte) -1);

		PDU pdu = new PDU(100);
		pdu.copy(p1.seal());
		// assertThat(pdu.get()).isEqualTo(p1.get());

	}

	@Test
	public void testSlice() {
		PDU p1 = new PDU.Builder().bigByteEndian(false)
			.bigWordEndian(false)
			.size(100)
			.build();
		p1.putU32(0x01020304);
		byte[] bs = p1.seal()
			.get();

		assertThat(bs.length).isEqualTo(4);
		assertThat(bs[0]).isEqualTo((byte) 4);
		assertThat(bs[1]).isEqualTo((byte) 3);
		assertThat(bs[2]).isEqualTo((byte) 2);
		assertThat(bs[3]).isEqualTo((byte) 1);

		p1.getU16();
		PDU p2 = p1.slice();
		assertThat(p2.bigByteEndian).isEqualTo(false);
		assertThat(p2.bigWordEndian).isEqualTo(false);

		assertThat(p2.position()).isEqualTo(0);
		assertThat(p2.limit()).isEqualTo(2);
		assertThat(p2.getU16()).isEqualTo(0x0102);
	}

}
