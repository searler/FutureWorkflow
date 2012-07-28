package cognitiveentity.data

import org.specs2.mutable._

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
    val mfloat: Monitor[Float] = mint.convert { _ * 2F }
    mfloat { ident } must beEqualTo(246F)
  }

  "convert via apply" in {
    val mint: Monitor[Int] = 123
    val mfloat: Monitor[Float] = mint { _ * 2F }
    mfloat { ident } must beEqualTo(246F)
  }

  "convert via apply with implicit" in {
    implicit def toString[T](v: T): String = v.toString
    val mint: Monitor[Int] = 123
    val mfloat: Monitor[String] = mint { _ * 2F }
    mfloat { ident } must beEqualTo("246.0")
  }

  "depend" in {
    val mint: Monitor[Int] = 123

    val mdep = mint.depend { _ * 2F }
    mdep { ident } must beEqualTo(246F)
    val depdep = mdep.depend { _.toString }
    depdep { ident } must beEqualTo("246.0")

  }

  "implicit" in {
    implicit def toString[M, T](v: Monitor[T]): String = v { _.toString }
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

  "trait" in {
    sealed trait MA
    sealed trait MB

    val ma: TaggedMonitor[MA, Int] = 123
    ma { _ * 2 } must beEqualTo(246)

    val mb: TaggedMonitor[MB, Int] = 123

    // val mbfail :TaggedMonitor[MB,Int] = ma
    
    def fun(y:Any) = (y match {
      case x: TaggedMonitor[MA, Int] => x
      case _ => null
    })
   

    fun(ma) must beEqualTo(ma)
    
     def disc(y:Any) = (y match {
      case x: TaggedMonitor[MA, Int] => "a"
      case x: TaggedMonitor[MB, Int] => "b"
      case _ => null
    })
    
    disc(ma) must beEqualTo("a")
  //  disc(mb) must beEqualTo("b")

    def fn(v: TaggedMonitor[MB, Int]) = "b"
    def gn(v: TaggedMonitor[MA, Int]) = "a"

    //   fn(ma) must beEqualTo("a")
    fn(mb) must beEqualTo("b")

  }
  
  "dom trait" in {
    def content(d: Document) = d.getDocumentElement.getTextContent
    implicit def toDom(s: String): Document = (DocumentBuilderFactory.newInstance().newDocumentBuilder()).parse(new InputSource(new StringReader(s)))
    
    sealed trait MA
    
     val mdoc: TaggedMonitor[MA, Document] = "<xml><key>value</key></xml>"
    mdoc { content } must beEqualTo("value")
    val mstring = mdoc.convert(content)
    mstring { ident } must beEqualTo("value")
  }

  "case class" in {
    implicit def toString[T](v: T): String = v.toString

    case class A(s: Int) extends BaseMonitor[String, Int]

    val a = A(12)
    a.s must beEqualTo(12)
    a { _ * 2 } must beEqualTo("1212")
    // a.v inaccessible
    type XML = BaseMonitor[org.w3c.dom.Document, String]
    implicit def toDom(s: String): Document = (DocumentBuilderFactory.newInstance().newDocumentBuilder()).parse(new InputSource(new StringReader(s)))

    case class XA(s: String) extends XML

    val mdoc = XA("<xml><key>value</key></xml>")
    mdoc { _.getDocumentElement.getTextContent } must beEqualTo("value")
  }

  "type" in {
    sealed trait MA
    sealed trait MB

    case class H[M, T](v: T)

    var v1 = H[MA, Int](12)
    var v2 = H[MB, Int](12)

   //   v1 = v2

    success
  }

}