package com.dma.web;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Test12 {
	
	static Document document = null;
	static XPath xpath = null;
	static Path path = Paths.get("/home/fr054721/dmagui/ca/model.xml");
	static String namespace = "/project/namespace/namespace[name='SHARED_ZONE']";
	static String localeName = "name[@locale='en-zw']";

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
		if(Files.exists(path)){
			
			try {
				String xml = new String(Files.readAllBytes(path));
				
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				document = builder.parse(new InputSource(new StringReader(xml)));
				
				XPathFactory xfact = XPathFactory.newInstance();
				xpath = xfact.newXPath();

				String xpathExp = namespace + "/querySubject/" + localeName;
				
//				xpathExp = "/project/namespace/namespace[name='SHARED_ZONE']/querySubject[name='TEC_CONTRACT']/queryItemFolder[3]/name[@locale='en-zw']";
//				
//				Node qiF = (Node) xpath.evaluate(xpathExp, document, XPathConstants.NODE);
//
//				System.out.println("qiF=" + qiF.getTextContent());
//				
//				System.exit(0);
				
				System.out.println("xpathExp=" + xpathExp);
				
				NodeList qss = (NodeList) xpath.evaluate(xpathExp, document, XPathConstants.NODESET);

				System.out.println(qss.getLength());
				
				for(int i = 0; i < qss.getLength(); i++) {
					String qsName = qss.item(i).getTextContent(); 
					System.out.println("qsName=" + qsName);
					xpathExp = namespace + "/querySubject[name='" + qsName + "']";
					recurse(xpathExp, document, qsName);
//					NodeList qis = (NodeList) xpath.evaluate(namespace + "/querySubject[name='" + qsName + "']/queryItem/" + localeName, document, XPathConstants.NODESET);
//					NodeList qis = (NodeList) xpath.evaluate("/project/namespace/namespace[name='SHARED_ZONE']/querySubject[name='" + qsName + "']/queryItemFolder[name='1_IDENTIFICATION']/queryItem/name[@locale='en-zw']", document, XPathConstants.NODESET);
//					System.out.println(qis.getLength());
//					for(int j = 0; j < qis.getLength(); j++) {
//						Node qi = qis.item(j);
//						String qiName = qi.getTextContent();
//						System.out.println("*****" + qiName);
//					}
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void recurse(String xpathExp, Document document, String qsName) throws XPathExpressionException {

		int j = 1;
		Node qiF = (Node) xpath.evaluate(xpathExp + "/queryItemFolder[" + j + "]", document, XPathConstants.NODE);
		String folder = "";
		while(qiF != null) {
			String qiFName = qiF.getTextContent();
			System.out.println(qsName + ";" + qiFName + ";" + folder + ";Folder");
			String nextXpathExp = xpathExp + "/queryItemFolder[" + j + "]/" + localeName + "/queryItemFolder[" + j + "]"; 
			System.out.println("nextXpathExp=" + nextXpathExp);
			recurse(nextXpathExp, document, qsName);
			j++;
			qiF = (Node) xpath.evaluate(xpathExp + "/queryItemFolder[" + j + "]", document, XPathConstants.NODE);
		}
		
//		NodeList qiFs = (NodeList) xpath.evaluate(xpathExp + "/queryItemFolder/" + localeName, document, XPathConstants.NODESET);
//		String folder = "";
//		for(int i = 0; i < qiFs.getLength(); i++) {
//			String qiFName = qiFs.item(i).getTextContent();
//			System.out.println(qsName + ";" + qiFName + ";" + folder + ";Folder");
//			String nextXpathExp = xpathExp + "/queryItemFolder[name='" + qiFName + "']/" + localeName; 
//			System.out.println("nextXpathExp=" + nextXpathExp);
//			NodeList subqiFs = qiFs.item(i).getChildNodes();
//			for(int j = 0; j < subqiFs.getLength(); j++) {
//				String subqiFName = subqiFs.item(j).getTextContent();
//				System.out.println(qsName + ";" + subqiFName + ";" + qiFName + ";Folder");
//			}
//		}
//		
//		
//		NodeList qis = (NodeList) xpath.evaluate(xpathExp + "/queryItem/" + localeName, document, XPathConstants.NODESET);
//		for(int i = 0; i < qis.getLength(); i++) {
//			String qiName = qis.item(i).getTextContent();
//			System.out.println(qsName + ";" + qiName + ";;Field");
//		}
		
	}
	
	public static void l2(String xpathExp, Document document, String qsName) throws XPathExpressionException {
		
		System.out.println("xpathExp=" + xpathExp);
		NodeList qiFs = (NodeList) xpath.evaluate(xpathExp, document, XPathConstants.NODESET);
		String folder = "";
		for(int i = 0; i < qiFs.getLength(); i++) {
			String qiFName = qiFs.item(i).getTextContent();
			System.out.println(qsName + ";" + qiFName + ";" + folder + ";Folder");
			String nextXpathExp = xpathExp + "/queryItemFolder[name='" + qiFName + "']/" + localeName; 
		}
	}
	
	
	protected static String getAttrValue(Node node,String attrName) {
	    if ( ! node.hasAttributes() ) return "";
	    NamedNodeMap nmap = node.getAttributes();
	    if ( nmap == null ) return "";
	    Node n = nmap.getNamedItem(attrName);
	    if ( n == null ) return "";
	    return n.getNodeValue();
	}
	
	protected static String getTextContent(Node parentNode,String childName) {
	    NodeList nlist = parentNode.getChildNodes();
	    for (int i = 0 ; i < nlist.getLength() ; i++) {
		    Node n = nlist.item(i);
		    String name = n.getNodeName();
		    if ( name != null && name.equals(childName) ) return n.getTextContent();
	    }
	    return "";
	}			

}
