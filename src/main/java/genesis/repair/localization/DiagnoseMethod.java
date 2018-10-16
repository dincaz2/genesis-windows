package genesis.repair.localization;

import java.util.ArrayList;
import java.util.List;

public class DiagnoseMethod {

	public String srcPath, className, methodName;
	public double suspiciousness;
	public List<Integer> lines;

	public DiagnoseMethod(String className, String methodName) {
		this.className = className;
		this.methodName = methodName;
		this.lines = new ArrayList<>();
	}

	@Override
	public String toString() {
		return srcPath + " " + methodName + " " + suspiciousness;
	}
}
