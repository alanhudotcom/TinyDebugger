/**
 * Tencent is pleased to support the open source community by making APT available.
 * Copyright (C) 2014 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */


package com.daqi.debug;

import java.util.HashMap;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;

/**
* @Description 
* @date 2013年11月10日 下午5:03:48 
*
 */
public class AdbClientListener implements AndroidDebugBridge.IClientChangeListener{

	private HashMap<String, Client> mTargetAppClients;
	
	//静态锁，相等于类锁
	//这里可以保证，所有该类的对象中的所有方法都是同步执行的
	public static final Object sLock = new Object();
	private Client mCurChangedClient = null;
	
	public AdbClientListener() {
		super();
		mTargetAppClients = new HashMap<String, Client>(2);
	}
	
	public void addTargetAppName(String appName) {
		mTargetAppClients.put(appName, null);
	}
	
	@Override
	public void clientChanged(Client client, int changeMask) {
		ConsoleFactory.PrintlnForDebugStatus("Client Changed=" + client.getClientData().getClientDescription() + ", " + client.getDebuggerListenPort() + ", " + changeMask + " = " + getChangeMask(changeMask));
		if (client.getClientData() == null 
				|| client.getClientData().getClientDescription() == null
				|| !mTargetAppClients.containsKey(client.getClientData().getClientDescription())) {
			return;
		}
		
		String appName = client.getClientData().getClientDescription();
		Client preClient = mTargetAppClients.get(appName); 
		if (preClient != null) {
			if (changeMask != 2/*client.getDebuggerListenPort() == preClient.getDebuggerListenPort()*/) {
				// 端口未发生变化，则不进行更新
				return; 
			}
		} else {
			mTargetAppClients.put(appName, client);
		}
		
		mCurChangedClient = client;
		synchronized (sLock) {
			sLock.notify();
		}
	}
	
	public Client getChangedClient() {
		return mCurChangedClient;
	}
	
	private String getChangeMask(int changeMask) {
		String result = "UNKNOW";
		switch (changeMask) {
			case 0x0001: {
				result = "CHANGE_NAME";
			}
				break;
			case 0x0002: {
				result = "CHANGE_DEBUGGER_STATUS";
			}
				break;
			case 0x0004: {
				result = "CHANGE_PORT";
			}
				break;
			case 0x0008: {
				result = "CHANGE_THREAD_MODE";
			}
				break;
			case 0x0010: {
				result = "CHANGE_THREAD_DATA";
			}
				break;
			case 0x0020: {
				result = "CHANGE_HEAP_MODE";
			}
				break;
			case 0x0040: {
				result = "CHANGE_HEAP_DATA";
			}
				break;
			case 0x0080: {
				result = "CHANGE_NATIVE_HEAP_DATA";
			}
				break;
			case 0x0100: {
				result = "CHANGE_THREAD_STACKTRACE";
			}
				break;
			case 0x0200: {
				result = "CHANGE_HEAP_ALLOCATIONS";
			}
				break;
			case 0x0400: {
				result = "CHANGE_HEAP_ALLOCATION_STATUS";
			}
				break;
			case 0x0800: {
				result = "CHANGE_METHOD_PROFILING_STATUS";
			}
				break;
			default:
				break;
		}
		return result;
	}
	
}
