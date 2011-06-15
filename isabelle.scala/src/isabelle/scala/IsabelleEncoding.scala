
package isabelle.scala

import java.nio.charset.{Charset, CodingErrorAction}
import java.io.{InputStream, OutputStream, Reader, Writer, OutputStreamWriter,
  CharArrayReader, ByteArrayOutputStream, ByteArrayInputStream}

import scala.io.{Codec, BufferedSource}

import scala.collection.JavaConversions._

import isabelle._

object IsabelleEncoding {

  val charset = Charset.forName(Standard_System.charset)
  val BUFSIZE = 32768

  private def text_reader(system : Isabelle_System, in: InputStream, codec: Codec): Reader =
  {
    val source = new BufferedSource(in)(codec)
    new CharArrayReader(system.symbols.decode(source.mkString).toArray)
  }

  def getTextReader(system : Isabelle_System, in: InputStream): Reader =
    text_reader(system, in, Standard_System.codec())

  def getPermissiveTextReader(system : Isabelle_System, in: InputStream): Reader =
  {
    val codec = Standard_System.codec()
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)
    text_reader(system, in, codec)
  }

  def getTextWriter(system : Isabelle_System, out: OutputStream): Writer =
  {
    val buffer = new ByteArrayOutputStream(BUFSIZE) {
      override def flush()
      {
        val text = system.symbols.encode(toString(Standard_System.charset))
        out.write(text.getBytes(Standard_System.charset))
        out.flush()
      }
      override def close() { out.close() }
    }
    new OutputStreamWriter(buffer, charset.newEncoder())
  }
  
  private def textInputStream(system : Isabelle_System, in: InputStream, codec: Codec): InputStream =
  {
    val source = new BufferedSource(in)(codec)
    new ByteArrayInputStream(system.symbols.decode(source.mkString).getBytes(charset))
  }

  def getTextInputStream(system : Isabelle_System, in: InputStream): InputStream =
    textInputStream(system, in, Standard_System.codec())

  def getPermissiveTextInputStream(system : Isabelle_System, in: InputStream): InputStream =
  {
    val codec = Standard_System.codec()
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)
    textInputStream(system, in, codec)
  }

  def getTextOutputStream(system : Isabelle_System, out: OutputStream): OutputStream =
  {
    new ByteArrayOutputStream(BUFSIZE) {
      override def flush()
      {
        println("Encoding raw: " + toString(Standard_System.charset))
        val text = system.symbols.encode(toString(Standard_System.charset))
        println("Writing to file: " + text.getBytes(Standard_System.charset))
        out.write(text.getBytes(Standard_System.charset))
        println("Flushing")
        out.flush()
        println("Flushed")
      }
      override def close() { println("Closing"); out.close(); println("Closed") }
    }
  }

}
