package com.daqi.debug;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * 日志输出类，可扩展至输出到控制台/本地文件/或上传数据
 * @author huyong
 *
 */
public class LogHelper {
	
	private static PrintWriter sWriter = new PrintWriter(System.out);
	private static PrintWriter sSystemWriter =  new PrintWriter(System.out);
	private static LogHelper sInstanceSelf = new LogHelper(); 
	
	private LogHelper() {
		try {
//			String logPath = "F:\\Code\\Android\\Debug_JDI\\vmthread.log";
			String logPath = "/home/huyong/workspace/work5/SourceCode/Project/TinyDebugger/vmthread.log";
			
			File file = new File(logPath);
			if (file.exists()) {
				file.delete();
			}
			sWriter = new PrintWriter(new FileWriter(file));
		} catch (Exception exc) {
			System.err.println("Cannot open output file: " + " - " + exc);
		}
		
	}
	
	private void printlnImp(String logInfo) {
		sWriter.println(logInfo);
		sSystemWriter.println(logInfo);
	}

	private static LogHelper getInstance() {
		return sInstanceSelf;
	}

	private void flushImp() {
		sWriter.flush();
		sSystemWriter.flush();
	}
	
	public static void println(String logInfo) {
		getInstance().printlnImp(logInfo);
	}
	
	public static void flush() {
		getInstance().flushImp();
	}
	
	

}
