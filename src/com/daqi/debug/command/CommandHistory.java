package com.daqi.debug.command;

import com.daqi.debug.ConsoleFactory;
import com.daqi.debug.DDMSUtil;
import com.daqi.debug.VMSessionEventHandler;

public class CommandHistory extends AbsCommand {

	public CommandHistory(DDMSUtil ddmsUtil) {
		super(ddmsUtil);
	}

	@Override
	public String getCommand() {
		return "history";
	}

	@Override
	public void exeCommand(String command) {
		if (mDDMS.getVMSessionTarget().size() == 0) {
			ConsoleFactory.Println("尚未调试连接任何程序，请首先调试连接程序，参考命令：debug或者relaunch命令！");
			return;
		}
		
		String threadName = command.substring(8);
		threadName = threadName.replace("*", "");
		ConsoleFactory.Println("========打印历史线程：" + threadName);
		int count = 0;
		for (VMSessionEventHandler handler : mDDMS.getVMSessionTarget()) {
			count += handler.printThreadInfoHistoryByName(threadName);
		}
		if (count > 0) {
			ConsoleFactory.Println("========共计查找到线程【" + count + "】个");	
		} else {
			ConsoleFactory.Println("========未查询到匹配的线程，请确认线程名称。");
		}		
	}

	@Override
	public void printHelpInfo() {
		ConsoleFactory.Println("\n\t history [线程名称]：输出正在运行的指定线程的首次运行时的历史堆栈信息，线程名支持模糊匹配；");		
	}

	@Override
	public void printHelpDetail() {
		// TODO Auto-generated method stub
		
	}

}
