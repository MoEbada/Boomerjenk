package testlink;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
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

	TestLinkClient testlinkclient;
	String TESTRESULT_XML = Config.getInstance().getTestlinkImportableXmlFilePath();
	static DocumentBuilderFactory FACTORY;
	static {
		FACTORY = DocumentBuilderFactory.newInstance();
		FACTORY.setValidating(true);
		FACTORY.setIgnoringElementContentWhitespace(true);
		}

	XMLParser(TestLinkClient testlinkclient) {
		this.testlinkclient = testlinkclient;
	}
	/**
	 * Parse xml file as DOM document
	 * @param filepath
	 * @return
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public Document parse(String filepath) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilder builder = FACTORY.newDocumentBuilder();
		File file = new File(filepath);
		Document doc = builder.parse(file);
		return doc;
	}

	/**
	 * Use generated test log xml to generate a new xml file to be imported by testlink
	 * @param testPlanName
	 * @param testPlanId
	 * @param buildName
	 * @param buildId
	 * @param platformName
	 * @param platformId
	 * @param logFilePath
	 */
	public void generateResultsXml(String testPlanName, Integer testPlanId, String buildName, Integer buildId,
			String platformName, Integer platformId, String logFilePath) {
		DocumentBuilder builder = null;
		try {
			builder = FACTORY.newDocumentBuilder();
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
		
		Document xmlResult = writeImportableXmlElements(parsedLogFile, builder, testPlanId, testPlanName, buildId, buildName, platformId, platformName);

		createImportableXmlFile(xmlResult);
	}
	
	private Document writeImportableXmlElements(Document parsedLogFile, DocumentBuilder builder, Integer testPlanId, String testPlanName, Integer buildId, String buildName, Integer platformId, String platformName) {
		Document xmlResult = builder.newDocument();

		Element root = xmlResult.createElement("results");
		xmlResult.appendChild(root);

		createChildElement(xmlResult, root, "testplan", testPlanName, testPlanId.toString());
		createChildElement(xmlResult, root, "build", buildName, buildId.toString());
		createChildElement(xmlResult, root, "platform", platformName, platformId.toString());		

		NodeList executedTestFixtures = getTestFixtures(parsedLogFile);
		Integer numTestCases = executedTestFixtures.getLength();
		String tester = parsedLogFile.getElementsByTagName("environment").item(0).getAttributes().getNamedItem("user")
				.getTextContent();

		if (testlinkclient.testFixtureId.size() != numTestCases) {
			System.out.println("\r\n\r\n*******Not all test cases were executed!!!\r\n\r\n");
		}

		for (int i = 0; i < numTestCases; i++) {
			Element testcase = xmlResult.createElement("testcase");

			if (null != executedTestFixtures.item(i)) {
				testcase.setAttribute("id", testlinkclient.testFixtureId
						.get(executedTestFixtures.item(i).getAttributes().getNamedItem("classname").getTextContent()));

				Element testerElement = xmlResult.createElement("tester");
				testerElement.setTextContent(tester);

				Element timestamp = xmlResult.createElement("timestamp");
				timestamp.setTextContent(executedTestFixtures.item(i).getAttributes().getNamedItem("end-time")
						.getTextContent().replace("Z", ""));

				Element result = xmlResult.createElement("result");
				result.setTextContent(executedTestFixtures.item(i).getAttributes().getNamedItem("result")
						.getTextContent().equalsIgnoreCase("passed") ? "p" : "f");

				Element execDuration = xmlResult.createElement("executionDuration");
				execDuration.setTextContent(
						executedTestFixtures.item(i).getAttributes().getNamedItem("duration").getTextContent());

				testcase.appendChild(testerElement);
				testcase.appendChild(timestamp);
				testcase.appendChild(result);
				testcase.appendChild(execDuration);

				root.appendChild(testcase);
			}
		}
		return xmlResult;
	}
	
	private void createImportableXmlFile(Document xmlResult) {
		// create the xml file
		// transform the DOM Object to an XML File
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			System.out.println("TransformerConfigurationException: " + e.getMessage());
		}
		DOMSource domSource = new DOMSource(xmlResult);
		StreamResult streamResult = new StreamResult(new File(TESTRESULT_XML));

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
	
	void createChildElement(Document xmldoc, Element parent, String elementName, String name, String id) {
		Element elem = xmldoc.createElement(elementName);
		elem.setAttribute("name", name);
		elem.setAttribute("id", id);
		
		parent.appendChild(elem);
	}

	/**
	 * Get all executed test fixtures from generated test log
	 * @param parsedLogFile
	 * @return
	 */
	private NodeList getTestFixtures(Document parsedLogFile) {
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();
		XPathExpression expr = null;
		NodeList nl = null;
		try {
			expr = xpath.compile("//test-suite[@type=\"TestFixture\" and @runstate=\"Runnable\"]");
			nl = (NodeList) expr.evaluate(parsedLogFile, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			System.out.println("XPathExpressionException: " + e.getMessage());
		}

		return nl;
	}
	
	Testcases getTestCasesFromTestReport(String xmlFilePath) {
		Testcases testcases = null;
		try {

			File file = new File(xmlFilePath);
			JAXBContext jaxbContext = JAXBContext.newInstance(Testcases.class);
		    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		     
		    //We had written this file in marshalling example
		    testcases = (Testcases) jaxbUnmarshaller.unmarshal(file);
		     
		    for(Testcase tc : testcases.getTestcases())
		    {
		        System.out.println(tc.getId());
		        System.out.println(tc.getTester());
		    }

		  } catch (JAXBException e) {
			e.printStackTrace();
		  }
		return testcases;
	}
}
