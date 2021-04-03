package biz.aQute.modbus.reflective;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import aQute.lib.collections.LineCollection;
import aQute.lib.collections.MultiMap;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import biz.aQute.modbus.api.PDU;

public class AccessMapper {
	public static class Access implements Comparable<Access> {

		final public Bank		bank;
		final public String		name;
		final public int		address;
		final public PDU.Entry	entry;

		public Access(Bank bank, String name, int address, PDU.DataType type, int width) {
			this.bank = bank;
			this.name = name;
			this.entry = new PDU.Entry(type, width, 0);
			this.address = address;
		}

		@Override
		public int compareTo(Access o) {
			return Integer.compare(address, o.address);
		}

		@Override
		public String toString() {
			return "Access [bank=" + bank + ", name=" + name + ", address=" + address + ", entry=" + entry + "]";
		}
	}

	final MultiMap<Bank, Access>	access	= new MultiMap<>();
	final List<String>				errors	= new ArrayList<>();
	final MultiMap<String, Access>	index	= new MultiMap<>();

	public AccessMapper(String config) throws IOException {
		this(IO.stream(config));
	}

	public AccessMapper(InputStream in) throws IOException {

		try (LineCollection lc = new LineCollection(in)) {
			lc.forEachRemaining(l -> {
				Access a = line(l);
				if (a != null) {
					access.add(a.bank, a);
				}
			});
		}

		access.values()
			.forEach(Collections::sort);
		access.entrySet()
			.forEach(e -> {
				int lowest = Integer.MAX_VALUE;
				int highest = Integer.MIN_VALUE;
				int rover = 0;

				Access previous = null;
				for (Access a : e.getValue()) {

					lowest = Math.min(lowest, a.address);
					highest = Math.max(highest, a.address + a.entry.width);

					if (rover > a.address) {
						error("overlapping addresses %s and %s", previous, a);
					}

					rover = a.address + a.entry.width;
					previous = a;
				}

			});

		access.values()
			.stream()
			.flatMap(Collection::stream)
			.forEach(a -> index.add(a.name, a));
	}

	private Access line(String l) {
		if (l == null)
			return null;

		String trimmed = Strings.trim(l);
		if (trimmed.startsWith("#") || trimmed.isEmpty())
			return null;

		String[] parts = trimmed.split("\\s*:\\s*");

		// 0 Bank
		Bank bank;
		try {
			bank = Bank.valueOf(parts[0]);
		} catch (IllegalArgumentException e0) {
			error("invalid bank in %s, must be one of %s", trimmed, Bank.values());
			return null;
		}

		if (parts.length < 3) {
			error("invalid key, must have at least 3 parts separated by ':' : %s", trimmed);
			return null;
		}

		// 1 Name
		String name = parts[1];

		// 2 Address
		int address;
		try {
			address = Integer.parseInt(parts[2]);
		} catch (NumberFormatException e0) {
			error("invalid address in  %s", trimmed);
			return null;
		}

		if (address < 0) {
			error("negative address in  %s", trimmed);
			return null;
		}

		// 3 Type
		PDU.DataType registerType;
		int width;

		if (parts.length > 3) {
			if (bank.word) {
				try {
					registerType = PDU.DataType.valueOf(parts[3]);
				} catch (IllegalArgumentException e0) {
					error("invalid address for %s: %s", trimmed);
					return null;
				}
				if (registerType == PDU.DataType.bit) {
					error("bit is not a valid type for bank %s", bank);
					return null;
				}

				if (parts.length > 4) {

					try {
						// x2 for word registers
						width = Integer.parseInt(parts[4]) * 2;
					} catch (NumberFormatException e0) {
						error("width is not an integer  %s", trimmed);
						return null;
					}
					if (width < 1) {
						error("width is less than 1  %s", trimmed);
						return null;
					}

					if (registerType != PDU.DataType.String) {
						if (width % registerType.width != 0) {
							error("Width %s for an array specified but not multiple of type %s width %s", width,
								registerType, registerType.width);
							return null;
						}
					}

				} else {
					if (registerType == PDU.DataType.String) {
						error("Width not specified for a String type %s", trimmed);
						return null;
					}
					width = registerType.width;
				}
			} else {
				error("superfluous arguments for bit bank %s", bank);
				return null;
			}
		} else {
			registerType = PDU.DataType.bit;
			width = 1;
		}

		return new Access(bank, name, address, registerType, width);
	}

	void error(String format, Object... args) {
		errors.add(String.format(format, args));
	}

	public Optional<Access> findAccess(Bank bank, int address) {
		List<Access> list = access.get(bank);
		for (Access access : list) {
			if (access.address == address)
				return Optional.of(access);

			if (access.address > address)
				break;
		}
		return Optional.empty();
	}

	public Access get(String name) {
		return null;
	}

}
