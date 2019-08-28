package testlink;

import java.io.IOException;
import br.eti.kinoshita.testlinkjavaapi.TestLinkAPI;

public class Main {
	static TestLinkClient testlinkClient;
	static AssemblyDownloader assemblyDownloader;

	public static void main(String[] args) throws IOException {
		init();
		assemblyDownloader.downloadAssembliesAndUnzipArchive();

		// Prompt for user's testlink devkey to login
		testlinkClient.testlinkDevkeyLogin();

		// Testlink API client to communicate with Testlink
		testlinkClient.api = getTestLinkApi();

		// Get jenkins build parameters to use them to extract test cases to execute
		getTestPlan_BuildId_Platform();

		/**
		 * Create pairs of test case id and test fixture to be executed by nunit.
		 * Update @Execution_Command with filtered test cases
		 */
		getTestFixtureAndUpdateExecFilter();

		// Get assemblies to execute tests on
		addTestAssemblyToExecution();

		// Execute selected test fixtures and and wait for log file to be created
		executeTestScripts();

		// Build Testlink-importable xml file containing test results obtained from
		// generated log file, that was created by execution of nunit tests
		generateResultsXml();

		// parse importable xml to testcase objects be ready for updating test results
		Testcases testcases = parseXmlResultToTestcases(Config.getInstance().getTestlinkImportableXmlFilePath());

		// Use parsed document to extract test results and update Testlink
		updateTestCases(testcases);

		System.out.println("Testlink - Test cases have been updated successfully");

	}

	private static void updateTestCases(Testcases testcases) {
		testlinkClient.updateTestCases(testcases);
	}

	private static Testcases parseXmlResultToTestcases(String testlinkImportableXmlFilePath) {
		Testcases listOfTestCases = testlinkClient.parser.getTestCasesFromTestReport(testlinkImportableXmlFilePath);
		return listOfTestCases;
	}

	private static void generateResultsXml() {
		testlinkClient.parser.generateResultsXml(testlinkClient.testPlanName, testlinkClient.testPlanId,
				testlinkClient.buildName, testlinkClient.buildId, testlinkClient.platformName,
				testlinkClient.platformId, Config.getInstance().getNunitExecLogFileLogFilePath());
	}

	private static Integer executeTestScripts() {
		Integer executionProcessExitValue = testlinkClient.executeTestScripts();
		if (executionProcessExitValue != 0) {
			System.out.println("Nunit execution failure! -> Process exited with value " + executionProcessExitValue);
		}
		return executionProcessExitValue;
	}

	private static void addTestAssemblyToExecution() {
		testlinkClient.addTestAssemblyToExecution();
	}

	private static void getTestFixtureAndUpdateExecFilter() {
		testlinkClient.getTestFixtureAndUpdateExecFilter();
	}

	private static void getTestPlan_BuildId_Platform() {
		testlinkClient.getTestPlan_BuildId_Platform();
	}

	private static TestLinkAPI getTestLinkApi() {
		return testlinkClient.getTestLinkApi();
	}

	private static void init() {
		Config.getInstance().initializeConfigurationVariables();
		testlinkClient = new TestLinkClient();
		// Prompt for artifacts URL to download assemblies and dependencies
		assemblyDownloader = new AssemblyDownloader(testlinkClient.getArtifacts());
	}

}
