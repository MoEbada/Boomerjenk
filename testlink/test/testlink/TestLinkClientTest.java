package testlink;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JOptionPane;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Mockito.*;
import org.apache.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import static org.mockito.ArgumentMatchers.*;

import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionStatus;
import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionType;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;

public class TestLinkClientTest {

	TestLinkClient testLinkClient = new TestLinkClient();
	br.eti.kinoshita.testlinkjavaapi.TestLinkAPI ApiMock = Mockito.mock(br.eti.kinoshita.testlinkjavaapi.TestLinkAPI.class);
	
	static Testcase tc1 = new Testcase();
	static Testcase tc2 = new Testcase();
	static Testcase tc3 = new Testcase();
	
	static TestCase testLinkTestcase1 = new TestCase();
	static TestCase testLinkTestcase2 = new TestCase();
	static TestCase testLinkTestcase3 = new TestCase();
	static TestCase[] testlinkTestcases = new TestCase[] {testLinkTestcase1, testLinkTestcase2, testLinkTestcase3};
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		init();
	}

	private static void init() {
		initTestcases(tc1, tc2, tc3);
		initTestLinkTestcases(testlinkTestcases);
		Config.getInstance().initializeConfigurationVariables();
	}

	private static void initTestLinkTestcases(TestCase[] testlinkTestcases) {
		for(int i =0; i< testlinkTestcases.length; i++) {
			testlinkTestcases[i].setId(i+11);
			testlinkTestcases[i].setFullExternalId("TL-" + (i + 1));
			testlinkTestcases[i].setExecutionStatus(ExecutionStatus.NOT_RUN);
		}
	}                                                                          

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	private static void initTestcases(Testcase tc1, Testcase tc2, Testcase tc3) {
		tc1.setId(1);
		tc1.setResult("knasoi23213");
		tc1.setTester("tester1");
		tc1.setTimestamp("2019-09-02 08:46:24");
		tc1.setExecutionDuration(15.0);
		
		tc2.setId(2);
		tc2.setResult("n");
		tc2.setTester("tester1");
		tc2.setTimestamp("2019-09-02 08:46:24");
		tc2.setExecutionDuration(1000000.0);
		
		tc3.setId(3);
		tc3.setResult("");
		tc3.setTester("tester1");
		tc3.setTimestamp("2019-09-02 08:46:24");
		tc3.setExecutionDuration(15.0);
	}

	@Test
	public void testGetExecutionType_Invalid() {
		String execType = "1";
		ExecutionType response = null;
		try {
			response = Whitebox.invokeMethod(testLinkClient, "getExecutionType", execType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(null, response);
	}

	@Test
	public void testGetExecutionType_Valid() {
		String execType = "a";
		ExecutionType response = null;
		try {
			response = Whitebox.invokeMethod(testLinkClient, "getExecutionType", execType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(ExecutionType.AUTOMATED, response);
	}
	
	@Test
	public void testGetExecutionStatus_Invalid() {
		initTestcases(tc1, tc2, tc3);
		String result = "ab67^*%G^G";
		ExecutionStatus response = null;
		try {
			response = Whitebox.invokeMethod(testLinkClient, "getExecutionStatus", result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(null, response);
	}
	
	@Test
	public void testGetExecutionStatus_Valid() {
		initTestcases(tc1, tc2, tc3);
		String result = "b";
		ExecutionStatus response = null;
		try {
			response = Whitebox.invokeMethod(testLinkClient, "getExecutionStatus", result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		assertEquals(ExecutionStatus.BLOCKED, response);
	}

	@Test
	public void testGetTestFixtureAndUpdateExecFilter() throws Exception {
		testLinkClient.api = ApiMock;
		Mockito.when(ApiMock.getTestCasesForTestPlan(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(testlinkTestcases);
		Whitebox.invokeMethod(testLinkClient, "getTestFixtureAndUpdateExecFilter");
		
		Map<String, String> testFixtureId = testLinkClient.testFixtureId;
		for(int i=0; i<testlinkTestcases.length; i++) {
			assertTrue(testFixtureId.get(testlinkTestcases[i].getFullExternalId()).equals(testlinkTestcases[i].getId().toString()));
			assertTrue(Config.getInstance().nunitExecutionCommand.contains(testlinkTestcases[i].getFullExternalId()));
		}
	}
}
