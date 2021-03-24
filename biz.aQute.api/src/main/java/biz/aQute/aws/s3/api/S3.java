package biz.aQute.aws.s3.api;

import java.util.List;

/**
 * Interface to the AWS S3 File System.
 * <p>
 * An S3 file system consist of _Buckets_. A Bucket consists of objects.
 */
public interface S3 {

	/**
	 * Create a bucket in different regions. If no region is specified it will
	 * fallback to a default region.
	 *
	 * @param bucket the name of the bucket
	 * @param regions the regions
	 * @return a Bucket
	 */
	Bucket createBucket(String bucket, String... region) throws Exception;

	/**
	 * Delete a bucket.
	 *
	 * @param bucket The bucket name
	 */
	void deleteBucket(String bucket) throws Exception;

	/**
	 * Get a bucket.
	 *
	 * @param bucket The bucket name
	 */
	Bucket getBucket(String name) throws Exception;

	/**
	 * List the existing buckets
	 *
	 */
	List<Bucket> listBuckets() throws Exception;

}
