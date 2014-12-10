package com.daqi.debug.command;

import com.daqi.debug.ConsoleFactory;
import com.daqi.debug.DDMSUtil;
import com.daqi.debug.VMSessionEventHandler;

public class CommandPause extends AbsCommand {

	public CommandPause(DDMSUtil ddms) {
		super(ddms);
	}
	
	@Override
	public String getCommand() {
		return "pause";
	}

	@Override
	public void exeCommand(String command) {
		if (mDDMS.getVMSessionTarget().size() == 0) {
			ConsoleFactory.Println("尚未调试连接任何程序，请首先调试连接程序，参考命令：debug或者relaunch命令！");
			return;
		}
		String args[] = command.split(" ");
		if (args.length == 1) {
			ConsoleFactory.Println("请重新输入参数");
			return;
		}
		String threadName = command.substring(6);
		if (threadName.equals("all")) {
			mDDMS.pauseVM();
			ConsoleFactory.Println("进程已暂停执行");
			
		} else if (threadName.equals("help")) {
			printHelpDetail();
			
		} else {
			ConsoleFactory.Println("暂停线程：" + threadName);
			threadName = threadName.replace("*", "");
			for (VMSessionEventHandler handler : mDDMS.getVMSessionTarget()) {
				handler.pauseThreadByName(threadName);
			}		
		}
		
	}

	@Override
	public void printHelpInfo() {
		ConsoleFactory.Println("\n\t pause [all][线程名称]：暂停并打印指定线程当前堆栈信息，与resume命令相对；");
	}

	@Override
	public void printHelpDetail() {
		ConsoleFactory.Println("pause 命令用于暂停并打印指定线程当前堆栈信息，支持两个参数[all] [线程名称]：");
		ConsoleFactory.Println("\n\t all :用于暂停整个虚拟机执行");
		ConsoleFactory.Println("\n\t 线程名称 :用于暂停指定线程，线程名支持模糊匹配");
	}

}
