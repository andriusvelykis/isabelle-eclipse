package isabelle.eclipse.ui.views

import isabelle.HTML
import isabelle.Markup
import isabelle.Pretty
import isabelle.XML
import isabelle.eclipse.ui.IsabelleUIPlugin
import java.io.StringWriter
import java.net.URLDecoder
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Attr
import org.w3c.dom.Element
import org.w3c.dom.Node


/** Utilities to render Isabelle output as a HTML page.
  * @author Andrius Velykis 
  */
object ProverOutputHtml {

  /** A template for HTML file, with placeholders for CSS statements and body, to be used with String.format() */
  val HTML_TEMPLATE =
    """|<?xml version="1.0" encoding="utf-8"?>
       |<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
       |<html xmlns="http://www.w3.org/1999/xhtml">
       |	<head>
       |%1$s
       |		<style media="all" type="text/css">
       |			%2$s
       |		</style>
       |	</head>
       |	%3$s
       |</html>""".stripMargin

  val CSS_TEMPLATE = """|%1$s
                        |pre { white-space: pre-wrap; }
                        |* { font-family: %2$s; font-size: %3$dpx; }""".stripMargin + "\n"

  val CSS_LINK_TEMPLATE = """		<link rel="stylesheet" type="text/css" href="%1$s" />""";
  

  def renderHtmlPage(commandResults: List[XML.Tree], cssPaths: List[String], inlineCss: String,
      fontFamily: String, fontSize: Int): String = {

    val bodyNode = renderHtmlBody(commandResults, 120)
    
    // replace sendback SPANs with hyperlinks otherwise cannot get an event when it is selected
    replaceXmlNode(sendbackToLink)(bodyNode)

    // print body to String
    val bodyHtml = printNode(bodyNode)

    val cssFull = CSS_TEMPLATE.format(inlineCss, fontFamily, fontSize)

    val cssPathsStr = cssPaths.map(CSS_LINK_TEMPLATE.format(_)).mkString("\n")

    htmlPage(cssPathsStr, cssFull, bodyHtml);
  }

  // taken from Isabelle/jEdit, html_panel.scala
  def renderHtmlBody(body: List[XML.Tree], width: Int): org.w3c.dom.Node =
    {
      val builder = DocumentBuilderFactory.newInstance.newDocumentBuilder

      val html_body =
        body.toList.flatMap(div =>
          Pretty.formatted(List(div), width)//, Pretty.font_metric(fontMetrics))
            .map(t =>
              XML.Elem(Markup(HTML.PRE, List((Markup.CLASS, Markup.MESSAGE))),
                HTML.spans(t, true))))

      val doc = builder.newDocument
      XML.document_node(doc, XML.elem(HTML.BODY, html_body))
    }
  
  def printNode(node: Node): String = {
    try {

      val transformer = TransformerFactory.newInstance.newTransformer
//      transformer.setOutputProperty(OutputKeys.INDENT, "yes")
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

      val result = new StreamResult(new StringWriter)
      val source = new DOMSource(node)
      transformer.transform(source, result)

      result.getWriter().toString();
    } catch {
      case ex: TransformerException => {
        IsabelleUIPlugin.log("Problems printing HTML document", ex);
        // use empty node
        ""
      }
    }
  }
  
  def htmlPage(cssPaths: String, inlineCss: String, body: String): String = {

    val validBody = if (body.isEmpty) "<body/>" else body
    HTML_TEMPLATE.format(cssPaths, inlineCss, body)
  }

  /** Extractor object for "sendback" SPAN element in output HTML */
  private object SendbackSpan {
    def unapply(node: Node): Option[(Element, String)] = node match {
      case e: Element if e.getTagName == HTML.SPAN && e.getAttribute(HTML.CLASS) == Markup.SENDBACK =>
        Some((e, e.getTextContent))
      case _ => None
    }
  }
  
  /** Replaces "sendback" SPAN element with a hyperlink */
  private def sendbackToLink: PartialFunction[Node, Node] = {
    case SendbackSpan(elem, content) => {
      val newElem = elem.getOwnerDocument.createElement("a")

      // copy attributes from sendback elem
      val attrs = elem.getAttributes
      val attrSeq = for (index <- 0 until attrs.getLength) yield attrs.item(index)

      attrSeq foreach {
        case a: Attr => newElem.setAttribute(a.getName, a.getValue)
        case _ => // ignore
      }
      
      // set the sendback link as the HREF attribute
      newElem.setAttribute("href", SendbackLink(content))
      // also set the text content
      newElem.setTextContent(content)
      newElem
    }
  }

  /** Extractor and factory object for "sendback" hyperlink text.
    * The link refers to the same page and the "sendback" information (command replacement) is encoded as query parameter.
    */
  object SendbackLink {

    val sendbackPrefix = "#?sendback="
    private val URL_ENCODING = "UTF-8"

    def unapply(link: String): Option[String] = {
      // there can be a prefix before the '#'
      val afterHash = link.dropWhile(_ != '#')
      
      if (afterHash.startsWith(sendbackPrefix)) {
        val rest = afterHash.substring(sendbackPrefix.length)
        // decode the text
        Some(URLDecoder.decode(rest, URL_ENCODING))
      } else {
        None
      }
    }

    def apply(text: String): String = {
      // encode the text
      val encoded = URLEncoder.encode(text, URL_ENCODING)
      // concat with the prefix
      sendbackPrefix + encoded
    }
  }
  
  def replaceXmlNode(repl: PartialFunction[Node, Node])(node: Node) {
    val children = node.getChildNodes;
    val childSeq = for (index <- 0 until children.getLength) yield children.item(index)
    
    childSeq foreach { child =>
      if (repl.isDefinedAt(child)) {
        val replChild = repl.apply(child)
        node.replaceChild(replChild, child)
      } else {
        // continue deeper
        replaceXmlNode(repl)(child)
      }
    }
  }
  
}
