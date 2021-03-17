package biz.aQute.aws.s3;

import java.io.*;
import java.net.*;
import java.security.*;
import java.text.*;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.w3c.dom.*;

import aQute.lib.base64.Base64;
import aQute.lib.io.*;

public class S3 {

	public enum StorageClass {
		STANDARD, REDUCED_REDUNDANCY
	}

	enum METHOD {
		GET(true, false), PUT(false, true), DELETE(false, false);
		public final boolean	doInput, doOutput;

		METHOD(boolean doInput, boolean doOutput) {
			this.doInput = doInput;
			this.doOutput = doOutput;
		}
	}

	static DocumentBuilderFactory	dbf				= DocumentBuilderFactory.newInstance();
	static XPathFactory				xpf				= XPathFactory.newInstance();
	static Mac						mac;
	static Random					r				= new Random();
	static SimpleDateFormat			httpDateFormat	= new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
	static SimpleDateFormat			awsDateFormat	= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);

	static {
		dbf.setNamespaceAware(false);
		httpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			mac = Mac.getInstance("HmacSHA1");
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
	}

	String							awsId;
	private SecretKeySpec			secret;
	boolean							debug			= false;

	public S3(String awsId, String secret) {
		this.secret = new SecretKeySpec(secret.getBytes(), "HmacSHA1");
		this.awsId = awsId;
	}

	public Bucket createBucket(String bucket, String... region) throws Exception {
		Bucket b = new Bucket(this, bucket);
		SortedMap<String,String> map = null;
		if (region != null && region.length > 0) {
			map = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
			map.put("LocationConstraint", region[0]);
		}
		construct(METHOD.PUT, b, null, null, null, null);
		return b;
	}

	public void deleteBucket(String bucket) throws Exception {
		Bucket b = new Bucket(this, bucket);
		construct(S3.METHOD.DELETE, b, null, null, null, null);
	}

	public Bucket getBucket(String name) throws Exception {
		return new Bucket(this, name);
	}

	public Collection<Bucket> listBuckets() throws Exception {
		InputStream in = construct(METHOD.GET, null, null, null, null, null);

		List<Bucket> result = new ArrayList<Bucket>();

		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(in);
		XPath xpath = xpf.newXPath();
		NodeList items = (NodeList) xpath.evaluate("/ListAllMyBucketsResult/Buckets/Bucket", doc,
				XPathConstants.NODESET);
		for (int i = 0; i < items.getLength(); i++) {
			String name = xpath.evaluate("Name", items.item(i));
			Bucket bucket = new Bucket(this, name);
			result.add(bucket);
		}
		return result;
	}

	/**
	 * <pre>
	 * StringToSign = HTTP - Verb + &quot;\n&quot; + Content - MD5 + &quot;\n&quot; + Content - Type + &quot;\n&quot; + Date + &quot;\n&quot;
	 * 		+ CanonicalizedAmzHeaders + CanonicalizedResource;
	 * </pre>
	 * 
	 * @param url
	 */

	InputStream construct(METHOD method, Bucket bucket, String id, InputStream content,
			SortedMap<String,String> headers, SortedMap<String,String> query) throws Exception {

		String etag = null;
		String type = null;
		if (headers != null) {
			type = headers.get("Content-Type");
			etag = headers.get("Content-MD5");
		}
		StringBuilder qsb = new StringBuilder();

		if (query != null && query.size() > 0) {
			String del = "?";
			for (Map.Entry<String,String> entry : query.entrySet()) {
				qsb.append(del);
				qsb.append(entry.getKey());
				qsb.append("=");
				qsb.append(encodeUrl(entry.getValue()));
				del = "&";
			}
		}

		String u;

		if (bucket == null)
			u = "http://s3.amazonaws.com";
		else {
			u = "http://" + bucket + ".s3.amazonaws.com";
		}
		if (id != null)
			u += "/" + id;
		if (query != null)
			u += "/" + qsb;

		URL url = new URL(u);
		System.out.println(u);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		if (content != null)
			conn.setDoOutput(true);

		StringBuilder sb = new StringBuilder();
		sb.append(method).append('\n');
		conn.setRequestMethod(method.toString());

		if (etag != null)
			sb.append(etag);

		sb.append('\n');
		if (type != null)
			sb.append(type);

		sb.append('\n');
		String date = httpDateFormat.format(new Date());
		sb.append(date);
		conn.setRequestProperty("Date", date);
		sb.append('\n');

		// CanonicalizedAmzHeaders
		canonicalizeHeaders(headers, conn, sb);

		if (bucket != null) {
			sb.append("/").append(bucket.getName().toLowerCase());
			conn.setRequestProperty("Host", bucket + ".s3.amazonaws.com");
		}

		String path = conn.getURL().getPath();
		if (path != null && path.length() > 0)
			sb.append(path);
		else
			sb.append("/");

		// sb.append(qsb); // query string if present

		String s = sb.toString();

		if (debug)
			System.out.println("Query to be signed: " + s + " " + u);

		String sig = "AWS " + awsId + ":" + sign(s);
		conn.setRequestProperty("Authorization", sig);

		// conn.setDoOutput(method.doOutput);
		// conn.setDoInput(method.doInput);

		conn.connect();

		if (conn.getDoOutput())
			IO.copy(content, conn.getOutputStream());

		InputStream in = null;
		if (conn.getDoInput())
			try {
				in = conn.getInputStream();
			}
			catch (IOException fnfe) {
				// ignore, handled through response code
			}

		if (conn.getResponseCode() >= 300) {
			String msg;
			if (in != null)
				msg = IO.collect(in);
			else
				msg = conn.getResponseMessage();

			if (HttpURLConnection.HTTP_NOT_FOUND == conn.getResponseCode()) {
				return null;
			}

			throw new S3Exception(msg, conn.getResponseCode());
		}
		return in;
	}

	private void canonicalizeHeaders(SortedMap<String,String> headers, HttpURLConnection conn, StringBuilder sb) {
		if (headers != null) {
			for (Map.Entry<String,String> entry : headers.entrySet()) {
				if (entry.getKey().startsWith("x-amz")) {
					sb.append(entry.getKey());
					sb.append(":");
					sb.append(entry.getValue().trim());
				} else if (entry.getKey().equalsIgnoreCase("Content-Length"))
					conn.setFixedLengthStreamingMode(Integer.parseInt(entry.getValue()));
				else
					conn.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}
	}

	synchronized String httpDate(Date date) {
		return httpDateFormat.format(date);
	}

	String encodeUrl(String value) throws Exception {
		return URLEncoder.encode(value, "utf-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~")
				.replace("%2F", "/");
	}

	synchronized Date awsDate(String evaluate) throws ParseException {
		return awsDateFormat.parse(evaluate);
	}

	/*
	 * Create a signed URI.
	 */
	public URI signedUri(String method, Bucket bucket, String key, SortedMap<String,String> headers, long expires)
			throws Exception {
		long now = System.currentTimeMillis();

		if (expires == 0)
			return new URI(String.format("https://%s.s3.amazonaws.com/%s", bucket, key));

		expires = now / 1000 + expires * 60; // in minutes

		String md5 = headers.get("Content-MD5");
		String type = headers.get("Content-Type");

		StringBuilder sb = new StringBuilder(method);
		sb.append("\n");
		if (md5 != null)
			sb.append(md5.trim().toLowerCase());
		sb.append("\n");
		if (type != null)
			sb.append(type.trim().toLowerCase());
		sb.append("\n").append(expires);
		sb.append("\n"); // amz headers
		sb.append("/").append(bucket.getName().toLowerCase()).append("/").append(key);
		// System.out.println("'" + sb + "'");

		String sig = URLEncoder.encode(sign(sb), "UTF-8");
		String url = String.format("https://%s.s3.amazonaws.com/%s?AWSAccessKeyId=%s&Signature=%s&Expires=%s", bucket,
				key, URLEncoder.encode(awsId, "UTF-8"), sig, expires);
		return new URI(url);
	}

	/*
	 * Signing method, is using a shared object.
	 */
	private synchronized String sign(CharSequence sb) throws InvalidKeyException, UnsupportedEncodingException {
		mac.init(secret);
		byte[] data = sb.toString().getBytes("UTF-8");
		byte[] signature = mac.doFinal(data);
		// System.out.println(Hex.toHexString(data).replaceAll("(..)", "$1 "));
		String s = Base64.encodeBase64(signature);
		// System.out.println(s);
		return s;
	}

	@Override
	public String toString() {
		return "S3 [" + (awsId != null ? "awsId=" + awsId + ", " : "") + "debug=" + debug + "]";
	}
}
