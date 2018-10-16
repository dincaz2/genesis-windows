package genesis.repair.localization;

import java.util.Scanner;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SuspiciousMethod {

	public final String srcPath, methodName;
	public final double suspiciousness;
	
	public SuspiciousMethod(String srcPath, String methodName, double suspiciousness) {
		this.srcPath = srcPath;
		this.methodName = methodName;
		this.suspiciousness = suspiciousness;
	}
	
	@Override
	public String toString() {
		return srcPath + " " + methodName + " " + suspiciousness;
	}
}
