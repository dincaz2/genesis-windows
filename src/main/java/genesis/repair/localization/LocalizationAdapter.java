package genesis.repair.localization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import genesis.Config;
import genesis.infrastructure.ExecShellCmd;
import genesis.repair.WorkdirManager;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class LocalizationAdapter implements DefectLocalization {
	
	WorkdirManager manager;

	public LocalizationAdapter(WorkdirManager manager) {
		this.manager = manager;
	}

	@Override
	public List<SuspiciousLocation> getSuspiciousLocations() {
		String diagnosticFile = runDbguer();
		List<Diagnose> diagnoses = parseDiagnosticFile(diagnosticFile);
		suspiciousMethodToLines(diagnoses);
		List<SuspiciousLocation> locs = diagnoseToLocs(diagnoses);
		return locs;
	}
	
	// This is a fix for now. Later this adapter will return List<Diagnose> 
	// to support diagnoses of 2 or more methods
	private List<SuspiciousLocation> diagnoseToLocs(List<Diagnose> diagnoses){
		int col = -1;
		List<SuspiciousLocation> locs = new ArrayList<>();
		for (Diagnose diagnose : diagnoses) {
			if (diagnose.methods.size() != 1)
				continue;
			DiagnoseMethod method = diagnose.methods.get(0);
			for (Integer line : method.lines)
				locs.add(new SuspiciousLocation(method.srcPath, line, col, diagnose.suspiciousness));
		}
		return locs;
	}
	
	public String runDbguer() {
//		String confPath = "conf"; //TODO
//		try {
//			Runtime rt = Runtime.getRuntime();
//			Process pr = rt.exec("python wrapper.py " + confPath + " learn",
//					new String[] {}, new File(Config.dbguerPath));
//			pr.waitFor(); // wait for dBGUer to finish
//		} catch (IOException | InterruptedException e1) {
//			e1.printStackTrace();
//		}
//
//		Properties properties = new Properties();
//		try {
//			properties.load(new FileReader(confPath));
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//		String workingDir = properties.getProperty("workingDir");
//		String dbguerOutput = workingDir + "/output.csv"; // TODO: get real csv output from dbguer
		String dbguerOutput = "diagnosis_sample.txt"; // TODO: get real csv output from dbguer
		return dbguerOutput;
	}
	
	public List<Diagnose> parseDiagnosticFile(String diagnosticFile) {
		List<Diagnose> diagnoses = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(diagnosticFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] args = line.split(" ");
				int numOfMethods = Integer.parseInt(args[0]);
				List<DiagnoseMethod> methods = new ArrayList<>();
				for (int i=1; i <= numOfMethods; i++) {
					String[] arg = args[i].split("@");
					String className = arg[0];
					String methodName = arg[1];
					methods.add(new DiagnoseMethod(className, methodName));
				}
				double suspiciousness = Double.parseDouble(args[numOfMethods + 1]);
				diagnoses.add(new Diagnose(methods, suspiciousness));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Comparator<Diagnose> comp = (diag1,
				diag2) -> (int) (diag1.suspiciousness - diag2.suspiciousness);
		Collections.sort(diagnoses, comp);
		return diagnoses;
	}
	

	public void suspiciousMethodToLines(List<Diagnose> diagnoses) {
		String workDir = manager.getWorkSrcDir();
		ClassPool pool = ClassPool.getDefault();
		try {
			for (String cp : getProjectClasspath(workDir))
				pool.appendClassPath(cp);
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		Map<String, CtClass> classCache = new HashMap<>();
		for (Diagnose diagnose : diagnoses)
			for (DiagnoseMethod method: diagnose.methods)
				suspiciousMethodToLines(workDir, classCache, pool, method);
	}

	public List<SuspiciousLocation> suspiciousMethodToLines(String workDir, Map<String, CtClass> classCache,
			ClassPool pool, DiagnoseMethod method) {
		List<SuspiciousLocation> locs = new ArrayList<>();
		String className = method.className;

		try {
			CtClass cc;
			if (classCache.containsKey(className))
				cc = classCache.get(className);
			else {
				cc = pool.get(className);
				classCache.put(className, cc);
			}
			String src = cc.getURL().getPath();
			if (src.startsWith("/"))
				src = src.substring(1);
			src = src.replaceAll("target[\\\\/]classes", "src/main/java");
			src = src.replace(".class", ".java");
			method.srcPath = src;
			CtMethod methodX = cc.getDeclaredMethod(method.methodName);
			int startLine = methodX.getMethodInfo().getLineNumber(0);
			int endLine = methodX.getMethodInfo().getLineNumber(Integer.MAX_VALUE);

			List<String> lines;
			try {
				lines = Files.readAllLines(Paths.get(src));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			Pattern pattern = Pattern.compile(".*[a-zA-Z].*");

			for (int lineNum = startLine; lineNum <= endLine; lineNum++)
				if (pattern.matcher(lines.get(lineNum - 1)).matches())
					method.lines.add(lineNum);
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		return locs;
	}
	
	private List<String> getProjectClasspath(String workDir) {
		List<String> classpaths = new ArrayList<>();
		Queue<String> dirs = new LinkedList<>();
		dirs.add(workDir);
		while (!dirs.isEmpty()) {
			File dir = new File(dirs.poll());
			File targetDir = new File(dir, "target/classes");
			if (targetDir.isDirectory())
				classpaths.add(targetDir.getPath());
			for (File f : dir.listFiles(file -> file.isDirectory()))
				dirs.add(f.getPath());
		}
		return classpaths;
	}
}
