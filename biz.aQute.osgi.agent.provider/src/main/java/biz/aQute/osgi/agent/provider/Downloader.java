package biz.aQute.osgi.agent.provider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Executor;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

/**
 * Simple type safe downloader. It will download JSON or it provides your with
 * an InputStream, byte[], or a DTO
 *
 */
public class Downloader {
	final static JSONCodec	codec	= new JSONCodec();
	final Executor			executor;

	public Downloader(Executor executor) {
		this.executor = executor;
	}

	/**
	 * Download a URI into a type. We support the following types:
	 * <ul>
	 * <li>InputStream
	 * <li>byte[]
	 * <li>a DTO – Converted from JSON
	 * </ul>
	 *
	 * @param path
	 *            The URI to download
	 * @param type
	 *            the type to download
	 * @return an instance of the given type
	 */
	public <T> Promise<T> download(URI path, Class<T> type) {

		if (type == InputStream.class) {
			return download(path).map(bytes -> type.cast(new ByteArrayInputStream(bytes)));
		}

		if (type == byte[].class) {
			return download(path).map(bytes -> type.cast(bytes));
		}

		return download(path)
				.map(in -> {
					try {
						return codec.dec().from(in).get(type);
					} catch (Exception e) {
						throw Exceptions.duck(e);
					}
				});
	}

	private Promise<byte[]> download(URI path) {
		Deferred<byte[]> deferred = new Deferred<>();
		executor.execute(() -> {
			try {
				URL url = path.toURL();
				URLConnection con = url.openConnection();
				if (con instanceof HttpURLConnection) {
					HttpURLConnection http = (HttpURLConnection) con;
					http.setConnectTimeout(5000);
					http.setReadTimeout(10000);
					http.setInstanceFollowRedirects(true);
				}
				InputStream in = con.getInputStream();
				byte[] read = IO.read(in);
				deferred.resolve(read);

			} catch (Exception e) {
				deferred.fail(e);
			}
		});
		return deferred.getPromise();
	}

}
