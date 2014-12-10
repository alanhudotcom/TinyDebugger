package com.daqi.debug.command;

import com.daqi.debug.ADBCommands;
import com.daqi.debug.ConsoleFactory;
import com.daqi.debug.DDMSUtil;

public class CommandStop extends AbsCommand {

	public CommandStop(DDMSUtil ddms) {
		super(ddms);
	}
	
	@Override
	public String getCommand() {
		return "stop";
	}

	@Override
	public void exeCommand(String command) {
		String args[] = command.split(" ");
		if (args.length == 1) {
			ConsoleFactory.Println("请重新输入参数");
			return;
		}
		String pkgName = command.substring(5);
		ADBCommands.stopApp(pkgName);
	}

	@Override
	public void printHelpInfo() {
		ConsoleFactory.Println("\n\t stop [进程的包名] ：强行停止指定包名的应用；");
	}

	@Override
	public void printHelpDetail() {
		// TODO Auto-generated method stub
		
	}

}
