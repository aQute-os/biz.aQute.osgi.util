package biz.aQute.aws.s3;

import biz.aQute.aws.s3.api.Bucket;


public class BucketImpl implements Bucket {
	final String	name;
	final S3Impl		parent;

	// TODO verify bucket name

	BucketImpl(S3Impl parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public GetRequest getObject(String key) {
		return new GetRequestImpl(parent, this, key);
	}

	@Override
	public void delete(String key) throws Exception {
		parent.construct(S3Impl.METHOD.DELETE, this, key, null, null, null);
	}

	@Override
	public PutRequest putObject(String key) {
		return new PutRequestImpl(parent, this, key);
	}

	@Override
	public ListRequest listObjects() throws Exception {
		return new ListRequestImpl(parent, this);
	}

	public String toString() {
		return name;
	}

}
