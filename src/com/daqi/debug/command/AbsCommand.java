package com.daqi.debug.command;

import com.daqi.debug.DDMSUtil;

public abstract class AbsCommand {
	
	protected DDMSUtil mDDMS;
	
	public AbsCommand(DDMSUtil ddmsUtil) {
		mDDMS = ddmsUtil;
	}

	public abstract String getCommand();
	
	public abstract void exeCommand(String command);
	
	public abstract void printHelpInfo();
	
	public abstract void printHelpDetail();

}
