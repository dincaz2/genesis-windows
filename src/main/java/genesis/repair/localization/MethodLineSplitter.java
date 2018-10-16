package genesis.repair.localization;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class MethodLineSplitter {
	
	

	public static List<SuspiciousLocation> run(String workDir, String diagnosticFile, String classPath) {
		List<SuspiciousMethod> methods = parseDiagnosticFile(workDir, diagnosticFile);
		List<SuspiciousLocation> locs = suspiciousMethodToLines(workDir, methods, classPath);
		return locs;
	}

	public static List<SuspiciousMethod> parseDiagnosticFile(String workDir, String diagnosticFile) {
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

	private static String pathToClass(String workDir, String path) {
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

	public static List<SuspiciousLocation> suspiciousMethodToLines(String workDir, List<SuspiciousMethod> methods,
			String classPath) {
		List<SuspiciousLocation> ret = new ArrayList<>();
		ClassPool pool = ClassPool.getDefault();
		try {
			for (String cp : classPath.split(";"))
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

	public static List<SuspiciousLocation> suspiciousMethodToLines(String workDir, Map<String, CtClass> classCache,
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

}
