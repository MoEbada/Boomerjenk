package testlink;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class XMLParser {

	public static Document parse(String filepath) throws ParserConfigurationException, IOException, SAXException {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setValidating(true);
	    factory.setIgnoringElementContentWhitespace(true);
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    File file = new File(filepath);
	    Document doc = builder.parse(file);
	    return doc;
	}
	
	public void generateResultsXml(String testPlanName, Integer testPlanId, String buildName, Integer buildId,
			String platformName, Integer platformId, String logFilePath) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setValidating(true);
	    factory.setIgnoringElementContentWhitespace(true);
	    DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			System.out.println("ParserConfigurationException: " + e.getMessage());
		}
		File file = new File(logFilePath);
		Document parsedLogFile = null;
	    try {
			parsedLogFile = builder.parse(file);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			System.out.println("Log file parsing exception: " + e.getMessage());
		}
	    Document xmlResult = builder.newDocument();
	    
	    Element root = xmlResult.createElement("results");
	    xmlResult.appendChild(root);
	    
	    Element testplan = xmlResult.createElement("testplan");
	    testplan.setAttribute("name", testPlanName);
	    testplan.setAttribute("id", testPlanId.toString());
	    
	    Element build = xmlResult.createElement("build");
	    build.setAttribute("name", buildName);
	    build.setAttribute("id", buildId.toString());
	    
	    Element platform = xmlResult.createElement("platform");
	    platform.setAttribute("name", platformName);
	    platform.setAttribute("id", platformId.toString());

	    root.appendChild(testplan);
	    root.appendChild(build);
	    root.appendChild(platform);
	    
	    Integer numTestCases = new Integer(parsedLogFile.getElementsByTagName("test-run").item(0).getAttributes().getNamedItem("total").getTextContent());
	    String tester = parsedLogFile.getElementsByTagName("environment").item(0).getAttributes().getNamedItem("user").getTextContent();
	    for (int i=0; i<numTestCases; i++) {
	    	Element testcase = xmlResult.createElement("testcase");
	    	Node parsedTestCase = parsedLogFile.getElementsByTagName("test-case").item(i);
//	    	testcase.setAttribute("name", parsedTestCase.getAttributes().getNamedItem("name").getTextContent());
//	    	testcase.setAttribute("name", ((Element)parsedTestCase).getElementsByTagName("property").item(0).getAttributes().getNamedItem("value").getTextContent());
	    	
//	    	testcase.setAttribute("id", parsedTestCase.getAttributes().getNamedItem("id").getTextContent());
//	    	if (((Element)parsedTestCase).getElementsByTagName("property").getLength() > 0) {
//	    		testcase.setAttribute("id", testlink.TestLinkClient.tcName_Id.get(((Element)parsedTestCase)
//	    				.getElementsByTagName("property").item(0).getAttributes()
//	    				.getNamedItem("value").getTextContent()));
//	    	}
	    	
	    	NodeList testFixtures = getTestFixtures(parsedLogFile);
	    	
	    	testcase.setAttribute("id", testlink.TestLinkClient.testFixtureId.get(testFixtures.item(i).getAttributes().getNamedItem("classname").getTextContent()));
	    	
	    	Element testerElement = xmlResult.createElement("tester");
	    	testerElement.setTextContent(tester);
	    	
	    	Element timestamp = xmlResult.createElement("timestamp");
	    	timestamp.setTextContent(parsedTestCase.getAttributes().getNamedItem("end-time").getTextContent().replace("Z", ""));
	    	
	    	Element result = xmlResult.createElement("result");
	    	result.setTextContent(parsedTestCase.getAttributes().getNamedItem("result").getTextContent().equalsIgnoreCase("passed")? "p" : "f");
	    	
	    	Element execDuration = xmlResult.createElement("executionDuration");
	    	execDuration.setTextContent(parsedTestCase.getAttributes().getNamedItem("duration").getTextContent());
//	    	execDuration.setTextContent("<![CDATA[" + parsedTestCase.getAttributes().getNamedItem("duration").getTextContent() + "]]");
	    	
	    	
	    	testcase.appendChild(testerElement);
	    	testcase.appendChild(timestamp);
	    	testcase.appendChild(result);
	    	testcase.appendChild(execDuration);
	    	
	    	root.appendChild(testcase);
	    }
	    
	    // create the xml file
        //transform the DOM Object to an XML File
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			System.out.println("TransformerConfigurationException: " + e.getMessage());
		}
        DOMSource domSource = new DOMSource(xmlResult);
        StreamResult streamResult = new StreamResult(new File("D:\\testresult.xml"));

        // If you use
        // StreamResult result = new StreamResult(System.out);
        // the output will be pushed to the standard output ...
        // You can use that for debugging 

        try {
			transformer.transform(domSource, streamResult);
		} catch (TransformerException e) {
			e.printStackTrace();
			System.out.println("TransformerException: " + e.getMessage());
		}

        System.out.println("Done creating XML File");
	}

	private NodeList getTestFixtures(Document parsedLogFile) {
		XPathFactory xPathfactory = XPathFactory.newInstance();
    	XPath xpath = xPathfactory.newXPath();
    	XPathExpression expr = null;
    	NodeList nl = null;
		try {
			expr = xpath.compile("//test-suite[@type=\"TestFixture\"]");
			nl = (NodeList) expr.evaluate(parsedLogFile, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			System.out.println("XPathExpressionException: " + e.getMessage());
		}
    	
		return nl;
	}
}
