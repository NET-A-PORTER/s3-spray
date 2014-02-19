s3-spray
========

Support for calling the [S3 Rest API](http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketOps.html) via spray-client

> **Note:** This is in the early stages of development, only a few endpoi

Introduction
------------

s3-spray adds support to spray for calling the [S3 Rest API](http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketOps.html).
It provides a `S3` actor that accepts messages such as `ListObjects` and `DeleteBucket` or alternatively provides the
spray-client pipeline elements to sign the request and unmarshal the response yourself.

For SBT add the dependency `"com.netaporter" %% "s3-spray" % "0.0.1"`


Introduction
------------