/**
 * Tencent is pleased to support the open source community by making APT available.
 * Copyright (C) 2014 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */


package com.daqi.debug;

import java.util.ArrayList;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;

/**
* @Description 
* @date 2013年11月10日 下午5:03:48 
*
 */
public class AdbDeviceListener implements AndroidDebugBridge.IDeviceChangeListener{

	//静态锁，相等于类锁
	//这里可以保证，所有该类的对象中的所有方法都是同步执行的
	public static final Object sLock = new Object();
	private IDevice mCurrentDevice = null;
	private ArrayList<String> mDebugAppList = new ArrayList<String>(2);
	private ArrayList<String> mChangedAppList = new ArrayList<String>(2);
	private IDeviceChangedListener mDeviceChangedListener;
	
	@Override
	public void deviceChanged(IDevice device, int changeMask) {
		ConsoleFactory.PrintlnForDebugStatus("Device Changed - " + changeMask);
		if (changeMask == IDevice.CHANGE_CLIENT_LIST && false) {
			//可测试列表发生变化，故而需要检验本次是否有待调试程序出现问题
			if (mChangedAppList != null && mChangedAppList.size() > 0) {
				for (Client client : device.getClients()) {
					String appName = client.getClientData().getClientDescription();
					boolean needToNotify = mChangedAppList.remove(appName);
					if (needToNotify) {
						ConsoleFactory.PrintlnForStatus("=====deviceChanged========mDeviceChangedListener.notifyDeviceChanged " + appName);
						mDeviceChangedListener.notifyDeviceChanged(appName);
					}
				}
				
			}
			// 找出变化的接口出来。
			ArrayList<String> toDebuggedAppList = (ArrayList<String>)mDebugAppList.clone();
			for (Client client : device.getClients()) {
				String appName = client.getClientData().getClientDescription();
				ConsoleFactory.PrintlnForStatus("=====deviceChanged========toDebuggedAppList.size = " + toDebuggedAppList.size() + ", appName=" + appName);
				toDebuggedAppList.remove(appName);
			}
			
			// 若toDebuggedAppList不为空，则说明有减少，需要在下次进行处理
			if (toDebuggedAppList.size() > 0) {
				mChangedAppList = toDebuggedAppList;
				ConsoleFactory.PrintlnForStatus("=====deviceChanged========mChangedAppList.size = " + mChangedAppList.size());
			}
		}
	}

	@Override
	public void deviceConnected(IDevice device) {
		synchronized (sLock) {
			this.mCurrentDevice = device;
			sLock.notify();
		}
	}

	@Override
	public void deviceDisconnected(IDevice device) {
		synchronized (sLock) {
			this.mCurrentDevice = null;
			sLock.notify();
		}
	}
	
	public IDevice getCurDevice() {
		return mCurrentDevice;
	}
	
	public void addTargetAppName(String appName) {
		mDebugAppList.add(appName);
	}
	
	public void setDeviceChangedListener(IDeviceChangedListener deviceChangedListener) {
		mDeviceChangedListener = deviceChangedListener;
	}
	
	public interface IDeviceChangedListener {
		public void notifyDeviceChanged(String appName);
	}

}
