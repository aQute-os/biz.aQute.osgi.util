package biz.aQute.aws.s3;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

import aQute.lib.base64.Base64;
import aQute.lib.io.IO;
import aQute.libg.cryptography.Digester;
import aQute.libg.cryptography.MD5;
import biz.aQute.aws.s3.api.Bucket.PutRequest;
import biz.aQute.aws.s3.api.StorageClass;

class PutRequestImpl extends CommonRequestImpl<PutRequest> implements PutRequest {
	final BucketImpl	bucket;
	final String	key;
	long			length	= -1;

	PutRequestImpl(S3Impl parent, BucketImpl Bucket, String key) {
		super(parent);
		this.bucket = Bucket;
		this.key = key;
	}

	@Override
	public PutRequestImpl contentEncoding(String s) {
		header("Content-Encoding", s);
		return this;
	}

	@Override
	public PutRequestImpl expect(String s) {
		header("Expect", s);
		return this;
	}

	@Override
	public PutRequestImpl expires(long ms) {
		header("Expires", Long.toString(ms));
		return this;
	}

	@Override
	public PutRequestImpl storageClass(StorageClass storageClass) {
		header("x-amz-storage-class", storageClass.toString());
		return this;
	}

	@Override
	public void put(InputStream in) throws Exception {
		if (length == -1) {
			File tmpfile = File.createTempFile("awss3", ".tmp");
			FileOutputStream out = new FileOutputStream(tmpfile);
			Digester<MD5> md5 = MD5.getDigester(out);
			IO.copy(in, md5);
			if (!headers.containsKey("Content-MD5"))
				headers.put("Content-MD5", Base64.encodeBase64(md5.digest().digest()));
			headers.put("Content-Length", Long.toString(tmpfile.length()));
			in = new FileInputStream(tmpfile);
		}
		// System.out.println(headers);
		parent.construct(S3Impl.METHOD.PUT, bucket, key, in, headers, null);
	}

	public PutRequest contentLength(long length) {
		this.length = length;
		return header("Content-Length", Long.toString(length));
	}

	@Override
	public void put(String in) throws Exception {
		byte[] bytes = in.getBytes("UTF-8");
		contentLength(bytes.length);
		put(new ByteArrayInputStream(bytes));
	}

	@Override
	public URI signedUri(long expires) throws Exception {
		return parent.signedUri("PUT", bucket, key, headers, expires);
	}

}
