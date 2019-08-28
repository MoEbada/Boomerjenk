package testlink;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "testcase")
@XmlAccessorType(XmlAccessType.FIELD)
public class Testcase {
	@XmlAttribute(name = "id")
	private Integer id;
	private String tester;
	private String result;
	private Double executionDuration;
	private String timestamp;
	
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getTester() {
		return tester;
	}
	
	public void setTester(String tester) {
		this.tester = tester;
	}
	
	public String getResult() {
		return result;
	}
	
	public void setResult(String result) {
		this.result = result;
	}
	
	public Double getExecutionDuration() {
		return executionDuration;
	}
	
	public void setExecutionDuration(Double executionDuration) {
		this.executionDuration = executionDuration;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestap) {
		this.timestamp = timestap;
	}
}
