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
		List<SuspiciousMethod> methods = parseDiagnosticFile(diagnosticFile);
		List<SuspiciousLocation> locs = suspiciousMethodToLines(methods);
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
		String dbguerOutput = "prediction_All_methods_sample.csv"; // TODO: get real csv output from dbguer
		return dbguerOutput;
	}
	
	public static List<SuspiciousMethod> parseDiagnosticFile(String diagnosticFile) {
		List<SuspiciousMethod> methods = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(diagnosticFile))) {
			String line;
			boolean first = true;
			while ((line = br.readLine()) != null) {
				if (first) {
					first = false;
					continue;
				}
				int indexDollar = line.indexOf("$");
				int indexComma = line.indexOf(",");
				String src = line.substring(0, indexDollar);
				String methodName = line.substring(indexDollar + 1, indexComma);
				double suspiciousness = Double.parseDouble(line.substring(indexComma + 1));
				methods.add(new SuspiciousMethod(src, methodName, suspiciousness));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return methods;
	}

	public List<SuspiciousLocation> suspiciousMethodToLines(List<SuspiciousMethod> methods) {
		String workDir = manager.getWorkSrcDir();
		List<SuspiciousLocation> ret = new ArrayList<>();
		ClassPool pool = ClassPool.getDefault();
		try {
			for (String cp : getProjectClasspath(workDir))
				pool.appendClassPath(cp);
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		Map<String, CtClass> classCache = new HashMap<>();
		for (SuspiciousMethod method : methods)
			ret.addAll(suspiciousMethodToLines(workDir, classCache, pool, method));
		Comparator<SuspiciousLocation> comp = (loc1,
				loc2) -> (int) (loc1.getSuspiciousness() - loc2.getSuspiciousness());
		Collections.sort(ret, comp);
		return ret;
	}

	public List<SuspiciousLocation> suspiciousMethodToLines(String workDir, Map<String, CtClass> classCache,
			ClassPool pool, SuspiciousMethod method) {
		List<SuspiciousLocation> locs = new ArrayList<>();
		int col = -1;
		String src = pathToClass(workDir, method.srcPath);

		try {
			CtClass cc;
			if (classCache.containsKey(src))
				cc = classCache.get(src);
			else {
				cc = pool.get(src);
				classCache.put(src, cc);
			}
			CtMethod methodX = cc.getDeclaredMethod(method.methodName);
			int startLine = methodX.getMethodInfo().getLineNumber(0);
			int endLine = methodX.getMethodInfo().getLineNumber(Integer.MAX_VALUE);

			List<String> lines;
			try {
				lines = Files.readAllLines(Paths.get(workDir + "/" + method.srcPath));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			Pattern pattern = Pattern.compile(".*[a-zA-Z].*");

			for (int lineNum = startLine; lineNum <= endLine; lineNum++)
				if (pattern.matcher(lines.get(lineNum - 1)).matches())
					locs.add(new SuspiciousLocation(method.srcPath, lineNum, col, method.suspiciousness));
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		return locs;
	}
	
	private String pathToClass(String workDir, String path) {
		String[] comps = path.split("[\\\\/]");
		String className = comps[comps.length - 1];
		className = className.substring(0, className.indexOf("."));
		path = workDir + '/' + path;
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			Pattern pattern = Pattern.compile("[ \\t]*package (?<package>[\\.\\w\\d]+);[ \\t]*");
			while ((line = br.readLine()) != null) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.matches())
					return matcher.group("package") + "." + className;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return className;
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
