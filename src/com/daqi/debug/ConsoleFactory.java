package com.daqi.debug;

public class ConsoleFactory {
	
	private static ConsoleFactory sInstance = null;
	
	private static boolean sIsDebug = false;
	
	private ConsoleFactory() {
		
	}
	
	public static ConsoleFactory getInstance() {
		if (sInstance == null) {
			sInstance = new ConsoleFactory();
		}
		return sInstance;
	}
	
	public static void Println(String message) {
		System.out.println(message);
	}
	
	public static void PrintlnForStatus(String message) {
		Println("***********STATUS=" + message);
	}
	
	public static void PrintlnForDebugStatus(String message) {
		if (sIsDebug) {
			PrintlnForStatus(message);
		}
	}

}
