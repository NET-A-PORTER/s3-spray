package com.netaporter.s3

import spray.httpx.unmarshalling._
import scala.xml.NodeSeq
import spray.http._
import spray.http.StatusCodes._
import spray.httpx.{ ResponseTransformation, PipelineException }
import com.netaporter.s3.S3.responses
import spray.http.HttpResponse
import com.netaporter.s3.S3.responses.{ GetObjectResult, ListBucketResult, Content }
import scala.Some

object S3Unmarshallers extends S3Unmarshallers

trait S3Unmarshallers
    extends BasicUnmarshallers
    with ResponseTransformation {

  def s3Unmarshal[T: FromResponseUnmarshaller]: HttpResponse ⇒ T =
    response => response.status match {
      case x if x.isSuccess => unmarshal[T].apply(response)
      case Unauthorized => throw responses.Unauthorized
      case Forbidden => throw responses.Forbidden
      case x => throw new PipelineException(s"S3 API returned status code ${x.intValue}")
    }

  implicit val noEntityUnmarshaller = new Unmarshaller[Unit] {
    def apply(entity: HttpEntity) = Right(())
  }

  implicit val ListBucketUnmarshaller =
    s3Unmarshaller[ListBucketResult] { xml =>
      val name = (xml \ "Name").headOption match {
        case Some(node) => node.text
        case _ => fail("Expected 'Name' tag in response")
      }
      val contents = (xml \\ "Key") map (k => Content(k.text))
      ListBucketResult(name, contents)
    }

  implicit val GetObjectUnmarshaller =
    new Unmarshaller[GetObjectResult] {
      def apply(entity: HttpEntity) = Right(GetObjectResult(entity.data.toByteArray))
    }

  def s3Unmarshaller[T](f: NodeSeq => T): Unmarshaller[T] =
    Unmarshaller.delegate[NodeSeq, T](ContentTypeRange(MediaTypes.`application/xml`))(f)

  def fail(msg: String) = throw new Exception("Invalid response from AWS S3. " + msg)
}
