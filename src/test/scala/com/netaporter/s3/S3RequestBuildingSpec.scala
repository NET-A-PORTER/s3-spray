package com.netaporter.s3

import org.scalatest.{OptionValues, Matchers, FlatSpecLike}
import spray.http.HttpRequest
import spray.http.HttpHeaders._

class S3RequestBuildingSpec
  extends FlatSpecLike
  with Matchers
  with OptionValues {

  "signS3" should "correctly sign a GET request" in {
    val in = HttpRequest(
      method = GET,
      uri = "https://s3.amazonaws.com/products-test",
      headers = RawHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8") ::
                RawHeader("Date", "Tue, 18 Feb 2014 15:13:39 GMT") ::
                Nil
    )

    val out = S3RequestBuilding.signS3("access-id", "secret-access-key").apply(in)
    val auth = out.header[Authorization].value
    auth.credentials.toString should equal("AWS access-id:")
  }
}
