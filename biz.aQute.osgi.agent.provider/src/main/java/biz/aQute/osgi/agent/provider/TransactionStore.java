package biz.aQute.osgi.agent.provider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.json.JSONCodec;

/**
 * Safe storage for a configuration file. It will always store the configuration
 * file into a new file (timestamped name). It will be able to find the last one
 *
 * @param <T>
 *            The DTO type for this store
 */
public class TransactionStore<T> {
	final static JSONCodec					codec				= new JSONCodec();
	final Class<T>							type;
	final File								directory;
	final static DateTimeFormatter			FORMATTER			= DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
			.withZone(ZoneId.systemDefault());
	private static final Predicate<String>	FILENAME_PRED		= Pattern.compile("\\d{16}").asPredicate();
	private int								generations;
	private Random							random				= new Random();
	private Set<String>						blacklist			= new HashSet<>();
	private final Predicate<? super String>	isNotBlacklisted	= blacklist::contains;
	private final URL						fallbackURL;

	/**
	 * Create a transactional store that keeps a number generations of files
	 * that contain a JSON encoded DTO.
	 * 
	 * @param dir
	 * @param generations
	 * @param type
	 * @param fallbackURL
	 */
	public TransactionStore(File dir, int generations, Class<T> type, URL fallbackURL) {
		this.directory = dir;
		this.generations = generations;
		this.type = type;
		this.fallbackURL = fallbackURL;
	}

	private File[] getSorted() {
		File[] sorted = Stream.of(directory.listFiles())
				.filter(f -> FILENAME_PRED.test(f.getName()))
				.filter(f -> isNotBlacklisted.negate().test(f.getName()))
				.sorted((a, b) -> b.getName().compareTo(a.getName()))
				.toArray(File[]::new);
		return sorted;
	}

	private File getNewFile() {
		File[] sorted = getSorted();

		File f = new File(directory, FORMATTER.format(Instant.now()));
		if (sorted.length > generations) {
			kill(sorted[0]);
		}
		return f;
	}

	private void kill(File file) {

		if (file == null)
			return;

		boolean deleted = file.delete();
		if (!deleted) {
			// TODO log
			blacklist.add(file.getName());
			boolean renamed = file.renameTo(new File(Long.toString(random.nextLong())));
			if (!renamed) {
				// TODO log
			}
		}
	}

	/**
	 * Will update the object to a new file. Will attempt to retry if the write
	 * fails.
	 * 
	 * @param dto the dto to update
	 * @throws InterruptedException when interrupted while sleeping during a recover
	 */
	public void update(T dto) throws InterruptedException {
		File f = null;
		for (int i = 0; i < 3; i++)
			try {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				codec.enc().to(bout).put(dto).close();

				f = getNewFile();
				byte[] bytes = bout.toByteArray();
				Files.write(f.toPath(), bytes, StandardOpenOption.CREATE);

				byte[] readAllBytes = Files.readAllBytes(f.toPath());
				if (Arrays.equals(bytes, readAllBytes))
					return;
			} catch (Exception e) {
				Thread.sleep(1000 * (i + 1));
				kill(f);
			}
		throw new IllegalStateException("Cannot write state file for " + dto);
	}

	/**
	 * Will go out of its way to return an object. First tries to read the previous
	 * files, then reads the fallback url, then returns a new instance.
	 * 
	 * @throws InterruptedException when interrupted while sleeping during a recover
	 */
	public T read() throws InterruptedException {

		//
		// First read generations
		//

		File[] sorted = getSorted();

		for (int i = 0; i < sorted.length; i++) {
			try {
				return codec.dec().from(sorted[i]).get(type);
			} catch (Exception e) {
				kill(sorted[i]);
				Thread.sleep(1000);
			}
		}

		//
		// Then read a file in our bundle
		//

		try {
			return codec.dec().from(fallbackURL.openStream()).get(type);
		} catch (Exception e) {
			try {
				//
				// Then try to create a new instance
				//
				return type.newInstance();
			} catch (Exception e1) {
				//
				// Finally give up
				//
				e1.printStackTrace();
				throw Exceptions.duck(e1);
			}
		}
	}

}
