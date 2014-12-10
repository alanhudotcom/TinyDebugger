package com.daqi.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.daqi.debug.command.AbsCommand;
import com.daqi.debug.command.CommandDebug;
import com.daqi.debug.command.CommandHistory;
import com.daqi.debug.command.CommandPause;
import com.daqi.debug.command.CommandPrint;
import com.daqi.debug.command.CommandRelaunch;
import com.daqi.debug.command.CommandResume;
import com.daqi.debug.command.CommandStatus;
import com.daqi.debug.command.CommandStop;

public class ConsoleController {
	
	private DDMSUtil mDDMS = null;
	private ArrayList<AbsCommand> mCommandList = new ArrayList<AbsCommand>(5);
	
	public ConsoleController() {
		mDDMS = new DDMSUtil();
		initCommandSets();
		printHelpInfo();
	}
	
	public void enterControleConsole() {
		
		while (!mDDMS.isReadyWork()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		ConsoleFactory.PrintlnForStatus("=====================设备连接成功，进入控制台，等待输入============================");	
		String command = "";
		while (!command.equals("exit")) {
			InputStreamReader isReader = new InputStreamReader(System.in);
			try {
				command = new BufferedReader(isReader).readLine();
				handleCommand(command);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ConsoleFactory.Println("=====================程序退出============================");
		System.exit(0);
	}
	
	private void initCommandSets() {
		mCommandList.add(new CommandPrint(mDDMS));
		mCommandList.add(new CommandRelaunch(mDDMS));
		mCommandList.add(new CommandHistory(mDDMS));
		mCommandList.add(new CommandResume(mDDMS));
		mCommandList.add(new CommandStatus(mDDMS));
		mCommandList.add(new CommandPause(mDDMS));
		mCommandList.add(new CommandStop(mDDMS));
		mCommandList.add(new CommandDebug(mDDMS));
	}
	
	private void handleCommand(String command) {
		if (command == null) {
			ConsoleFactory.Println("命令不允许为空，请重新输入！");
			return;
		}
		String args[] = command.split(" ");
		String cmdname = args[0];
		if (cmdname != null) {
			for (AbsCommand cmd : mCommandList) {
				if (cmd.getCommand().equals(cmdname)) {
					cmd.exeCommand(command);
					return;
				}
			}
			if (cmdname.equals("help")) {
				printHelpInfo();
				return;
			}
			if (cmdname.equals("exit")) {
				return;
			}
		}
		ConsoleFactory.Println("======不能识别的命令，按help获取帮助信息");
	}
	
	private void printHelpInfo() {
		ConsoleFactory.Println("欢迎进入TinyDebugger，你可以通过如下命令进行控制：");	
		for (AbsCommand command : mCommandList) {
			command.printHelpInfo();
		}
		ConsoleFactory.Println("=================================================");
	}
	
}
