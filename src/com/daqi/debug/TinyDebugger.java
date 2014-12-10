package com.daqi.debug;


public class TinyDebugger {
	public static void main(String[] args) {
		ADBCommands.startADBServer();
		new TinyDebugger();
	}
	
	private TinyDebugger() {
		ConsoleController console = new ConsoleController();
		console.enterControleConsole();
	}

}
