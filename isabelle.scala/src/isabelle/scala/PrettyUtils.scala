
package isabelle.scala

import java.awt.FontMetrics
import javax.xml.parsers._
import org.xml.sax.InputSource
import java.io.StringReader

import scala.collection.JavaConversions._

import isabelle._

object PrettyUtils {

//    private val current_font_metrics: FontMetrics = null
//    private val current_font_family = ""
//    private val current_font_size: Int = 0
//    private val current_margin: Int = 0
//    private val current_body: XML.Body = Nil

    /* document template with style sheets */

  private val template_head =
    """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<style media="all" type="text/css">
"""

  private val template_tail =
"""
</style>
</head>
<body/>
</html>
"""

  implicit def htmlHead(css : String, font_family: String, font_size: Int): String =
    template_head + css +
    "body { font-family: " + font_family + "; font-size: " + font_size + "px; }" +
    // TODO check for raw_px
//    "body { font-family: " + font_family + "; font-size: " + raw_px(font_size) + "px; }" +
    template_tail


//  implicit def renderHtmlBody(body : java.util.List[XML.Tree], fontMetrics : FontMetrics) : org.w3c.dom.Node =
//  {
//    val current_margin = 0
//
//    val bodyList = body.toList;
//    println("Done to List")
//
//    def formatting(div : XML.Tree) = {
//    val formatted = Pretty.formatted(List(div), current_margin, Pretty.font_metric(fontMetrics))
//            .map(t =>
//              XML.Elem(Markup(HTML.PRE, List((Markup.CLASS, Markup.MESSAGE))),
//                HTML.spans(t, true)))
//     println("Doing formatting")
//     formatted
//    }
//
//    println("Done flat map")
//    val html_body =
//      bodyList.flatMap(div =>
//          formatting(div))
//
//    println("Done formatting")
//    XML.document_node(document, XML.elem(HTML.BODY, html_body))
////    val doc = builder.parse(new InputSource(new StringReader(head)))
////
////    println("Parsed head")
////
////    doc.removeChild(doc.getLastChild());
////    println("Removed child")
////    doc.appendChild(XML.document_node(doc, XML.elem(HTML.BODY, html_body)))
////    println("appended Child")
////    doc
////
//    println("Done everything")
//
//  }

  implicit def renderHtmlBody(body : java.util.List[XML.Tree], width : Int, fontMetrics : FontMetrics) : org.w3c.dom.Node =
  {

    val builder = DocumentBuilderFactory.newInstance.newDocumentBuilder

    val html_body =
      body.toList.flatMap(div =>
        Pretty.formatted(List(div), width, Pretty.font_metric(fontMetrics))
        .map(t =>
          XML.Elem(Markup(HTML.PRE, List((Markup.CLASS, Markup.MESSAGE))),
                   HTML.spans(t, true))))

    val doc = builder.newDocument
    XML.document_node(doc, XML.elem(HTML.BODY, html_body))
  }

//   implicit def renderHtml(body : java.util.List[XML.Tree], css : String, fontMetrics : FontMetrics, fontFamily : String, fontSize: Int) =
//  {
//    val current_margin = 0
//
//    val builder = DocumentBuilderFactory.newInstance.newDocumentBuilder
//
//    val html_body =
//      body.toList.flatMap(div =>
//          Pretty.formatted(List(div), current_margin, Pretty.font_metric(fontMetrics))
//            .map(t =>
//              XML.Elem(Markup(HTML.PRE, List((Markup.CLASS, Markup.MESSAGE))),
//                HTML.spans(t, true))))
//
//    val head = template(css, fontFamily, fontSize)
//    val doc = builder.parse(new InputSource(new StringReader(head)))
//    doc.removeChild(doc.getLastChild());
//    doc.appendChild(XML.document_node(doc, XML.elem(HTML.BODY, html_body)))
//    doc
//  }

}
