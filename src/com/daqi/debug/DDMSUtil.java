/**
 * Tencent is pleased to support the open source community by making APT available.
 * Copyright (C) 2014 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */


package com.daqi.debug;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.daqi.debug.AdbDeviceListener.IDeviceChangedListener;
import com.sun.jdi.VirtualMachine;


/**
* @Description  dump相关的工具类
* @date 2013年11月10日 下午5:06:20 
*
 */
public class DDMSUtil {
	
	private static AdbDeviceListener mDeviceListener;
	private static AdbClientListener mClientListener = null;
//	private static AdbHProfDumpListener hprofListener = null;
	
	private static String ADB_PATH = "/usr/android-sdks/platform-tools/adb";
	
	private static int sRetryConnectClient = 0;
	
	private ConcurrentHashMap<String, VMSessionEventHandler> mVMDebugSession = new ConcurrentHashMap<String, VMSessionEventHandler>(2);
	
	
	public DDMSUtil() {
		init();
	}
	
	/**
	* @Description 初始化ddmlib 
	* @param    
	* @return void 
	* @throws
	 */
	private void init() {
//			hprofListener = new AdbHProfDumpListener();
//			ClientData.setHprofDumpHandler(hprofListener);
		
		AndroidDebugBridge.init(true);
//			AndroidDebugBridge.createBridge(ADB_PATH, false);
		
		initDeviceChangedMonitor();
		initClientChangedMonitor();

		AndroidDebugBridge.createBridge();
	}
	
	
	public boolean isReadyWork() {
		return mDeviceListener.getCurDevice() != null;
	}
	
	public void printDeviceStatus() {
		printDebugableAppInfo();
	}
	
	
	private void initDeviceChangedMonitor() {
		mDeviceListener = new AdbDeviceListener();
		mDeviceListener.setDeviceChangedListener(new IDeviceChangedListener() {
			@Override
			public void notifyDeviceChanged(String appName) {
				rebindDebugApp(appName, 0);
			}
		});
		AndroidDebugBridge.addDeviceChangeListener(mDeviceListener);
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				while (true) {
					try {
						synchronized (AdbDeviceListener.sLock) {
							AdbDeviceListener.sLock.wait();
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (mDeviceListener.getCurDevice() != null) {
						if (mVMDebugSession.size() > 0) {
							ConsoleFactory.PrintlnForStatus("=============设备已连接，准备重新连接");
							resetVMDebugApp();
						} else {
							Client[] clientList = mDeviceListener.getCurDevice().getClients();
							if (clientList != null && clientList.length > 0) {
								printDebugableAppInfo();
							} else {
								int count = 0;
								int printCount = 0;
								while (++count < 10 && printCount == 0) {
									printCount = printDebugableAppInfo();
									waitCurThreadForOneSecond();
								}
							}
						}
						
					} else {
						// 设备已经被拔掉，则等待
						ConsoleFactory.PrintlnForStatus("=============设备已断开");
					}
				}
			}
		}, "Device-Changed-Monitor").start();
	}
	
	
	private void initClientChangedMonitor() {
		mClientListener = new AdbClientListener();
		AndroidDebugBridge.addClientChangeListener(mClientListener);
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					try {
						synchronized (AdbClientListener.sLock) {
							AdbClientListener.sLock.wait();
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					int port = mClientListener.getChangedClient().getDebuggerListenPort();
					String appName = mClientListener.getChangedClient().getClientData().getClientDescription();
					ConsoleFactory.PrintlnForStatus("=============程序" + appName + "端口号变为：" + port);
					
					rebindDebugApp(appName, port);
				}
			}
		}, "Client-Changed-Monitor").start();;
		
	}
	
	private int printDebugableAppInfo() {
		Client[] clientList = mDeviceListener.getCurDevice().getClients();
		if (clientList != null && clientList.length > 0) {
			ConsoleFactory.PrintlnForStatus("=============当前设备已连接，可调试程序信息如下：");
			for (Client client : clientList) {
				ConsoleFactory.PrintlnForStatus("====进程名称：" + client.getClientData().getClientDescription() 
						+ ", pid：" + client.getClientData().getPid());
			}
		}
		return clientList.length;
	}
	
	/**
	 * 首次进入，调试程序
	 * @param appName：待调试程序的进程名称
	 */
	public void debugApp(String appName) {
		if (mVMDebugSession != null) {
			VMSessionEventHandler handler = mVMDebugSession.get(appName);
			if (handler != null && handler.isVMRunningOK()) {
				ConsoleFactory.Println("=======" + appName + "已处于调试状态");
				return;
			}
		}
		
		mDeviceListener.addTargetAppName(appName);
		mClientListener.addTargetAppName(appName);
		
		VMSessionEventHandler vmSessionHandler = bindDebugApp(appName, 0);
		if (vmSessionHandler == null) {
			ConsoleFactory.Println("=====绑定失败，请稍后重试");
			return;
		}
		mVMDebugSession.put(appName, vmSessionHandler);
	}
	
	public void pauseVM() {
		Collection<VMSessionEventHandler> allVM = mVMDebugSession.values();
		for (VMSessionEventHandler vmSessionEventHandler : allVM) {
			vmSessionEventHandler.pauseVM();
		}
	}
	
	public void resumeVM() {
		Collection<VMSessionEventHandler> allVM = mVMDebugSession.values();
		for (VMSessionEventHandler vmSessionEventHandler : allVM) {
			vmSessionEventHandler.resumeVM();
		}
	}
	
	public Collection<VMSessionEventHandler> getVMSessionTarget() {
		return mVMDebugSession.values();
	}
	
	/**
	 * 程序重启或其他原因，导致程序端口号发生变化的，重新绑定程序。
	 * @param appName
	 * @param port
	 */
	private void rebindDebugApp(String appName, int port) {
		if (!mVMDebugSession.containsKey(appName)) {
			return;
		}
		VMSessionEventHandler vmSessionHandler = mVMDebugSession.get(appName);
		if (vmSessionHandler.isVMRunningOK()) {
			//当前虚拟机运行良好，则不做重新绑定处理
			return;
		}
		if (vmSessionHandler != null) {
			vmSessionHandler.destroy();
		}
		
		vmSessionHandler = bindDebugApp(appName, port);
		if (vmSessionHandler == null) {
			ConsoleFactory.Println("=====重新绑定失败，请稍后重试");
			return;
		}
		mVMDebugSession.put(appName, vmSessionHandler);
	}
	
	private void resetVMDebugApp() {
		Iterator<String> appNameIter = mVMDebugSession.keySet().iterator(); 
		while (appNameIter.hasNext()) {
			String appName = appNameIter.next();
			rebindDebugApp(appName, 0);
		}
	}
	
	private VMSessionEventHandler bindDebugApp(String appName, int port) {
		if (port == 0) {
			port = getClientPort(appName); 
		}
		
		VMSessionEventHandler vmEventHandler = null;
		try {
			VirtualMachine vm = new VMDebugSession().getToAttachDebugVM(port);
			if (vm == null) {
				return vmEventHandler;
			}
			vm.setDebugTraceMode(VirtualMachine.TRACE_NONE);
			vm.resume();
			vmEventHandler = new VMSessionEventHandler(vm);
			vmEventHandler.startEventLoop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return vmEventHandler;
	}
		 
	private boolean checkAndWaitToConnetcDevice() {
		synchronized (AdbDeviceListener.sLock) {
			if (mDeviceListener.getCurDevice() == null) {
				ConsoleFactory.PrintlnForStatus("等待设备连接");
				try {
					AdbDeviceListener.sLock.wait(5000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
					ConsoleFactory.Println(e.getMessage());
				}
			}
		}
		
		if (mDeviceListener.getCurDevice() == null) {
			ConsoleFactory.PrintlnForStatus("当前没有设备连接, 请重连");
		}
		
		return mDeviceListener.getCurDevice() != null; 
	}
	
	private static void waitCurThreadForOneSecond() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public int getClientPort(String appName) {
		int count = 0;
		while (!checkAndWaitToConnetcDevice() || mDeviceListener.getCurDevice().getClients().length == 0) {
			AndroidDebugBridge.createBridge();
			waitCurThreadForOneSecond();
			++count;
			ConsoleFactory.PrintlnForStatus("===== wait for = " + count + " s");
		}
		
		++sRetryConnectClient;
		ConsoleFactory.PrintlnForStatus("=========进行第" + sRetryConnectClient + "次尝试连接应用进程：" + appName);
		if (sRetryConnectClient > 12) {
			ConsoleFactory.PrintlnForStatus("=========已尝试连接" + sRetryConnectClient + "次不成功，请重新检查接口或是否已启动DDMS进行连接...");
			return 0;
		}
		
		Client targetClient = mDeviceListener.getCurDevice().getClient(appName);
		int resultPort = 0;
		if (targetClient == null) {
			AndroidDebugBridge.createBridge();
			waitCurThreadForOneSecond();
			//重启后重新来一遍
			resultPort = getClientPort(appName);
			--sRetryConnectClient;
		} else {
			resultPort = targetClient.getDebuggerListenPort();
		}
		if (sRetryConnectClient <= 1) {
			ConsoleFactory.PrintlnForStatus("=========端口连接成功，调试端口号为" + resultPort);
		}
		return resultPort;
	}
	
	/**
	* @Description 触发GC，并获取一次内存数据 
	* @param @param pkgName
	* @param @return   
	* @return boolean 
	* @throws
	 */
	public static boolean gc(String pkgName) {
		if (mDeviceListener.getCurDevice() == null) {
			return false;
		}
		Client targetClient = mDeviceListener.getCurDevice().getClient(pkgName);

		if (targetClient == null) {
			ConsoleFactory.Println("进程连接失败:pkgName=" + pkgName);
			ConsoleFactory.Println("1.首先保证系统或者被测应用是可调试的");
			ConsoleFactory.Println("2.其次保证APT先于DDMS启动（打开APT透视图，重启eclipse即可）");
			return false;
		}

		targetClient.executeGarbageCollector();
		ConsoleFactory.Println("GC Done");
		return true;
	}
	
	/**
	* @Description 执行dump操作 
	* @param  pkgName执行dump操作对应的进程
	* @return boolean dump操作是否成功
	* @throws
	 */
	public static boolean dump(String pkgName) {
		if ( mDeviceListener.getCurDevice() == null ) {
			return false;
		}

		Client targetClient = mDeviceListener.getCurDevice().getClient(pkgName);

		if (targetClient == null) {
			ConsoleFactory.PrintlnForStatus("进程连接失败:pkgName=" + pkgName);
			ConsoleFactory.Println("1.首先保证系统或者被测应用是可调试的");
			ConsoleFactory.Println("2.其次保证APT先于DDMS启动（打开APT透视图，重启eclipse即可）");
			return false;
		}

		targetClient.dumpHprof();
		return true;
	}
}
