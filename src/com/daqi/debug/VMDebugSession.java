package com.daqi.debug;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.Connector.Argument;


/**
 * 连接目标虚拟机管理类，可支持四种模式的目标虚拟机连接 
 * @author huyong
 *
 */
public class VMDebugSession {
	private int mTryAttachCount = 0;

	public VMDebugSession() {
		//TODO:选定一些参数
	}
	
	public final VirtualMachine getToAttachDebugVM(int port) {
		return attachTarget(String.valueOf(port));
//		return launchTarget(String.valueOf(port));
	}
	
	/**
	 * 绑定到目标虚拟机.
	 */
	private VirtualMachine attachTarget(String mainArgs) {
		mainArgs = mainArgs.trim();
		VirtualMachine vm = null;
		AttachingConnector connector = findAttachingConnector();
		try {
			Map<String, Argument> arguments = connectorArguments(connector, mainArgs);
			vm = connector.attach(arguments);
		} catch (Exception exc) {
			ConsoleFactory.Println("尝试重连设备 " + mTryAttachCount);
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (mTryAttachCount > 30) {
				ConsoleFactory.Println("尝试连接失败，请重新插入USB线或重启调试器试试");	
			} else {
				++mTryAttachCount;
				vm = attachTarget(mainArgs);
			}
		}
		
		return vm;
	}

	/**
	 * 查找到一个绑定式连接器
	 */
	private AttachingConnector findAttachingConnector() {
		List<Connector> connectors = Bootstrap.virtualMachineManager().allConnectors();
		Iterator<Connector> iter = connectors.iterator();
		while (iter.hasNext()) {
			Connector connector = (Connector) iter.next();
			if ("com.sun.jdi.SocketAttach".equals(connector.name())) {
				return (AttachingConnector) connector;
			}
		}
		throw new Error("No launching connector");
	}
	
	/**
	 * 进行连接的参数配置.
	 */
	private Map<String, Argument> connectorArguments(AttachingConnector connector, String port) {
		Map<String, Argument> arguments = connector.defaultArguments();
		Connector.Argument mainArg = (Connector.Argument) arguments.get("port");
		mainArg.setValue(port);

		mainArg = (Connector.Argument) arguments.get("hostname");
		mainArg.setValue("localhost");
		
		mainArg = arguments.get("timeout");
		mainArg.setValue("3000");
		
		return arguments;
	}
	
	
	/**
	 * launch一个虚拟机
	 * @param mainArgs
	 * @return
	 */
	private  VirtualMachine launchTarget(String mainArgs) {
		mainArgs = mainArgs.trim();
		VirtualMachine vm = null;
		LaunchingConnector connector = findLaunchingConnector();
		try {
			Map<String, Argument> arguments = launcherArguments(connector, mainArgs);
			vm = connector.launch(arguments);
		} catch (Exception exc) {
			System.out.println("Try to attache vm again");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			vm = attachTarget(mainArgs);
		}
		
		return vm;
	}
	
	/**
	 * 查找一个启动式连接器
	 * @return
	 */
	private LaunchingConnector findLaunchingConnector() {
		List<Connector> connectors = Bootstrap.virtualMachineManager().allConnectors();
		Iterator<Connector> iter = connectors.iterator();
		while (iter.hasNext()) {
			Connector connector = (Connector) iter.next();
			System.out.println(connector.name() + "/ " + connector.getClass().getName());
			if ("com.sun.jdi.CommandLineLaunch".equals(connector.name())) {
				return (LaunchingConnector) connector;
			}
		}
		throw new Error("No launching connector");
	}
	
	private Map<String, Argument> launcherArguments(LaunchingConnector connector, String mainArgs) {
		Map<String, Argument> arguments = connector.defaultArguments();	
	    Connector.Argument mainArg = (Connector.Argument) arguments.get("main");	
	    if (mainArg == null) {		
	        throw new Error("Bad launching connector");	
	    }
	    mainArg.setValue(mainArgs);	
	    return arguments;
	}
	
}
