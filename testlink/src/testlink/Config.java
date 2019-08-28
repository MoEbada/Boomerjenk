package testlink;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class Config {

	String devKey;
	private URL testlinkURL;
	private String testlinkImportableXmlFilePath;
	private String testProjectName;
	private String nunitExecLogFileLogFilePath;
	String nunitExecutionCommand;
	private String testfixtureCustomfieldName;
	private Integer testProjectId;
	String assembliesPathOnServer;
	private String assembliesPathOnDisk;
	private String assembliesUriOnDisk;
	
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
		devKey = prop.getProperty("devKey");
		testlinkImportableXmlFilePath = prop.getProperty("testlinkImportableXmlFilePath");
		testProjectName = prop.getProperty("testProjectName");
		nunitExecLogFileLogFilePath = prop.getProperty("nunitExecLogFileLogFilePath");
		nunitExecutionCommand = prop.getProperty("nunitExecutionCommand");
		testfixtureCustomfieldName = prop.getProperty("testfixtureCustomfieldName");
		testProjectId = new Integer(prop.getProperty("testProjectId"));
		assembliesPathOnServer = prop.getProperty("defaultAssembliesPathOnServer");
		assembliesPathOnDisk = prop.getProperty("assembliesPathOnDisk");
		assembliesUriOnDisk = prop.getProperty("assembliesUriOnDisk");
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

	public String getTestfixtureCustomfieldName() {
		return testfixtureCustomfieldName;
	}

	public int getTestProjectId() {
		return testProjectId;
	}

	public String getAssembliesPathOnDisk() {
		return assembliesPathOnDisk;
	}

	public String getAssembliesUriOnDisk() {
		return assembliesUriOnDisk;
	}
}
