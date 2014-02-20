s3-spray
========

[![Build Status](https://travis-ci.org/NET-A-PORTER/s3-spray.png?branch=master)](https://travis-ci.org/NET-A-PORTER/s3-spray)

Support for calling the [S3 Rest API](http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketOps.html) via spray-client

> **Note:** This library in the early stages of development and lacking functionality

Introduction
------------

s3-spray adds support to spray for calling the [S3 Rest API](http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketOps.html).
It provides a [`S3`](blob/master/src/main/scala/com/netaporter/s3/S3.scala) `Actor` that accepts messages such as `ListObjects` and returns messages such as `ListBucketResult`. Alternatively
this library provides the individual `spray-client` components that allow you to manually build your own requests such as the
[`S3RequestBuilders`](blob/master/src/main/scala/com/netaporter/s3/S3RequestBuilders.scala) to sign requests and the [`S3Unmarshallers`](blob/master/src/main/scala/com/netaporter/s3/S3Unmarshallers.scala) to parse responses.

For SBT add the dependency `"com.netaporter" %% "s3-spray" % "0.0.1"`


Using the S3 Actor
------------------

Instantiate the [`S3`](blob/master/src/main/scala/com/netaporter/s3/S3.scala) `Actor` with your S3 access key ID and secert access key like so:

```scala
val s3 = context.actorOf(Props(classOf[S3], IO(Http), accessKeyId, secretAccessKey))
```

You can send the `S3` Actor the following messages:

```scala
s3 ! PutBucket("bucket-name")
// Expect a response of PutBucketSuccess

s3 ! GetBucket("bucket-name")
// Expect a response of ListBucketResult

s3 ! DeleteBucket("bucket-name")
// Expect a response of DeleteBucketSuccess
```

Using the S3 Request Builders
-----------------------------

If you prefer not to use the `S3` actor and would like to manually build the HttpRequests you send to S3, you can either
import or mixin [`S3RequestBuilders`](blob/master/src/main/scala/com/netaporter/s3/S3RequestBuilders.scala) to get the `signS3` `RequestTransformer`. This can then be inserted into your
spray-client pipeline to sign the request with your credentials like so:

```scala
val pipeline = (
  signS3(accessKeyId, secretAccessKey)
  ~> sendReceive(transport)
)

val res = pipeline(Get("http://mybucket.s3.amazonaws.com"))
```

Running the unit tests
---------------------

    sbt test

Running the integration tests
-----------------------------

The integration tests require you to have a AWS account. Export your AWS access key id and secret that have read/write
permissions in S3 as environment variables like so:

    export S3_ACCESS_KEY_ID=AHKJGHSUYEJEKJHEKHE
    export S3_SECRET_ACCESS_KEY=a/ads6asd/ad6/D7sda7asdds78as87asd87ds

Then run the tests like so:

    sbt it:test