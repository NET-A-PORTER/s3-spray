package com.netaporter.s3

import spray.http.{ HttpRequest, DateTime }
import spray.http.HttpHeaders.{ RawHeader, Date }
import spray.http.HttpEntity.NonEmpty
import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import spray.httpx.RequestBuilding

object S3RequestBuilding extends S3RequestBuilding

trait S3RequestBuilding extends RequestBuilding {

  // RequestTransformers

  def signS3(accessKeyId: String, secretAccessKey: String): RequestTransformer = r => {
    val now = DateTime(currentTimeMillis)
    val sts = stringToSign(r, now)
    val signature = base64(hmacSha1(sts.getBytes("UTF-8"), secretAccessKey))

    val addHeaders =
      Date(now) ::
        RawHeader("Authorization", s"AWS $accessKeyId:$signature") ::
        r.headers

    r.copy(headers = addHeaders ::: r.headers)
  }

  def getBucket(prefix: String): RequestTransformer =
    if (prefix.nonEmpty) addParam("prefix" -> prefix) else identity

  def addParam(kv: (String, String)): RequestTransformer = r => {
    val query = kv +: r.uri.query
    r.copy(uri = r.uri withQuery query)
  }

  // Helpers

  val paramsToSign = Set(
    "acl", "lifecycle", "location", "logging", "notification", "partNumber", "policy", "requestPayment",
    "torrent", "uploadId", "uploads", "versionId", "versioning", "versions", "website", "response-content-type",
    "response-content-language", "response-expires", "response-cache-control", "response-content-disposition",
    "response-content-encoding", "delete")

  private def stringToSign(r: HttpRequest, date: DateTime) = {
    val (contentMd5, contentType) = r.entity match {
      case NonEmpty(ct, data) => md5(data.toByteArray) -> ct.toString
      case _ => "" -> ""
    }

    val amzHeaders = r.headers
      .collect { case h if h.lowercaseName.startsWith("x-amz-") => h.toString }
      .mkString("\n")

    val subdomain = r.uri.authority.host.address.split('.').head

    val canonicalizedResource =
      (if (subdomain == "s3") "" else "/" + subdomain) +
        r.uri.path.toString + "/" +
        r.uri.query.filter { case (k, _) => paramsToSign.contains(k) }.toString

    r.method + "\n" +
      contentMd5 + "\n" +
      contentType + "\n" +
      date.toRfc1123DateTimeString + "\n" +
      amzHeaders +
      canonicalizedResource
  }

  private def base64(bytes: Array[Byte]) = {
    new sun.misc.BASE64Encoder().encode(bytes)
  }

  private def md5(bytes: Array[Byte]) = {
    val md = MessageDigest.getInstance("MD5")
    val bytesOut = md.digest(bytes)
    new String(bytesOut, "UTF-8")
  }

  private def hmacSha1(bytes: Array[Byte], secret: String) = {
    val signingKey = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(signingKey)
    mac.doFinal(bytes)
  }

  def currentTimeMillis = System.currentTimeMillis
}
