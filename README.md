s3-spray
========

Support for calling the [S3 Rest API](http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketOps.html) via spray-client

> **Note:** This library in the early stages of development and not complete

Introduction
------------

s3-spray adds support to spray for calling the [S3 Rest API](http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketOps.html).
It provides a `S3` actor that accepts messages such as `ListObjects` and returns messages such as `ListBucketResult`. Alternatively
this library provides the individual `spray-client` components that allow you to manually build your own requests such as the
`S3RequestBuilders` to sign requests and the `S3Unmarshallers` to parse responses.

For SBT add the dependency `"com.netaporter" %% "s3-spray" % "0.0.1"`


Using the S3 Actor
------------------

Instantiate the `S3` Actor with a


Using the S3 Request Builders
-----------------------------

