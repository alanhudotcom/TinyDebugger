package com.daqi.debug.command;

import com.daqi.debug.ConsoleFactory;
import com.daqi.debug.DDMSUtil;

public class CommandStatus extends AbsCommand {

	public CommandStatus(DDMSUtil ddmsUtil) {
		super(ddmsUtil);
	}

	@Override
	public String getCommand() {
		return "status";
	}

	@Override
	public void exeCommand(String command) {
		mDDMS.printDeviceStatus();		
	}

	@Override
	public void printHelpInfo() {
		ConsoleFactory.Println("\n\t status ：输出当前连接设备状态；");
		
	}

	@Override
	public void printHelpDetail() {
		// TODO Auto-generated method stub
		
	}

}
