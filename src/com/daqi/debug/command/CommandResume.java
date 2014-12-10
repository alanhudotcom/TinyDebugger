package com.daqi.debug.command;

import com.daqi.debug.ConsoleFactory;
import com.daqi.debug.DDMSUtil;

public class CommandResume extends AbsCommand {

	public CommandResume(DDMSUtil ddmsUtil) {
		super(ddmsUtil);
	}

	@Override
	public String getCommand() {
		return "resume";
	}

	@Override
	public void exeCommand(String command) {
		mDDMS.resumeVM();
	}

	@Override
	public void printHelpInfo() {
		ConsoleFactory.Println("\n\t resume ：恢复被暂停的虚拟机，与pause命令相对应；");		
	}

	@Override
	public void printHelpDetail() {
		// TODO Auto-generated method stub
		
	}

}
