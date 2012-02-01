package cognitiveentity.data

import org.specs2.mutable._
import akka.dispatch.Future
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import org.specs2.execute.Result

@RunWith(classOf[JUnitRunner])
object MonitorTest extends org.specs2.mutable.SpecificationWithJUnit {
  def ident[T](v: T) = v

  "int" in {
    implicit def stringToInt(s: String) = Integer.parseInt(s)
    val m: Monitor[Int] = "123"
    m { _ * 2 } must beEqualTo(246)
  }

  "float" in {
    implicit def stringToFloat(s: String) = java.lang.Float.parseFloat(s)
    val m: Monitor[Float] = "123"
    m { _ * 2 } must beEqualTo(246F)
  }

  "string" in {
    val m: Monitor[String] = "123"
    m { _ * 2 } must beEqualTo("123123")
  }

  "convert" in {
    val mint: Monitor[Int] = 123
    val mfloat = mint.convert { _ * 2F }
    mfloat { ident } must beEqualTo(246F)
  }

  "depend" in {
    val mint: Monitor[Int] = 123

    val mdep = mint.depend { _ * 2F }
    mdep { ident } must beEqualTo(246F)
    val depdep = mdep.depend { _.toString }
    depdep { ident } must beEqualTo("246.0")

  }

  "type" in {
    val mint: Monitor[Int] = 123
    val xint: Monitor[Int] = 123

    val x = new mint.Dependent(xint, 12) //TODO want failure
    success
  }

  "implicit" in {
    implicit def toString[T](v: Monitor[T]): String = v { _.toString }
    val mint: Monitor[Int] = 123
    val s: String = mint
    s must beEqualTo("123")
  }

  "dom" in {
    def content(d: Document) = d.getDocumentElement.getTextContent
    implicit def toDom(s: String): Document = (DocumentBuilderFactory.newInstance().newDocumentBuilder()).parse(new InputSource(new StringReader(s)))
    val mdoc: Monitor[Document] = "<xml><key>value</key></xml>"
    mdoc { content } must beEqualTo("value")
    val mstring = mdoc.convert(content)
    mstring { ident } must beEqualTo("value")
  }

  "match" in {
    val mstring: Monitor[String] = "123"
    //need explicit type to make compiler happy
    val result: Result = mstring match {
      case m: Monitor[String] => mstring(ident) must beEqualTo("123")
      case _ => failure
    }
    result
  }

}