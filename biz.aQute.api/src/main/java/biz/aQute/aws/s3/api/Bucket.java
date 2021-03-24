package biz.aQute.aws.s3.api;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Date;

/**
 * A Bucket is a set of Objects. An Object is a blob with some meta data,
 * including a path.
 *
 */
public interface Bucket {

	/**
	 * Get the name of the bucket.
	 *
	 * @return the name of the bucket
	 */
	String getName();

	/**
	 * Create a PUT request. The PUT request can be used to store content in the
	 * object.
	 *
	 * @param key
	 *            the path of the object
	 * @return a request
	 */
	PutRequest putObject(String key);

	/**
	 * Create a GET request. The GET request can be used to get content from the
	 * object.
	 *
	 * @param key
	 *            the path of the object
	 * @return a request
	 */
	GetRequest getObject(String key);

	/**
	 * Delete an Object
	 *
	 * @param key
	 *            the path of the object
	 */
	void delete(String key) throws Exception;

	/**
	 * Create a LIST request
	 *
	 * @return a List Request
	 */
	ListRequest listObjects() throws Exception;

	public class Content {
		public Bucket		bucket;
		public String		key;
		public Date			lastModified;
		public long			size;
		public StorageClass	storageClass;
		public String		etag;
	}

	public class Range {
		final public long	start;
		final public long	length;

		public Range(long start, long length) {
			assert length > 0;
			this.start = start;
			this.length = length;
		}

		public Range(long start) {
			this.start = start;
			this.length = -1;
		}
	}

	/**
	 * The Common part of all requests *
	 *
	 * @param <T>
	 *            the actual request type
	 */
	public interface CommonRequest<T> {

		/**
		 * Specify the content type
		 *
		 * @param contentType
		 *            th
		 */
		T contentType(String contentType);

		/**
		 * ??
		 */
		T date(Date date);

		/**
		 * Specify the MD5 checksum that is the ETag on Amazon
		 *
		 * @param etag
		 *            the etag
		 */
		T contentMD5(String etag);

		/**
		 * Specify a header
		 *
		 * @param name
		 *            the name of the argument
		 * @param value
		 *            of the argument
		 */
		T header(String name, String value);

		/**
		 * Specify an argument
		 *
		 * @param name
		 *            the name of the argument
		 * @param value
		 *            of the argument
		 */
		T argument(String name, String value);
	}

	/**
	 * A request builder for a GET request.
	 */
	public interface GetRequest extends CommonRequest<GetRequest> {
		/**
		 * Specify a range to read
		 *
		 * @param range
		 *            the range to read
		 */
		GetRequest range(Range range);

		/**
		 * Specify a set of ranges to read
		 *
		 * @param ranges
		 *            the ranges to read
		 */
		GetRequest ranges(Collection<Range> range);

		/**
		 * The IF-MODIFIED header. Specifies that if the target object is
		 * modified after the date it should be read.
		 *
		 * @param date
		 *            the threshold date
		 */
		GetRequest ifModfiedSince(Date date);

		/**
		 * The IF-UNMODIFIED header. Specifies that if the target object is
		 * not modified after the date it should be read.
		 *
		 * @param date
		 *            the threshold date
		 */
		GetRequest ifUnmodfiedSince(Date date);

		/**
		 * The IF-MATCH header. Specifies that if the target object
		 * has the same Etag
		 *
		 * @param etag the ETag
		 */
		GetRequest ifMatch(String etag);

		/**
		 * ??
		 */
		GetRequest ifNoneMatch(String etag);

		/**
		 * Set the content type
		 */
		GetRequest contentType(String string);

		/**
		 * Open the connection and get the bytes
		 */
		InputStream get() throws Exception;

		/**
		 * Create a signed URI that will expire after expires ms. The verb specifies GET, PUT, etc.
		 * Notice that the URI is signed with the headers. If you use the URI,
		 * ensure that you set those headers.
		 *
		 * @param expires validity period of the URI in milliseconds
		 * @param verb the request HTTP verb
		 */
		URI signedUri(long expires, String verb) throws Exception;

	}

	public interface PutRequest extends CommonRequest<PutRequest> {
		/**
		 * Specify the encoding of the contents.
		 * @param s encoding
		 */
		PutRequest contentEncoding(String s);

		/**
		 * ??
		 */
		PutRequest expect(String s);

		/**
		 * ??
		 */
		PutRequest expires(long ms);

		/**
		 * Specify the storage class
		 */
		PutRequest storageClass(StorageClass storageClass);

		/**
		 * Specify the content length ahead so no chunking necessary
		 */
		PutRequest contentLength(long length);

		/**
		 * Put the content
		 * @param in the content
		 */
		void put(InputStream in) throws Exception;

		/**
		 * Put a string as UTF-8 encoded data.
		 *
		 * @param in
		 *            The string to put
		 * @throws Exception
		 */
		void put(String in) throws Exception;

		URI signedUri(long expires) throws Exception;
	}

	public interface ListRequest extends CommonRequest<ListRequest>, Iterable<Content> {
		ListRequest delimiter(String delimeter);

		ListRequest marker(String marker);

		ListRequest maxKeys(int maxKeys);

		ListRequest prefix(String prefix);
	}

}
