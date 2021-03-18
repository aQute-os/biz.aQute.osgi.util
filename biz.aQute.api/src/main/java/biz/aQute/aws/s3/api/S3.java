package biz.aQute.aws.s3.api;

import java.util.List;

public interface S3 {

	Bucket createBucket(String bucket, String ...region) throws Exception;

	void deleteBucket(String bucket) throws Exception;

	Bucket getBucket(String name) throws Exception;

	List<Bucket> listBuckets() throws Exception;

}
