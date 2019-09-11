package testlink;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
	static final DocumentBuilderFactory FACTORY;
	private static final XPath XPATH;
	static {
		FACTORY = DocumentBuilderFactory.newInstance();
		FACTORY.setValidating(true);
		FACTORY.setIgnoringElementContentWhitespace(true);

		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPATH = xPathfactory.newXPath();
	}

	XMLParser(TestLinkClient testlinkclient) {
		this.testlinkclient = testlinkclient;
	}

	/**
	 * Parse xml file as DOM document
	 * 
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
	 * Use generated test log xml to generate a new xml file to be imported by
	 * testlink
	 * 
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

		Document xmlResult = writeImportableXmlElements(parsedLogFile, builder, testPlanId, testPlanName, buildId,
				buildName, platformId, platformName);

		createImportableXmlFile(xmlResult);
	}

	private Document writeImportableXmlElements(Document parsedLogFile, DocumentBuilder builder, Integer testPlanId,
			String testPlanName, Integer buildId, String buildName, Integer platformId, String platformName) {
		XPathExpression expr = null;
		Document xmlResult = builder.newDocument();

		Element root = xmlResult.createElement("results");
		xmlResult.appendChild(root);

		createChildElement(xmlResult, root, "testplan", testPlanName, testPlanId.toString());
		createChildElement(xmlResult, root, "build", buildName, buildId.toString());
		createChildElement(xmlResult, root, "platform", platformName, platformId.toString());

		NodeList executedTestFixtures = getTestFixtures(parsedLogFile);

		// if no test fixtures were executed, skip writing test cases and return the
		// document in current state.
		// Test cases are written only when there are executed test fixtures.
		if (null == executedTestFixtures) {
			return xmlResult;
		}
		Integer numTestCases = executedTestFixtures.getLength();
		String tester = ((Element)(parsedLogFile.getElementsByTagName("environment").item(0))).getAttribute("user");

		if (testlinkclient.testFixtureId.size() != numTestCases) {
			System.out.println("\r\n\r\n*******Not all test cases were executed!!!\r\n\r\n");
		}

		try {
			expr = XPATH.compile("./properties/property[@name=\"tcid\"]/@value");
		} catch (XPathExpressionException e1) {
			System.out.println("XPathExpressionException: " + e1.getMessage());
			e1.printStackTrace();
		}
		for (int i = 0; i < numTestCases; i++) {
			Element executedTestFixture = (Element) executedTestFixtures.item(i);
			Element testcase = xmlResult.createElement("testcase");

			if (null != executedTestFixture) {
				String testFixtureProperty = "";
				try {
					testFixtureProperty = (String) expr.evaluate(executedTestFixture, XPathConstants.STRING);
				} catch (XPathExpressionException e) {
					System.out.println("XPathExpressionException: " + e.getMessage());
					e.printStackTrace();
				} catch (NullPointerException e) {
					System.out.println("NullPointerException: " + e.getMessage());
					e.printStackTrace();
				}

				testcase.setAttribute("FullExternalId", testFixtureProperty);

				testcase.setAttribute("id", testlinkclient.testFixtureId.get(testFixtureProperty));

				createChildElementWithTextOnly(xmlResult, testcase, "tester", tester);
				createChildElementWithTextOnly(xmlResult, testcase, "timestamp", executedTestFixture.getAttribute("end-time").replace("Z", ""));
				createChildElementWithTextOnly(xmlResult, testcase, "result", (executedTestFixture.getAttribute("result").equalsIgnoreCase("passed") ? "p" : "f"));
				createChildElementWithTextOnly(xmlResult, testcase, "executionDuration", executedTestFixture.getAttribute("duration"));

				root.appendChild(testcase);
			}
		}
		return xmlResult;
	}
	
	

	private void createChildElementWithTextOnly(Document doc, Element parent, String childElementName, String textContent) {
		Element elem = doc.createElement(childElementName);
		elem.setTextContent(textContent);
		parent.appendChild(elem);
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
	 * 
	 * @param parsedLogFile
	 * @return
	 */
	private NodeList getTestFixtures(Document parsedLogFile) {
		NodeList nl = null;
		try {
			XPathExpression expr = XPATH.compile(
					"//test-suite[@type=\"TestFixture\" and @runstate=\"Runnable\" and descendant::properties]");
			nl = (NodeList) expr.evaluate(parsedLogFile, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			System.out.println("XPathExpressionException: " + e.getMessage());
		}

		return nl;
	}

	Testcases getTestCasesFromTestReport(String xmlFilePath) {
		System.out.println("Parsing testcases from xml to java objects..");
		Testcases testcases = null;
		try {

			File file = new File(xmlFilePath);
			JAXBContext jaxbContext = JAXBContext.newInstance(Testcases.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			// We had written this file in marshalling example
			testcases = (Testcases) jaxbUnmarshaller.unmarshal(file);

			if (null == testcases.getTestcases() || testcases.getTestcases().size() <= 0) {
				System.out.println("No test cases were executed!");
			} else {
				System.out.println("Following testcases were executed successfully:");
				for (Testcase tc : testcases.getTestcases()) {
					System.out.println("Testcase with ID \'" + tc.getId() + "\'" + "by user <" + tc.getTester() + ">");
				}
			}

		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return testcases;
	}
}
