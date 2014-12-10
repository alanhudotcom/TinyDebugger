package com.daqi.debug.command;

import com.daqi.debug.ConsoleFactory;
import com.daqi.debug.DDMSUtil;
import com.daqi.debug.VMSessionEventHandler;

public class CommandPrint extends AbsCommand{
	
	public CommandPrint(DDMSUtil ddms) {
		super(ddms);
	}
	
	@Override
	public String getCommand() {
		return "print";
	}
		
	@Override
	public void exeCommand(String command) {
	
		String args[] = command.split(" ");
	
		if (mDDMS.getVMSessionTarget().size() == 0) {
			ConsoleFactory.Println("尚未调试连接任何程序，请首先调试连接程序，参考命令：debug命令！");
			return;
		}
		
		if (args.length == 1 || args[1].equals("all")) {
			for (VMSessionEventHandler handler : mDDMS.getVMSessionTarget()) {
				handler.printInfoForCurProcessVM();
			}
			
		} else if (args[1].equals("new")) {
			int count = 0;
			for (VMSessionEventHandler handler : mDDMS.getVMSessionTarget()) {
				count += handler.printNewAddedThreadInfo();
			}
			if (count == 0) {
				ConsoleFactory.Println("========未有新增线程。");
			} else {
				ConsoleFactory.Println("========新增线程信息打印结束。");
			}
			
		} else {
			String threadName = command.substring(6);
			threadName = threadName.replace("*", "");
			ConsoleFactory.Println("========打印线程：" + threadName);
			int count = 0;
			for (VMSessionEventHandler handler : mDDMS.getVMSessionTarget()) {
				count += handler.printThreadInfoByName(threadName);
			}
			if (count > 0) {
				ConsoleFactory.Println("========共计查找到线程【" + count + "】个");	
			} else {
				ConsoleFactory.Println("========未查询到匹配的线程，请确认线程名称。");
			}
		}
			
	}
	
	@Override
	public void printHelpInfo() {
		ConsoleFactory.Println("\n\t print [all][new][线程名称]：打印指定线程当前堆栈信息；");
	}
	
	@Override
	public void printHelpDetail() {
		ConsoleFactory.Println("print 命令用于输出打印指定线程当前堆栈信息，支持三种参数[all] [new] [线程名称]：");
		ConsoleFactory.Println("\n\t all ：用于输出整个虚拟机中所有线程信息");
		ConsoleFactory.Println("\n\t new ：用于输出从初始联调状态至今，虚拟机中现存的新增线程信息");
		ConsoleFactory.Println("\n\t 线程名称 ：用于输出指定线程堆栈信息，线程名称支持模糊匹配");
	}

}
