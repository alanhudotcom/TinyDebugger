package com.daqi.debug.command;

import com.daqi.debug.ConsoleFactory;
import com.daqi.debug.DDMSUtil;

public class CommandDebug extends AbsCommand {

	public CommandDebug(DDMSUtil ddms) {
		super(ddms);
	}
	
	@Override
	public String getCommand() {
		return "debug";
	}

	@Override
	public void exeCommand(String command) {
		String args[] = command.split(" ");
		if (args.length == 1) {
			ConsoleFactory.Println("请重新输入参数");
			return;
		}
		String appName = command.substring(6);
		mDDMS.debugApp(appName);
	}

	@Override
	public void printHelpInfo() {
		ConsoleFactory.Println("\n\t debug [应用进程名称]：联调正在运行的指定应用进程；");
	}

	@Override
	public void printHelpDetail() {
		// TODO Auto-generated method stub
		
	}

}
