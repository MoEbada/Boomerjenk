package testlink;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import br.eti.kinoshita.testlinkjavaapi.TestLinkAPI;
import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionStatus;
import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionType;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import br.eti.kinoshita.testlinkjavaapi.util.TestLinkAPIException;

public class TestLinkClient {

	TestLinkAPI api = null;
	testlink.TestLinkAPI api_tcResult = null; // Modified TestLink API to handle string "execution duration" for
	XMLParser parser = null;
	Document parsedXml = null;
	Map<String, String> tcName_Id = new HashMap<String, String>();
	Map<String, String> testFixtureId = new HashMap<String, String>();
	AssemblyDownloader assemblyDownloader = null;

	// Following variables are being read from the system after jenkins stores them
	String testPlanName = null;
	Integer testPlanId = null;
	Integer buildId = null;
	String buildName = null;
	Integer platformId = null;
	String platformName = null;

	TestLinkClient() {
		this.parser = new XMLParser(this); // XML parser to read and write test execution log and testlink importable
											// file
	}

	/*************** update test case result ***************/
	void updateTestCases(Testcases listOfTestCases) {
		if (null != listOfTestCases.getTestcases() && listOfTestCases.getTestcases().size() > 0) {
			for (Testcase tc : listOfTestCases.getTestcases()) {
				Double executionDurationMinutes = tc.getExecutionDuration() / 60.0;
				ExecutionStatus tcStatus = getExecutionStatus(tc.getResult());

				api_tcResult.reportTCResult(tc.getId(), null, testPlanId, tcStatus, null, buildId, buildName, "",
						executionDurationMinutes, true, null, platformId, platformName, null, true,
						tc.getTester(), tc.getTimestamp());
			}
		}
	}

	ExecutionType getExecutionType(String textContent) {
		ExecutionType executionType = null;
		if (textContent.equalsIgnoreCase("A")) {
			executionType = ExecutionType.AUTOMATED;
		} else if (textContent.equalsIgnoreCase("M")) {
			executionType = ExecutionType.MANUAL;
		} else {
			System.out.println("Invalid test execution type!");
		}
		return executionType;
	}

	ExecutionStatus getExecutionStatus(String textContent) {
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
	Document parseXmlResults(String xmlFilePath) {
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
	Integer executeTestScripts() {
		String executableWriteTo = Config.getInstance().getWorkingDirectory() + "\\NunitExecuteTests.bat";

		Process p;
		int exitVal = 0;
		System.out.println(testPlanId);

		byte[] bytearray = Config.getInstance().nunitExecutionCommand.getBytes();
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
		return exitVal;
	}

	/*********** Creates TestLink API Client ************/
	TestLinkAPI getTestLinkApi() {
		try {
			api_tcResult = new testlink.TestLinkAPI(Config.getInstance().getTestlinkURL(), Config.getInstance().devKey);
			api = new TestLinkAPI(Config.getInstance().getTestlinkURL(), Config.getInstance().devKey);
		} catch (TestLinkAPIException te) {
			te.printStackTrace(System.err);
			System.out.println("TestLinkAPIException: " + te.getMessage());
		}
		return api;
	}

	void getTestFixtureAndUpdateExecFilter() {
		TestCase[] executableTestCases = api.getTestCasesForTestPlan(testPlanId, null, null, null, null, null, null,
				null, ExecutionType.AUTOMATED, Boolean.TRUE, null);

		for (int i = 0; i < executableTestCases.length; i++) {
			TestCase tc = executableTestCases[i];

			String fullExternalId = tc.getFullExternalId();
			String testcaseTestlinkId = tc.getId().toString();

			if (null != fullExternalId && null != testcaseTestlinkId
					&& fullExternalId.length() + testcaseTestlinkId.length() > 0) {
				// create test fixture and test case id pairs
				testFixtureId.put(fullExternalId, testcaseTestlinkId);

				// Add test fixtures to nunit execution filter
				if (i > 0) {
					Config.getInstance().nunitExecutionCommand = Config.getInstance().nunitExecutionCommand
							.replaceFirst("--where:\"", "--where:\" tcid == " + fullExternalId + " || ");
				} else {
					Config.getInstance().nunitExecutionCommand = Config.getInstance().nunitExecutionCommand
							.replaceFirst("--where:\"", "--where:\" tcid ==  " + fullExternalId);
				}
			} else {
				System.out.println("Testcase's testlink ID missing!");
			}
		}
	}

	void getTestPlan_BuildId_Platform() {
		testPlanName = System.getenv("TEST_PLAN");
		testPlanId = api.getTestPlanByName(testPlanName, Config.getInstance().getTestProjectName()).getId();
		buildName = System.getenv("BUILD_ID");
		buildId = api.getBuildsForTestPlan(testPlanId)[0].getId();
		platformName = System.getenv("PLATFORM");
		platformId = api.getTestPlanPlatforms(testPlanId)[0].getId();

		System.out.println("Testlink's TestPlan: " + testPlanName);
		System.out.println("Testlink's BuildId: " + buildName);
		System.out.println("Testlink's Platform: " + platformName);
	}

	void addTestAssemblyToExecution() {
		File dir = new File(Config.getInstance().getAssembliesPathOnDisk());

		List<Path> files = null;
		try {
			files = Files.walk(Paths.get(dir.getAbsolutePath())).filter(s -> s.toString().endsWith("Test.dll")).sorted()
					.collect(Collectors.toList());
		} catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}

		for (Path file : files) {
			Config.getInstance().nunitExecutionCommand = Config.getInstance().nunitExecutionCommand.replaceFirst(
					"console.exe",
					"console.exe " + "\"" + file.toAbsolutePath().toString().replace("\\", "\\\\") + "\"");
		}
		System.out.println("NUNIT Execution Command : \n" + Config.getInstance().nunitExecutionCommand);
	}

	void testlinkDevkeyLogin() {
		// TODO: remove this function as devKey will be read only from configuration
		// file, when tool
		// is running on dedicated machine instead of users' machines
		Config.getInstance().devKey = JOptionPane.showInputDialog(null, "Enter your DevKey");
	}

	String getArtifactsUrl() {
		Config.getInstance().assembliesPathOnServer = JOptionPane.showInputDialog(null,
				"Enter artifacts/assemblies URL", Config.getInstance().assembliesPathOnServer);
		return Config.getInstance().assembliesPathOnServer;
	}
}
