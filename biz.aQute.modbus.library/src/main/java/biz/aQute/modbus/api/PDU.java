package biz.aQute.modbus.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;

public class PDU {

	public static class Entry {

		public final DataType	type;
		public final int		width;
		public final int		bitpos;

		public Entry(DataType type, int width, int bitpos) {
			this.type = type;
			this.width = width == 0 ? type.width : width;
			this.bitpos = bitpos;
		}

		public boolean isArray() {
			return (type != DataType.String && type != DataType.bit) && type.width != width;
		}

		@Override
		public String toString() {
			return "Entry [type=" + type + ", width=" + width + ", bitpos=" + bitpos + "]";
		}

	}

	public enum DataType {
		bit(0, null), //
		u8(1, int.class), //
		i8(1, byte.class), //
		u16(2, int.class), //
		i16(2, short.class), //
		u32(4, long.class), //
		i32(4, int.class), //
		i64(8, long.class), //
		Double(8, double.class), //
		Float(4, float.class), //
		String(0, null), //
		;

		public final int		width;
		public final Class<?>	type;

		DataType(int width, Class<?> type) {
			this.width = width;
			this.type = type;
		}
	}

	public static class Builder {
		int		size			= 256;
		byte[]	buffer;
		boolean	bigByteEndian	= true;
		boolean	bigWordEndian	= true;
		int		offset;
		int		capacity		= -1;

		public Builder size(int size) {
			this.size = size;
			return this;
		}

		public Builder bigByteEndian(boolean b) {
			this.bigByteEndian = b;
			return this;
		}

		public Builder bigWordEndian(boolean b) {
			this.bigWordEndian = b;
			return this;
		}

		public Builder buffer(byte[] buffer) {
			this.buffer = buffer;
			return this;
		}

		public Builder capacity(int capacity) {
			this.capacity = capacity;
			return this;
		}

		public Builder offset(int offset) {
			this.offset = offset;
			return this;
		}

		public PDU build() {
			if (buffer == null) {
				if (size < 0)
					throw new IllegalArgumentException("size less than zero " + size);

				buffer = new byte[size];
			}
			if (capacity < 0)
				capacity = buffer.length;

			if (capacity > buffer.length)
				throw new IllegalArgumentException(
					"limit larger than buffer length. b.length=" + buffer.length + " capacity=" + capacity);

			if (offset > capacity)
				throw new IllegalArgumentException(
					"offset larger than capacity capacity=" + capacity + " offset=" + offset);

			return new PDU(buffer, offset, capacity, bigByteEndian, bigWordEndian);
		}
	}

	/**
	 * Marks a position for a length field and then will fill in the length
	 * field when this block is closed.
	 */
	public interface BlockLength {
		/**
		 * Close the
		 */
		int close();
	}

	/**
	 * Reserves a number of bytes and interprets them as bitfields.
	 */
	public interface Bits {
		int bits();

		int byteWidth();

		Bits put(boolean v);

		Bits put(int bit, boolean v);

		Bits put(int width, long value);

		Bits put(int offset, int width, long value);

		default Bits set() {
			return put(true);
		}

		default Bits reset() {
			return put(false);
		}

		boolean get();

		boolean get(int bit);

		Bits flip();
	}

	private static final int	MAX_SIZE	= 1_100_000_000;

	final boolean				bigByteEndian;
	final boolean				bigWordEndian;
	final byte[]				buffer;
	final int					offset;
	final int					capacity;
	int							limit;
	int							absPosition;

	private boolean				sealed;

	public PDU(int size) {
		this(new byte[size], 0, size, true, true);
	}

	public PDU(byte[] buffer, int offset, int capacity, boolean bigByteEndian, boolean bigWordEndion) {
		this.bigByteEndian = bigByteEndian;
		this.bigWordEndian = bigWordEndion;
		this.buffer = buffer;
		this.offset = offset;
		this.capacity = capacity;
		this.limit = capacity;
		this.absPosition = offset;
	}

	public PDU(PDU pdu) {
		this(pdu.buffer.clone(), pdu.offset, pdu.capacity, pdu.bigByteEndian, pdu.bigWordEndian);
		this.absPosition = pdu.absPosition;
		this.limit = pdu.limit;
		invariant();
	}

	public int getU8() {
		int v = getU8(position());
		next(1);
		return v;
	}

	public int getU8(int relPosition) {
		int effective = check(relPosition, 1);
		return 0xFF & buffer[effective];
	}

	public PDU putU8(int v) {
		putU8(position(), v);
		next(1);
		return this;
	}

	public PDU putU8(int relPosition, int v) {
		if (v < 0 || v > 0xFF) {
			throw new IllegalArgumentException("U8 >=0 && < 256 : " + v);
		}
		int absPosition = check(relPosition, 1);
		buffer[absPosition] = (byte) v;
		return this;
	}

	// I8

	public int getI8() {
		int v = getI8(position());
		next(1);
		return v;
	}

	public int getI8(int relPosition) {
		int absPosition = check(relPosition, 1);
		return buffer[absPosition];
	}

	public PDU putI8(int v) {
		putI8(position(), v);
		next(1);
		return this;
	}

	public PDU putI8(int relPosition, int v) {
		if (v < Byte.MIN_VALUE || v > Byte.MAX_VALUE) {
			throw new IllegalArgumentException("I8 >=" + Byte.MIN_VALUE + " && < " + Byte.MAX_VALUE + " : " + v);
		}

		int absPosition = check(relPosition, 1);
		buffer[absPosition] = (byte) v;
		return this;
	}

	// U16

	public int getU16() {
		int v = getU16(position());
		next(2);
		return v;
	}

	public int getU16(int relPosition) {
		int low, high;
		if (bigByteEndian) {
			low = getU8(relPosition) << 8;
			high = getU8(relPosition + 1);
		} else {
			low = getU8(relPosition);
			high = getU8(relPosition + 1) << 8;
		}
		return high | low;
	}

	public PDU putU16(int v) {
		putU16(position(), v);
		next(2);
		return this;
	}

	public PDU putU16(int relPosition, int v) {
		if (v < 0 || v > 0xFFFF) {
			throw new IllegalArgumentException("U8 >=0 && < 0x1_0000 : " + v);
		}

		if (bigByteEndian) {
			putU8(relPosition, 0xFF & (v >> 8));
			putU8(relPosition + 1, 0xFF & v);
		} else {
			putU8(relPosition, 0xFF & v);
			putU8(relPosition + 1, 0xFF & (v >> 8));
		}
		return this;
	}

	// I16

	public int getI16() {
		int v = getI16(position());
		next(2);
		return v;
	}

	public int getI16(int relPosition) {
		int a, b;
		if (bigByteEndian) {
			a = getI8(relPosition) * 0x100;
			b = getU8(relPosition + 1);
		} else {
			a = getU8(relPosition);
			b = getI8(relPosition + 1) * 0x100;
		}
		return a | b;
	}

	public PDU putI16(int v) {
		putI16(position(), v);
		next(2);
		return this;
	}

	public PDU putI16(int relPosition, int v) {
		assert v >= Short.MIN_VALUE && v <= Short.MAX_VALUE;

		if (bigByteEndian) {
			putU8(relPosition, 0xFF & (v >> 8));
			putU8(relPosition + 1, 0xFF & v);
		} else {
			putU8(relPosition, 0xFF & v);
			putU8(relPosition + 1, 0xFF & (v >> 8));
		}
		return this;
	}

	// U32

	public long getU32() {
		long v = getU32(position());
		next(4);
		return v;
	}

	public long getU32(int relPosition) {
		long high, low;
		if (bigWordEndian) {
			low = ((long) getU16(relPosition)) << 16;
			high = getU16(relPosition + 2);
		} else {
			low = getU16(relPosition);
			high = ((long) getU16(relPosition + 2)) << 16;
		}
		return high | low;
	}

	public PDU putU32(long v) {
		putU32(position(), v);
		next(4);
		return this;
	}

	public PDU putU32(int relPosition, long v) {
		if (v < 0 || v > 0xFFFF_FFFFL)
			throw new IllegalArgumentException("U32 > 0 & < 0x1_0000_0000 : " + v);

		if (bigWordEndian) {
			putU16(relPosition, (int) (0xFFFF & (v >> 16)));
			putU16(relPosition + 2, (int) (0xFFFF & (v >> 0)));
		} else {
			putU16(relPosition, (int) (0xFFFF & (v >> 0)));
			putU16(relPosition + 2, (int) (0xFFFF & (v >> 16)));
		}
		return this;
	}

	// I32

	public int getI32() {
		int v = getI32(position());
		next(4);
		return v;
	}

	public int getI32(int relPosition) {
		check(relPosition, 4);
		int low, high;
		if (bigWordEndian) {
			high = getU16(relPosition) << 16;
			low = getU16(relPosition + 2);
		} else {
			high = getU16(relPosition);
			low = getU16(relPosition + 2) << 16;
		}
		return low | high;
	}

	public PDU putI32(int v) {
		putI32(position(), v);
		next(4);
		return this;
	}

	public PDU putI32(int relPosition, int v) {
		int high = 0xFFFF & (v >> 16), low = (0xFFFF & v);

		if (bigWordEndian) {
			putU16(relPosition, high);
			putU16(relPosition + 2, low);
		} else {
			putU16(relPosition, low);
			putU16(relPosition + 2, high);
		}
		return this;
	}

	// I64

	public long getI64() {
		long v = getI64(position());
		next(8);
		return v;
	}

	public long getI64(int relPosition) {
		check(relPosition, 8);
		long low, high;
		if (bigWordEndian) {
			low = getU32(relPosition) << 32;
			high = getU32(relPosition + 4);
		} else {
			low = getU32(relPosition);
			high = getU32(relPosition + 4) << 32;
		}
		return low | high;
	}

	public PDU putI64(long v) {
		putI64(position(), v);
		next(8);
		return this;
	}

	public PDU putI64(int relPosition, long v) {
		if (bigWordEndian) {
			putU16(relPosition, (int) (0xFFFF & (v >> 48)));
			relPosition += 2;
			putU16(relPosition, (int) (0xFFFF & (v >> 32)));
			relPosition += 2;
			putU16(relPosition, (int) (0xFFFF & (v >> 16)));
			relPosition += 2;
			putU16(relPosition, (int) (0xFFFF & (v >> 0)));

		} else {
			putU16(relPosition, (int) (0xFFFF & (v >> 0)));
			relPosition += 2;
			putU16(relPosition, (int) (0xFFFF & (v >> 16)));
			relPosition += 2;
			putU16(relPosition, (int) (0xFFFF & (v >> 32)));
			relPosition += 2;
			putU16(relPosition, (int) (0xFFFF & (v >> 48)));
		}
		return this;
	}

	// Double

	public double getDouble() {
		double v = getDouble(position());
		next(8);
		return v;
	}

	public double getDouble(int relPosition) {
		long bits = getI64(relPosition);
		return Double.longBitsToDouble(bits);
	}

	public PDU putDouble(double value) {
		long doubleToLongBits = Double.doubleToLongBits(value);
		putI64(doubleToLongBits);
		return this;
	}

	public PDU putDouble(int relPosition, double v) {
		long doubleToLongBits = Double.doubleToLongBits(v);
		putI64(relPosition, doubleToLongBits);
		return this;
	}

	// Float

	public float getFloat() {
		float v = getFloat(position());
		next(4);
		return v;
	}

	public float getFloat(int relPosition) {
		int bits = getI32(relPosition);
		return Float.intBitsToFloat(bits);
	}

	public PDU putFloat(float v) {
		putFloat(position(), v);
		next(4);
		return this;
	}

	public PDU putFloat(int relPosition, float value) {
		int floatToIntBits = Float.floatToIntBits(value);
		putI32(relPosition, floatToIntBits);
		return this;
	}

	// String

	public String getString(int maxbytes) {
		String s = getString(position(), maxbytes, StandardCharsets.UTF_8);
		next(maxbytes);
		return s;
	}

	public String getString(int relPosition, int maxbytes) {
		return getString(relPosition, maxbytes, StandardCharsets.UTF_8);
	}

	public String getString(int relPosition, int maxbytes, Charset charSet) {
		check(relPosition, maxbytes);

		int i = 0;
		for (; i < maxbytes; i++) {
			if (getU8(relPosition + i) == 0)
				break;
		}
		return new String(buffer, offset + relPosition, i, charSet);
	}

	public PDU putString(String str, int width) {
		return putString(str, width, StandardCharsets.UTF_8);
	}

	public PDU putString(String str, int width, Charset charSet) {
		putString(position(), str, width, charSet);
		next(width);
		return this;
	}

	public PDU putString(int relPosition, String str, int width) {
		return putString(position(), str, width, StandardCharsets.UTF_8);
	}

	public PDU putString(int relPosition, String str, int width, Charset charSet) {
		check(relPosition, width);

		byte[] bytes = str.getBytes(charSet);
		for (int i = 0; i < width; i++) {
			if (i < bytes.length) {
				putU8(relPosition + i, bytes[i]);
			} else {
				putU8(relPosition + i, 0);
			}
		}
		return this;

	}

	private int check(int relPosition, int length) {

		if (relPosition < 0)
			throw new IllegalArgumentException("position >= 0 " + relPosition);

		if (relPosition > MAX_SIZE)
			throw new IllegalArgumentException("buffers larger than 1Gb not supported " + relPosition);

		if (length < 0)
			throw new IllegalArgumentException("length must be >= 0 " + length);

		if (relPosition > MAX_SIZE)
			throw new IllegalArgumentException("buffers larger than 1Gb not supported " + relPosition);

		if (length + relPosition > MAX_SIZE)
			throw new IllegalArgumentException("relPosition+length must be < 1Gb " + (length + relPosition));

		int absPosition = offset + relPosition;

		if (absPosition + length > limit)
			throw new IllegalArgumentException(error("[absPosition=%s, length=%s) must be within [offset=%s,limit=%s)",
				absPosition, length, offset, limit));

		invariant();
		return absPosition;
	}

	String error(String format, Object... args) {
		return String.format(format, args) + "\n" + this;
	}

	public PDU putObject(Object o) {
		int size = putObject0(position(), o);
		next(size);
		return this;
	}

	public PDU putObject(int relPosition, Object o) {
		putObject0(relPosition, o);
		return this;
	}

	private int putObject0(int relPosition, Object o) {
		if (o instanceof Short) {
			short v = (short) o;
			putI16(relPosition, v);
			return 2;
		} else if (o instanceof Integer) {
			int v = (int) o;
			putI32(relPosition, v);
			return 4;
		} else if (o instanceof Long) {
			long v = (int) o;
			putI64(relPosition, v);
			return 8;
		} else if (o instanceof Float) {
			float f = (float) o;
			putFloat(relPosition, f);
			return 4;
		} else if (o instanceof Double) {
			Double d = (Double) o;
			putDouble(relPosition, d);
			return 4;
		} else if (o instanceof Number) // I64
		{
			Number b = (Number) o;
			long longValue = b.longValue();
			putI64(relPosition, longValue);
			return 8;
		} else if (o instanceof Inet4Address) {
			Inet4Address i4a = (Inet4Address) o;
			byte[] address = i4a.getAddress();
			put(relPosition, address);
			return 4;
		} else if (o instanceof Inet6Address) {
			Inet6Address i6a = (Inet6Address) o;
			byte[] address = i6a.getAddress();
			put(relPosition, address);
			return 4;
		} else
			throw new IllegalArgumentException("Unnown type " + o);
	}

	public void put(byte[] data) {
		put(data, 0, data.length);
	}

	public void put(byte[] data, int i, int length) {
		put(position(), data, 0, data.length);
		next(length);

	}

	public void put(int relPosition, byte[] data) {
		put(relPosition, data, 0, data.length);
	}

	private void put(int position, byte[] data, int i, int length) {
		int absPosition = check(position, length);
		System.arraycopy(data, i, buffer, absPosition, length);
	}

	public int position() {
		return absPosition - offset;
	}

	public PDU seal() {
		if (sealed)
			throw new IllegalStateException("PDU is already sealed");
		this.limit = this.absPosition;
		this.absPosition = offset;
		this.sealed = true;
		return this;
	}

	public PDU position(int relPosition) {
		check(relPosition, 0);
		this.absPosition = relPosition + offset;
		return this;
	}

	public PDU limit(int relLimit) {
		if (relLimit < 0)
			throw new IllegalArgumentException(error("limit is negative %s", relLimit));

		if (relLimit > MAX_SIZE)
			throw new IllegalArgumentException(error("limit %s is larger than 1Gb", relLimit));

		int absLimit = relLimit + offset;

		if (absLimit > capacity)
			throw new IllegalArgumentException(error("abs limit %s is larger than capacity %s", absLimit, capacity));

		this.limit = absLimit;
		return this;
	}

	public int limit() {
		return limit - offset;
	}

	public void reset() {
		this.absPosition = offset;
		this.limit = capacity;
		this.sealed = false;
	}

	public void write(OutputStream outputStream) throws IOException {
		outputStream.flush();
		outputStream.write(this.buffer, absPosition, limit - absPosition);
		outputStream.flush();
	}

	public PDU read(InputStream in, int available) throws IOException {
		if (available <= 0)
			return this;

		read(position(), in, available);
		next(available);
		return this;
	}

	public PDU read(int relPosition, InputStream in, int available) throws IOException {
		int absPosition = check(relPosition, available);
		in.read(buffer, absPosition, available);
		return this;
	}

	public PDU duplicate() {
		return new PDU(this);
	}

	public BlockLength putBlockLengthU8() {
		check(position(), 1);
		int place = position();
		next(1);

		return () -> {
			int l = position() - place - 1;
			if (l > 0xFF)
				throw new IllegalArgumentException(error("block length exceeded. Max %s, is %s", 0xFF, l));

			putU8(place, l);
			return l;
		};
	}

	public BlockLength putBlockLengthU16() {
		check(position(), 2);
		int place = position();
		next(2);

		return () -> {
			int l = position() - place - 2;
			if (l > 0xFFFF)
				throw new IllegalArgumentException(error("block length exceeded. Max %s, is %s", 0xFFFF, l));
			putU16(place, l);
			return l;
		};
	}

	public Bits putBits(int bits) {
		Bits b = putBits(position(), bits);
		next(b.byteWidth());
		return b;
	}

	public Bits putBits(int relPosition, int bits) {
		int size = (bits + 7) / 8;
		int absPosition = check(relPosition, size);
		next(size);

		return new Bits() {
			int bitRover = 0;

			@Override
			public int bits() {
				return bits;
			}

			@Override
			public int byteWidth() {
				return size;
			}

			@Override
			public Bits put(boolean v) {
				this.put(bitRover++, v);
				return this;
			}

			@Override
			public Bits put(int bit, boolean v) {

				if (bit >= bits)
					throw new IllegalArgumentException("Bit outside range. is " + bit + " max " + bits);

				int bytePos = absPosition + (bit / 8);
				int bitPos = bit % 8;

				int mask = getU8(bytePos);
				int rover = 1 << bitPos;

				if (v) {
					mask |= rover;
				} else {
					mask &= ~rover;
				}
				putU8(bytePos, mask);
				return this;
			}

			@Override
			public boolean get() {
				return get(bitRover++);
			}

			@Override
			public boolean get(int bit) {
				if (bit >= bits)
					throw new IllegalArgumentException("Bit outside range. is " + bit + " max " + bits);

				int mask = getU8(absPosition + bit / 8);
				if ((mask & 1 << (bit % 8)) != 0) {
					return true;
				} else {
					return false;
				}
			}

			@Override
			public Bits flip() {
				bitRover = 0;
				return this;
			}

			@Override
			public Bits put(int width, long value) {
				put(bits, width, value);
				bitRover += width;
				return this;
			}

			@Override
			public Bits put(int bit, int width, long value) {

				if (width >= 64 || width < 0)
					throw new IllegalArgumentException("a bit field width must be between 0 & 64 " + width);

				long mask = 1 << width;
				for (int i = 0; i < width; i++) {
					put(bit + i, (mask & value) != 0);
					mask >>= 1;
				}
				return this;
			}

		};
	}

	public Object get(Entry entry) {
		Object o = get(position(), entry);
		next(entry.width);
		return o;
	}

	public Object get(int position, Entry entry) {

		if (entry.type == DataType.String) {
			return getString(position, entry.width);
		}

		int n = entry.width / entry.type.width;
		if (n == 1) {
			return getByType(position, entry);
		} else {
			Object array = new Object[n];
			for (int i = 0; i < n; i++) {
				Object v = getByType(position + i * entry.type.width, entry);
				Array.set(array, i, v);
			}
			return array;
		}
	}

	private Object getByType(int position, Entry entry) {
		switch (entry.type) {
			case Double :
				return getDouble(position);

			case Float :
				return getFloat(position);

			case i16 :
				return getI16(position);
			case i32 :
				return getI32(position);
			case i64 :
				return getI64(position);
			case i8 :
				return getI8(position);
			case u16 :
				return getU16(position);
			case u32 :
				return getU32(position);
			case u8 :
				return getU8(position);

			default :
			case String :
			case bit :
				throw new UnsupportedOperationException();
		}
	}

	public PDU put(Entry entry, Object o) {
		put(position(), entry, o);
		next(entry.width);
		return this;
	}

	public PDU put(int position, Entry entry, Object o) {

		if (entry.isArray()) {
			try {
				Object[] array = Converter.cnv(Object[].class, o);
				int n = entry.width / entry.type.width;
				for (int i = 0; i < n; i++) {
					int p = position + i * entry.type.width;
					if (array.length > i) {
						Object v = array[i];
						putSimple(p, entry, v);
					} else {
						putSimple(p, entry, 0);
					}
				}
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		} else {
			putSimple(position, entry, o);
		}
		return this;
	}

	PDU putSimple(int position, Entry entry, Object o) {
		switch (entry.type) {
			case Double :
				return putDouble(position, ((Number) o).doubleValue());

			case Float :
				return putFloat(position, ((Number) o).floatValue());

			case String :
				return putString(position, o.toString(), entry.width);
			case bit :
				throw new UnsupportedOperationException();
			case i16 :
				return putI16(position, toInt(o));
			case i32 :
				return putI32(position, toInt(o));
			case i64 :
				return putI64(position, toLong(o));
			case i8 :
				return putI8(position, toInt(o));
			case u16 :
				return putU16(position, toInt(o));
			case u32 :
				return putU32(position, toLong(o));
			case u8 :
				return putU8(position, toInt(o));
		}
		return null;
	}

	private long toLong(Object o) {
		if (o instanceof Number)
			return ((Number) o).longValue();
		return toInt(o);
	}

	private int toInt(Object o) {
		if (o instanceof Boolean)
			return ((Boolean) o) ? 1 : 0;

		if (o instanceof Number)
			return ((Number) o).intValue();

		try {
			return Converter.cnv(Integer.class, o);
		} catch (Exception e) {}
		return 0;
	}

	@Override
	public String toString() {
		try (Formatter f = new Formatter()) {
			int n = offset;
			String del = "";
			while (n < limit) {
				if ((n % 8) == 0) {
					f.format("%s%04x", del, n);
					del = "\n";
				}
				f.format(" %02X", buffer[n]);
				n++;
			}
			f.format("\n");
			return f.toString();
		}

	}

	private void invariant() {
		assert capacity >= 0 && capacity <= buffer.length;
		assert limit >= offset && limit <= capacity;
		assert offset >= 0 && offset <= limit;
		assert absPosition >= offset && absPosition <= limit;

	}

	private void next(int i) {
		absPosition += i;
	}

	public int remaining() {
		return limit - absPosition;
	}

	public static Builder build(int size) {
		return new Builder().size(size);
	}

}
