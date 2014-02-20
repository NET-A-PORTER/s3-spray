package com.netaporter.s3

import org.scalatest.{ OptionValues, Matchers, FlatSpecLike }
import spray.http.HttpRequest
import spray.http.HttpHeaders._
import spray.http.HttpMethods._

class S3RequestBuildingSpec
    extends FlatSpecLike
    with Matchers
    with OptionValues
    with S3RequestBuilding {

  // Tue, 18 Feb 2014 15:13:39 GMT
  override val currentTimeMillis = 1392736419000l

  "signS3" should "correctly sign a GET request" in {
    val in = HttpRequest(
      method = GET,
      uri = "https://s3.amazonaws.com/products-test",
      headers = RawHeader("Date", "Tue, 18 Feb 2014 15:13:39 GMT") :: Nil
    )

    val out = signS3("access-id", "secret-access-key").apply(in)
    val auth = out.headers.find(_.name == "Authorization")
    auth.value.value should equal("AWS access-id:1n94fVlDK4+AyO/Dx/8Fz6VfWQM=")
  }

  it should "correctly sign a GET request for bucket names with dots" in {
    val in = HttpRequest(
      method = GET,
      uri = "https://products.test.s3.amazonaws.com",
      headers = RawHeader("Date", "Tue, 18 Feb 2014 15:13:39 GMT") :: Nil
    )

    val out = signS3("access-id", "secret-access-key").apply(in)
    val auth = out.headers.find(_.name == "Authorization")
    auth.value.value should equal("AWS access-id:qhTefrcgoCjwk/MrCPWMS+9C6jk=")
  }
}
