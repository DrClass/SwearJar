package io.github.drclass.swearjar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class CodeExecutionEngine {
	
	private String startingCode = "package temp;import java.util.*;import java.math.*;import java.time.*;import java.text.*;public class Executer {public static String execute() {\n";
	private String endingCode = "\n}}";
	
	private String innerCode;
	private File sourceFile;
	private File rootFile;
	
	@SuppressWarnings("unused")
	private CodeExecutionEngine() {	
	}
	
	public CodeExecutionEngine(String code) {
		innerCode = code;
	}
	
	public void initialize() throws IOException {
		String source = startingCode + innerCode + endingCode;
		rootFile = Files.createTempDirectory("java").toFile();
		sourceFile = new File(rootFile, "temp/Executer.java");
		sourceFile.getParentFile().mkdirs();
		Files.write(sourceFile.toPath(), source.getBytes(StandardCharsets.UTF_8));
	}
	
	public List<String> compile() {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		compiler.run(null, out, err, sourceFile.getPath());
		String outString = new String(out.toByteArray());
		String errString = new String(err.toByteArray());
		return List.of(outString, errString);
	}
	
	public String run() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { rootFile.toURI().toURL() });
		Class<?> cls = Class.forName("temp.Executer", true, classLoader);
		Object returnValue = cls.getMethod("execute", null).invoke(null, null);
		return returnValue.toString();
	}
}
