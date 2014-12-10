package com.daqi.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ADBCommands {
	
	public static void startADBServer() {
		String startServer = "adb start-server";
		exeCmd(startServer);
	}
	
	public static void stopApp(String pkgName) {
		String stopApp = "adb shell am force-stop " + pkgName;
		exeCmd(stopApp);
	}
	
	public static void debugLaunchApp(String componentName) {
//		String cmd="adb -d shell am start -e debug true -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.thirdwhale/com.thirdwhale.activity.SplashActivity";
		String launchApp = "adb -d shell am start -e debug true"
				+ " -a android.intent.action.MAIN"
				+ " -c android.intent.category.LAUNCHER"
				+ " -n " + componentName;
		exeCmd(launchApp);
	}

	public static void reLaunchDebugApp(String componentName) {
		String pkgName = componentName.substring(0, componentName.indexOf("/"));
		stopApp(pkgName);
		debugLaunchApp(componentName);
	}
	
	private static void exeCmd(String command) {
		try {
			Process adb = Runtime.getRuntime().exec(command);
			BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(adb.getInputStream()));
	
			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				System.out.println(str);
			}
			adb.waitFor();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
