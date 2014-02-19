package com.netaporter.s3

import spray.httpx.unmarshalling.{Unmarshaller, BasicUnmarshallers}
import scala.xml.NodeSeq
import spray.http.{MediaTypes, ContentTypeRange}
import com.netaporter.s3.S3.responses.{Content, ListBucketResult}

object S3Unmarshallers extends S3Unmarshallers

trait S3Unmarshallers extends BasicUnmarshallers {

  implicit val ListBucketUnmarshaller =
    Unmarshaller.delegate[NodeSeq, ListBucketResult](ContentTypeRange(MediaTypes.`application/xml`)) { xml =>
      val name = (xml \ "Name").headOption match {
        case Some(node) => node.text
        case _ => fail("Expected 'Name' tag in response")
      }
      val contents = (xml \\ "Key") map (k => Content(k.text))
      ListBucketResult(name, contents)
    }

  def fail(msg: String) = throw new Exception("Invalid response from AWS S3. " + msg)
}
