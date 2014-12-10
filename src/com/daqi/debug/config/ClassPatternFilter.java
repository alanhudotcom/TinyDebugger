package com.daqi.debug.config;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;


public class ClassPatternFilter {
	private static String SEPARATOR = ";";
	private static String EXCLUDED_CLASS_PATTERN = "ExcludedClassPattern";
	private static String[] excludes =new String[0];
	static
	{
		Properties configPro = new Properties();
		try {
//			"/" + ExceptionFilter.class.getResource("").getPath().substring(1)+"ClassExcludeConfig.properties";
//			String path = "F:\\Code\\Android\\Debug_JDI\\raw\\ClassExcludeConfig.properties";
			String path = "/home/huyong/workspace/work5/SourceCode/Project/TinyDebugger/raw/ClassExcludeConfig.properties";
			configPro.load(new FileInputStream(path));
			String excludeString = (String) configPro.get(EXCLUDED_CLASS_PATTERN);
			excludes = excludeString.split(SEPARATOR);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String[] getExcludes()
	{
		return excludes;
	}
}
