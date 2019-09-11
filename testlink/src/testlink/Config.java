package testlink;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class Config {

	private String workingDirectory;
	String devKey;
	private URL testlinkURL;
	private String testlinkImportableXmlFilePath;
	private String testProjectName;
	private String nunitExecLogFileLogFilePath;
	String nunitExecutionCommand;
	String assembliesPathOnServer;
	private String assembliesPathOnDisk;
	
	private static Config instance = null;
	private static final String PROPERTIES_FILENAME = "config.properties";

	private Config() {
	}

	public static Config getInstance() {
		if (instance == null) {
			try {
				instance = new Config();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return instance;
	}
	
	void initializeConfigurationVariables() {
		this.workingDirectory = System.getProperty("user.dir");
		Properties prop = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(PROPERTIES_FILENAME);
			prop.load(in);
			in.close();
		} catch (IOException e1) {
			System.out.println("IOException: " + e1.getMessage());
			e1.printStackTrace();
		}

		prop.setProperty("assembliesPathOnDisk", workingDirectory + "\\assembly");
		prop.setProperty("testlinkImportableXmlFilePath", workingDirectory + "\\testlink_testresult.xml");
		prop.setProperty("nunitExecLogFileLogFilePath", workingDirectory + "\\TestResult.xml");
		
		devKey = prop.getProperty("devKey");
		testlinkImportableXmlFilePath = prop.getProperty("testlinkImportableXmlFilePath");
		testProjectName = prop.getProperty("testProjectName");
		nunitExecLogFileLogFilePath = prop.getProperty("nunitExecLogFileLogFilePath");
		nunitExecutionCommand = prop.getProperty("nunitExecutionCommand");
		assembliesPathOnServer = prop.getProperty("defaultAssembliesPathOnServer");
		assembliesPathOnDisk = prop.getProperty("assembliesPathOnDisk");
		
		try {
			testlinkURL = new URL(prop.getProperty("testlinkUrl"));
		} catch (MalformedURLException e) {
			System.out.println("MalformedURLException: " + e.getMessage());
			e.printStackTrace();
		}
	}

//	public static String getDevKey() {
//		return devKey;
//	}

	public URL getTestlinkURL() {
		return testlinkURL;
	}

	public String getTestlinkImportableXmlFilePath() {
		return testlinkImportableXmlFilePath;
	}

	public String getTestProjectName() {
		return testProjectName;
	}

	public String getNunitExecLogFileLogFilePath() {
		return nunitExecLogFileLogFilePath;
	}

	public String getAssembliesPathOnDisk() {
		return assembliesPathOnDisk;
	}
	
	public String getWorkingDirectory() {
		return workingDirectory;
	}
}
