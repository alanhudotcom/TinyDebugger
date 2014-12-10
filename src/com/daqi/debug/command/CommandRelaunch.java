package com.daqi.debug.command;

import com.daqi.debug.ADBCommands;
import com.daqi.debug.ConsoleFactory;
import com.daqi.debug.DDMSUtil;

public class CommandRelaunch extends AbsCommand {

	public CommandRelaunch(DDMSUtil ddmsUtil) {
		super(ddmsUtil);
	}

	@Override
	public String getCommand() {
		return "relaunch";
	}

	@Override
	public void exeCommand(String command) {
		String args[] = command.split(" ");
		if (args.length == 1) {
			ConsoleFactory.Println("请重新输入参数");
			return;
		}
		String componentName = command.substring(9);
		if (!componentName.contains("/")) {
			ConsoleFactory.Println("relaunch 命令需要完整包名与完整入口Activity名，格式为【完整包名/完整入口Activity名】，请重新输入！");
			return;
		} else if (mDDMS.getVMSessionTarget().size() == 0) {
			ConsoleFactory.Println("relaunch 命令，由于仅通过程序包名与Activity名，不能推断出将要重启的进程名称。" 
							+ "故而在使用之前，您需要先绑定一个程序，如使用debug命令绑定需要重启的进程后，再进行relaunch命令");
			return;
		}
		ADBCommands.reLaunchDebugApp(componentName);		
	}

	@Override
	public void printHelpInfo() {
		ConsoleFactory.Println("\n\t relaunch [完整包名/完整入口Activity名]：重启并联调指定程序；");
	}

	@Override
	public void printHelpDetail() {
		// TODO Auto-generated method stub
		
	}

}
