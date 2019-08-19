package testlink;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import br.eti.kinoshita.testlinkjavaapi.TestLinkAPI;
import br.eti.kinoshita.testlinkjavaapi.constants.ActionOnDuplicate;
import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionStatus;
import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionType;
import br.eti.kinoshita.testlinkjavaapi.constants.TestCaseDetails;
import br.eti.kinoshita.testlinkjavaapi.constants.TestCaseStatus;
import br.eti.kinoshita.testlinkjavaapi.constants.TestCaseStepAction;
import br.eti.kinoshita.testlinkjavaapi.constants.TestImportance;
import br.eti.kinoshita.testlinkjavaapi.model.Attachment;
import br.eti.kinoshita.testlinkjavaapi.model.CustomField;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import br.eti.kinoshita.testlinkjavaapi.model.TestCaseStep;
import br.eti.kinoshita.testlinkjavaapi.model.TestCaseStepResult;
import br.eti.kinoshita.testlinkjavaapi.util.TestLinkAPIException;

public class TestLinkClient {

	static final String testlinkLogin = "http://s001thermo.siouxehv.nl/testlink/login.php";
	static final String url = "http://s001thermo.siouxehv.nl/testlink/lib/api/xmlrpc/v1/xmlrpc.php";
	static final String devKey = "4835f134e99b352c32fbe30512656580";
	static TestLinkAPI api = null;
	static testlink.TestLinkAPI api_tcResult = null;	//Modified TestLink API to handle string "execution duration" for test results
	static URL testlinkURL;
	static XMLParser parser = null;
	static String xmlFilePath = "D:\\testresult.xml";
	static Document parsedXml = null;
	static String testProjectName = "D2i Demo Project";
	static String testPlanName = null;
	static Integer testPlanId = null;
	static Integer buildId = null;
	static String buildName = null;
	static Integer platformId = null;
	static String platformName = null;
	static String logFilePath = "D:\\nunitoutput\\TestResult.xml";
	public static Map<String, String> tcName_Id = new HashMap<String, String>();
	private static Process p;
	private static int exitVal = 0;
	public static Map<String, String> testFixtureId = new HashMap<String, String>();
	static String Execution_Command = "START /WAIT D:\\nunit-console\\bin\\net35\\nunit3-console.exe \"D:\\ReportNunitTestResultToTestlink\\Testlink.Test\\bin\\Debug\\Testlink.Test.dll\" --where:\"\" --work=\"D:\\nunitoutput\" \r\n exit";
	private static final String TESTFIXTURE_CUSTOMFIELD = "testfixture";

	/*************** update test case result ***************/
	private static void updateTestCases() {
		for (int tcNum = 0; tcNum < parsedXml.getElementsByTagName("testcase").getLength(); tcNum++) {
			Node testcase = parsedXml.getElementsByTagName("testcase").item(tcNum);

			Integer testPlanId = new Integer(parsedXml.getElementsByTagName("testplan").item(0).getAttributes()
					.getNamedItem("id").getTextContent());
			Integer buildId = new Integer(parsedXml.getElementsByTagName("build").item(0).getAttributes()
					.getNamedItem("id").getTextContent());
			String buildName = parsedXml.getElementsByTagName("build").item(0).getAttributes().getNamedItem("name")
					.getTextContent();
			Integer platformId = new Integer(parsedXml.getElementsByTagName("platform").item(0).getAttributes()
					.getNamedItem("id").getTextContent());
			String platformName = parsedXml.getElementsByTagName("platform").item(0).getAttributes()
					.getNamedItem("name").getTextContent();

			Integer testCaseId = new Integer(testcase.getAttributes().getNamedItem("id").getTextContent());
//			Integer testCaseExternalId = new Integer(
//					testcase.getAttributes().getNamedItem("external_id").getTextContent());

			String notes, user, timestamp, bugId;
			notes = user = timestamp = bugId = null;
//			Integer executionDuration = null;
			String executionDuration = null;
			Boolean overwrite, guess;
			overwrite = guess = null;
			List<TestCaseStepResult> stepsResult = new LinkedList<TestCaseStepResult>();
			ExecutionType executionType = null;
			ExecutionStatus stepStatus, tcStatus;
			stepStatus = tcStatus = null;
			Map<String, String> customFields = null;

			NodeList tcData = testcase.getChildNodes();
			for (int i = 0; i < tcData.getLength(); i++) {
				if (tcData.item(i).getNodeType() == Node.ELEMENT_NODE) {
					switch (tcData.item(i).getNodeName()) {
//					case "notes":
//						notes = tcData.item(i).getTextContent();
//						break;

					case "executionDuration":
//						executionDuration = new Integer(tcData.item(i).getTextContent());
						executionDuration = tcData.item(i).getTextContent();
						break;

//					case "overwrite":
//						overwrite = new Boolean(tcData.item(i).getTextContent());
//						break;

					case "tester":
						user = tcData.item(i).getTextContent();
						break;

					case "timestamp":
						timestamp = tcData.item(i).getTextContent();
						break;

//					case "guess":
//						guess = new Boolean(tcData.item(i).getTextContent());
//						break;

//					case "bug_id":
//						bugId = tcData.item(i).getTextContent();
//						break;

					case "result":
						tcStatus = getExecutionStatus(tcData.item(i).getTextContent());
						break;

					case "stepresult":
						NamedNodeMap attributes = tcData.item(i).getAttributes();
						stepStatus = getExecutionStatus(attributes.getNamedItem("status").getTextContent());
						executionType = getExecutionType(attributes.getNamedItem("exectype").getTextContent());

						stepsResult.add(new TestCaseStepResult(
								new Integer(attributes.getNamedItem("num").getTextContent()), stepStatus,
								tcData.item(i).getTextContent(),
								new Boolean(attributes.getNamedItem("active").getTextContent()), executionType));
						break;

					default:
						System.out.println(
								"Element " + tcData.item(i).getNodeName() + "has been found and not assigned.");
						break;
					}
				}
			}

			//Additional test case information that are currently not available
//			api.reportTCResult(testCaseId, testCaseExternalId, testPlanId, tcStatus, stepsResult, buildId, buildName,
//					notes, executionDuration, guess, bugId, platformId, platformName, customFields, overwrite, user,
//					timestamp);
//			api.reportTCResult(testCaseId, null, testPlanId, tcStatus, null, buildId, buildName, "", executionDuration,
//					true, null, platformId, platformName, customFields, true, user, timestamp);
			api_tcResult.reportTCResult(testCaseId, null, testPlanId, tcStatus, null, buildId, buildName, "",
					executionDuration, true, null, platformId, platformName, customFields, true, user, timestamp);
		}
	}

	private static ExecutionType getExecutionType(String textContent) {
		ExecutionType executionType = null;
		if (textContent.equalsIgnoreCase("A")) {
			executionType = ExecutionType.AUTOMATED;
		} else if (textContent.equalsIgnoreCase("M")) {
			executionType = ExecutionType.MANUAL;
		} else {
			System.out.println("Invalid step execution type!");
		}
		return executionType;
	}

	private static ExecutionStatus getExecutionStatus(String textContent) {
		ExecutionStatus status = null;
		if (textContent.equalsIgnoreCase("p")) {
			status = ExecutionStatus.PASSED;
		} else if (textContent.equalsIgnoreCase("f")) {
			status = ExecutionStatus.FAILED;
		} else if (textContent.equalsIgnoreCase("n")) {
			status = ExecutionStatus.NOT_RUN;
		} else if (textContent.equalsIgnoreCase("b")) {
			status = ExecutionStatus.BLOCKED;
		} else {
			System.out.println("Invalid step result status!");
		}
		return status;
	}

	/************
	 * Import test case result from XML
	 * 
	 * @return parsed xml document to extract test results
	 **************/
	private static Document parseXmlResults(String xmlFilePath) {
		try {
			parsedXml = parser.parse(xmlFilePath);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			System.out.println("ParserConfigurationException: " + e.getMessage());
		} catch (SAXException e) {
			e.printStackTrace();
			System.out.println("SAXException: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IOException: " + e.getMessage());
		}
		return parsedXml;
	}

	/***************** Create Test Case ****************/
	/* POSTPONED: Automation of test generation */
	/*
	 * private void createTestCase() { api = new TestLinkAPI(testlinkURL, devKey);
	 * String testCaseName = "DemoAutomated_TestCase"; Integer testSuiteId = 6;
	 * Integer testProjectId = 1; String authorLogin = "mohamedm"; String summary =
	 * "DemoAutomated_Summary"; List<TestCaseStep> steps =
	 * Collections.singletonList(new TestCaseStep(33, 90, 100,
	 * "DemoAutomated_actions", "DemoAutomated-expectedResults", true,
	 * ExecutionType.AUTOMATED)); String preconditions =
	 * "DemoAutomated_preconditions"; TestCaseStatus status =
	 * TestCaseStatus.REVIEW_IN_PROGRESS; TestImportance importance =
	 * TestImportance.HIGH; ExecutionType execution = ExecutionType.AUTOMATED;
	 * Integer order = 123; Integer internalId = 555; Boolean checkDuplicatedName =
	 * true; ActionOnDuplicate actionOnDuplicatedName = ActionOnDuplicate.BLOCK;
	 * 
	 * api.createTestCase(testCaseName, testSuiteId, testProjectId, authorLogin,
	 * summary, steps, preconditions, status, importance, execution, order,
	 * internalId, checkDuplicatedName, actionOnDuplicatedName);
	 * 
	 * }
	 */

	/**
	 * Download and execute test case attachment script. Use TestPlan id, Buld id,
	 * and Platform to select test cases to execute. For each test case in selected
	 * testplan, download attached script and execute it.
	 */
	private static void executeTestScripts() {
		String executableWriteTo = "D:\\attach.bat";
		System.out.println(testPlanId);

		byte[] bytearray = Execution_Command.getBytes();
		try {
			FileUtils.writeByteArrayToFile(new File(executableWriteTo), bytearray);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IOException: " + e.getMessage());
		}

		/* Execute downloaded executable */
		try {
			p = Runtime.getRuntime().exec("cmd /c start /wait " + executableWriteTo);
			exitVal = p.waitFor();
			System.out.println("Exit value: " + exitVal);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IOException: " + e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.out.println("InterruptedException: " + e.getMessage());
		}
	}

	/*********** Creates TestLink API Client ************/
	private static TestLinkAPI getTestLinkApi() {
		try {
			testlinkURL = new URL(url);
		} catch (MalformedURLException mue) {
			mue.printStackTrace(System.err);
			System.out.println("MalformedURLException: " + mue.getMessage());
		}

		try {
			api_tcResult = new testlink.TestLinkAPI(testlinkURL, devKey);
			api = new TestLinkAPI(testlinkURL, devKey);
		} catch (TestLinkAPIException te) {
			te.printStackTrace(System.err);
			System.out.println("TestLinkAPIException: " + te.getMessage());
		}
		return api;
	}

	private static void getTestFixtureAndUpdateExecFilter() {
		TestCase[] executableTestCases = api.getTestCasesForTestPlan(testPlanId, null, null, null, null, null, null,
				null, ExecutionType.AUTOMATED, Boolean.TRUE, null);

		for (int i = 0; i < executableTestCases.length; i++) {
			TestCase tc = executableTestCases[i];

			// For each test case, get test fixture from custom fields
			CustomField cf = api.getTestCaseCustomFieldDesignValue(tc.getId(), tc.getExternalId(), tc.getVersion(), 1,
					TESTFIXTURE_CUSTOMFIELD, null);

			// create test fixture and test case id pairs
			testFixtureId.put(cf.getValue(), tc.getId().toString());

			// Add test fixtures to nunit execution filter
			if (i > 0) {
				Execution_Command = Execution_Command.replaceFirst("--where:\"",
						"--where:\" class == " + cf.getValue() + " || ");
			} else {
				Execution_Command = Execution_Command.replaceFirst("--where:\"",
						"--where:\" class ==  " + cf.getValue());
			}

		}
	}

	private static void getTestPlan_BuildId_Platform() {
		System.out.println(System.getenv("TEST_PLAN"));
		System.out.println(api.getProjects().toString());

		testPlanName = System.getenv("TEST_PLAN");
		testPlanId = api.getTestPlanByName(testPlanName, testProjectName).getId();
		buildName = System.getenv("BUILD_ID");
		buildId = api.getBuildsForTestPlan(testPlanId)[0].getId();
		platformName = System.getenv("PLATFORM");
		platformId = api.getTestPlanPlatforms(testPlanId)[0].getId();

	}

	public static void main(String[] args) throws IOException {
		// XML parser to read and write test execution log and testlink importable file
		parser = new XMLParser();

		// Testlink API client to communicate with Testlink
		api = getTestLinkApi();

		// Get jenkins build parameters to use them to extract test cases to execute
		getTestPlan_BuildId_Platform();

		/**
		 * Create pairs of test case id and test fixture to be executed by nunit. 
		 * Update @Execution_Command with filtered test cases
		 */
		getTestFixtureAndUpdateExecFilter();

		// Execute selected test fixtures and and wait for log file to be created
		executeTestScripts();

		// Build Testlink-importable xml file containing test results obtained from
		// generated log file, that was created by execution of nunit tests
		parser.generateResultsXml(testPlanName, testPlanId, buildName, buildId, platformName, platformId, logFilePath);
		
		//parse importable xml to be ready for updating test results
		parsedXml = parseXmlResults(xmlFilePath);
		
		//Use parsed document to extract test results and update Testlink
		updateTestCases();

		System.out.println("Testlink - Test cases have been updated successfully");

	}

}
