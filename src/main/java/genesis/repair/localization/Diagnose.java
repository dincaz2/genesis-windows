package genesis.repair.localization;

import java.util.List;

public class Diagnose {
	
	public final List<DiagnoseMethod> methods;
	public final double suspiciousness;
	
	public Diagnose(List<DiagnoseMethod> methods, double suspiciousness) {
		this.methods = methods;
		this.suspiciousness = suspiciousness;
	}

}
