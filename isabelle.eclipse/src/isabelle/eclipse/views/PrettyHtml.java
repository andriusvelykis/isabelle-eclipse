package isabelle.eclipse.views;

import isabelle.Isabelle_System;
import isabelle.XML.Tree;
import isabelle.eclipse.IsabelleEclipsePlugin;
import isabelle.scala.PrettyUtils;

import java.io.StringWriter;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

public class PrettyHtml {

	/**
	 * A template for HTML file, with placeholders for CSS statements and body, to be used with String.format()
	 */
	private static final String HTML_TEMPLATE = 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
	  + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
	  + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
	  + "	<head>\n" 
	  + "		<style media=\"all\" type=\"text/css\">\n"
	  + "			%1$s\n"
	  + "		</style>\n"
	  + "	</head>\n"
	  + "	%2$s\n"
	  + "</html>";
	
	private static final String CSS_TEMPLATE =
		"%1$s\n" +
		"pre { white-space: pre-wrap; }\n" +
	    "* { font-family: %2$s; font-size: %3$dpx; }";
	
	public static String getHtmlPage(String css, String body) {
		
		if (body == null || body.isEmpty()) {
			body = "<body/>";
		}
		
		return String.format(HTML_TEMPLATE, css, body);
	}
	
	public static String renderHtmlPage(Isabelle_System system, List<Tree> commandResults, 
			String css, String fontFamily, int fontSize) {
		
		Node bodyNode = PrettyUtils.renderHtmlBody(commandResults, 120, null);
		
		// print body to String
		String bodyHtml = printNode(system, bodyNode);
		
		String cssFull = String.format(CSS_TEMPLATE, css, fontFamily, fontSize);
		
		return getHtmlPage(cssFull, bodyHtml);
	}
	
	private static String printNode(Isabelle_System system, Node node) {
		try {
		
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
//			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(node);
			transformer.transform(source, result);
	
			String output = result.getWriter().toString();
			
			return output;
		} catch (TransformerException ex) {
			IsabelleEclipsePlugin.getDefault().log("Problems printing HTML document", ex);
			return "";
		}
	}
	
}
