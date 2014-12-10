package com.daqi.debug;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.daqi.debug.config.ClassPatternFilter;
import com.daqi.debug.config.ExceptionFilter;
import com.sun.jdi.Field;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;
import com.sun.jdi.request.ThreadDeathRequest;
import com.sun.jdi.request.ThreadStartRequest;

public class VMSessionEventHandler {

	private VirtualMachine mTargetVM; // Running VM

	private boolean mConnected = true; // Connected to VM

	private boolean vmDied = true; // VMDeath occurred

	// Maps ThreadReference to ThreadTrace instances
	private Map<ThreadReference, ThreadTrace> traceMap = new LinkedHashMap<ThreadReference, ThreadTrace>();

	private Map<String, String> mKeyMethods = new LinkedHashMap<String, String>();
	
	private Map<String, String> mWatchFiledsMap = new HashMap<String, String>();
	
	private Thread mEventThread = null;
	
	VMSessionEventHandler(VirtualMachine vm) {
		this.mTargetVM = vm;
		registEventRequests();
		
		initMethodFilter();
	}
	
	private void initMethodFilter() {
//		mKeyMethods.put("com.example.apkplugdemo.Launcher$MyAdapter", "getView");
		mKeyMethods.put("org.apkplug.Bundle.StartActivity", "StartActivity");
		mKeyMethods.put("com.example.apkplugdemo.Launcher", "startActivity");
		mKeyMethods.put("com.apkplug.bundle.example.bundledemostartactivity1.MainActivity", "onCreate");
	}
	
	public VirtualMachine getCurSessionTargetVM() {
		return mTargetVM;
	}
	
	/**
	 * Run the event handling thread. As long as we are connected, get event
	 * sets off the queue and dispatch the events within them.
	 */
	public void startEventLoop() {
		
		ConsoleFactory.Println("已进入调试监控状态，可看到当前线程运行状态如下：");
		ConsoleFactory.Println("当前虚拟机支持调试信息如下：");
		ConsoleFactory.Println("是否支持字段修改监控=" + mTargetVM.canWatchFieldModification());
		ConsoleFactory.Println("是否支持字段读取监控=" + mTargetVM.canWatchFieldAccess());
		ConsoleFactory.Println("是否支持同步锁状态监控=" + mTargetVM.canRequestMonitorEvents());
		
		printInfoForCurProcessVM();
		
		mEventThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				eventLoop();
			}
		}, "Debug-Event-Loop");
		mEventThread.start();
		
//		try {
//			mEventThread.join();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}
	
	public void destroy() {
		
	}
	
	public void pauseVM() {
		mTargetVM.suspend();
	}
	
	public void resumeVM() {
		mTargetVM.resume();
	}
	
	public boolean isVMRunningOK() {
		boolean result = false;
		try {
			EventRequestManager mgr = mTargetVM.eventRequestManager();
			ExceptionRequest excReq = mgr.createExceptionRequest(null, false, true);
			excReq.setSuspendPolicy(EventRequest.SUSPEND_NONE);
			excReq.enable();
			excReq.disable();
			mgr.deleteEventRequest(excReq);
			result = true;
		} catch (Throwable e) {
			//NOTHING TO DO
		}
		return result;
	}
	
	private void eventLoop() {
		EventQueue queue = mTargetVM.eventQueue();
		while (mConnected) {
			try {
				EventSet eventSet = queue.remove();
				EventIterator it = eventSet.eventIterator();
				while (it.hasNext()) {
					handleEvent(it.nextEvent());
				}
				eventSet.resume();
			} catch (InterruptedException exc) {
				// Ignore
			} catch (VMDisconnectedException discExc) {
				handleDisconnectedException();
				break;
			}
		}
	}

	/**
	 * 注册事件监听
	 */
	private void registEventRequests() {
		EventRequestManager mgr = mTargetVM.eventRequestManager();
		// want all exceptions
//		ExceptionRequest excReq = mgr.createExceptionRequest(null, true, true);
//		// suspend so we can step
//		excReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
//		excReq.enable();

		String[] excludes = ClassPatternFilter.getExcludes();
		MethodEntryRequest menr = mgr.createMethodEntryRequest();
		for (int i = 0; i < excludes.length; ++i) {
			menr.addClassExclusionFilter(excludes[i]);
		}
		menr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
		menr.enable();

		MethodExitRequest mexr = mgr.createMethodExitRequest();
		for (int i = 0; i < excludes.length; ++i) {
			mexr.addClassExclusionFilter(excludes[i]);
		}
		mexr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
		mexr.enable();

		ThreadStartRequest tsr = mgr.createThreadStartRequest();
		// Make sure we sync on thread death
		tsr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
		tsr.enable();

		ThreadDeathRequest tdr = mgr.createThreadDeathRequest();
		// Make sure we sync on thread death
		tdr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
		tdr.enable();
	}

		/**
	 * Returns the ThreadTrace instance for the specified thread, creating one
	 * if needed.
	 */
	ThreadTrace threadTrace(ThreadReference thread) {
		ThreadTrace trace = (ThreadTrace) traceMap.get(thread);
		if (trace == null) {
			trace = new ThreadTrace(thread, mTargetVM);
			traceMap.put(thread, trace);
		}
		return trace;
	}
	
	ThreadTrace getThreadTrace(ThreadReference thread) {
		ThreadTrace trace = (ThreadTrace) traceMap.get(thread);
		return trace;
//		return threadTrace(thread);
	}

	/**
	 * Dispatch incoming events
	 */
	private void handleEvent(Event event) {
		int count = 0;
		if (count == 1) {
			printAllThreadInfos();
		}
		if (event instanceof ExceptionEvent) {
			if (ExceptionFilter.isAllowedException(((ExceptionEvent) event)
					.exception().referenceType().name())) {
				exceptionEvent((ExceptionEvent) event);
			}
			
		} else if (event instanceof MethodEntryEvent) {
			if (!isMainThreadOrCreatedFromMain(((MethodEntryEvent) event).thread())
					/*&& !isFilterThread(((MethodEntryEvent) event).thread())*/) {
				methodEntryEvent((MethodEntryEvent) event);
			}
			
		} else if (event instanceof MethodExitEvent) {
//			String className = ((MethodExitEvent) event).method().declaringType().name();
//			String keyMethod = mKeyMethods.get(className);
//			if (keyMethod != null && ((MethodExitEvent) event).method().name().contains(keyMethod)) {
//				printCurMethodStackTraceWhenMethodExit((MethodExitEvent)event);
//			}
			if (!isMainThreadOrCreatedFromMain(((MethodExitEvent) event).thread())) {
				methodExitEvent((MethodExitEvent) event);
			}
			
		} else if (event instanceof StepEvent) {
			stepEvent((StepEvent) event);
			// 打印当前线程快照
			printStackSnapShot(((StepEvent) event).thread());
			
		} else if (event instanceof ThreadStartEvent) {
			threadStartEvent((ThreadStartEvent) event);
			handleStartThread(((ThreadStartEvent) event).thread());
			
		} else if (event instanceof ThreadDeathEvent) {
			threadDeathEvent((ThreadDeathEvent) event);
			
		} else if (event instanceof VMStartEvent) {
			vmStartEvent((VMStartEvent) event);
			
		} else if (event instanceof VMDeathEvent) {
			vmDeathEvent((VMDeathEvent) event);
			
		} else if (event instanceof VMDisconnectEvent) {
			vmDisconnectEvent((VMDisconnectEvent) event);
			
		} else if (event instanceof ClassPrepareEvent) {
			String watchClassName = ((ClassPrepareEvent) event).referenceType().name();
			ConsoleFactory.Println("监控被修改的类：" + watchClassName);
			String fieldName = mWatchFiledsMap.get(watchClassName);
			Field filed = ((ClassPrepareEvent)event).referenceType().fieldByName(fieldName);
			ModificationWatchpointRequest modificationWatchpointRequest =
					mTargetVM.eventRequestManager().createModificationWatchpointRequest(filed);
			modificationWatchpointRequest.setEnabled(true);
			
		} else if (event instanceof ModificationWatchpointEvent) {
			final ModificationWatchpointEvent modEvent = (ModificationWatchpointEvent) event;
//			final ObjectReference oldRef = (ObjectReference) modEvent.valueCurrent();
//	        final ObjectReference newRef = (ObjectReference) modEvent.valueToBe();
	        String message = "！！！注意：字段" + "将要被线程" + modEvent.thread().name() + "所修改";
	        ConsoleFactory.Println(message);
	        message = "oldvalude=" + modEvent.valueCurrent() != null ? modEvent.toString() : "UNKNOW" + ", newvalue=" + modEvent.valueToBe() != null ? modEvent.valueToBe().toString() : "UNKNOW";
	        ConsoleFactory.Println(message);
//	          System.out.println("["
//	              + "]# "
//	              + "Prism Kernel: "
//	              + (oldRef == null ? "\"UNKNOW\"" : oldRef.invokeMethod(((ModificationWatchpointEvent) event).thread(), vm
//	                  .classesByName(STATE_CLASS_NAME).get(0).methodsByName("name").get(0), new ArrayList<Value>(),
//	                  ObjectReference.INVOKE_NONVIRTUAL))
//	              + " -> "
//	              + (newRef == null ? "\"UNKNOW\"" : newRef.invokeMethod(((ModificationWatchpointEvent) event).thread(), vm
//	                  .classesByName(STATE_CLASS_NAME).get(0).methodsByName("name").get(0), new ArrayList<Value>(),
//	                  ObjectReference.INVOKE_NONVIRTUAL)));
			
		} else {
//			throw new Error("Unexpected event type");
		}
	}

	/***************************************************************************
	 * A VMDisconnectedException has happened while dealing with another event.
	 * We need to flush the event queue, dealing only with exit events (VMDeath,
	 * VMDisconnect) so that we terminate correctly.
	 */
	synchronized void handleDisconnectedException() {
		EventQueue queue = mTargetVM.eventQueue();
		while (mConnected) {
			try {
				EventSet eventSet = queue.remove();
				EventIterator iter = eventSet.eventIterator();
				while (iter.hasNext()) {
					Event event = iter.nextEvent();
					if (event instanceof VMDeathEvent) {
						vmDeathEvent((VMDeathEvent) event);
					} else if (event instanceof VMDisconnectEvent) {
						vmDisconnectEvent((VMDisconnectEvent) event);
					}
				}
				mConnected = false;
//				eventSet.resume(); // Resume the VM
			} catch (InterruptedException exc) {
				exc.printStackTrace();
			}
		}
	}

	private void vmStartEvent(VMStartEvent event) {
		LogHelper.println("-- VM Started --");
	}

	// Forward event for thread specific processing
	private void methodEntryEvent(MethodEntryEvent event) {
		ThreadTrace tTrace = getThreadTrace(event.thread());
		if (tTrace != null) {
			tTrace.methodEntryEvent(event);
		}
	}
	
	// Forward event for thread specific processing
	private void methodExitEvent(MethodExitEvent event) {
		ThreadTrace tTrace = getThreadTrace(event.thread());
		if (tTrace != null) {
			tTrace.methodExitEvent(event);
		}
	}

	// Forward event for thread specific processing
	private void stepEvent(StepEvent event) {
		threadTrace(event.thread()).stepEvent(event);
	}

	void threadDeathEvent(ThreadDeathEvent event) {
		ThreadTrace trace = (ThreadTrace) traceMap.get(event.thread());
		if (trace != null) { // only want threads we care about
			trace.threadDeathEvent(event); // Forward event
		}
		traceMap.remove(event.thread());
	}

	void threadStartEvent(ThreadStartEvent event) {
		threadTrace(event.thread()).threadStartEvent(event);
	}

	private void exceptionEvent(ExceptionEvent event) {
		threadTrace(event.thread()).exceptionEvent(event);
	}

	public void vmDeathEvent(VMDeathEvent event) {
		vmDied = true;
		printAllThreadInfos();
		LogHelper.println("-- The application exited --");
	}

	public void vmDisconnectEvent(VMDisconnectEvent event) {
		mConnected = false;
		printAllThreadInfos();
		if (!vmDied) {
			LogHelper.println("-- The application has been disconnected --");
		}
	}
	
	/**
	 * 当指定函数执行完毕后，打印出当前整体函数堆栈信息
	 * @param event
	 */
	private void printCurMethodStackTraceWhenMethodExit(MethodExitEvent event) {
		ThreadTrace tTrace = getThreadTrace(event.thread());
		if (tTrace != null) {
			tTrace.printCurMethodStackSnapShot();
		}
		LogHelper.flush();
	}

	public void printAllThreadInfos() {
		Set<ThreadReference> threadSet = traceMap.keySet();
		for (ThreadReference thread : threadSet) {
			LogHelper.println(traceMap.get(thread).getLogRecord());
			LogHelper.println("*********************************************************");
		}
		LogHelper.flush();
	}

	public boolean isMainThreadOrCreatedFromMain(ThreadReference tr) {
		if (tr == null || "system".equalsIgnoreCase(tr.name())) {
			return true;
		}
		if (tr.name().toLowerCase().contains("main")) {
			return true;
		}
		return false;
	}
	
	private boolean isStopDebuggedMethod(ThreadReference tr) {
		if (tr == null) {
			return true;
		}
		return tr.name().contains("justForTestScreenFinished");
//		return tr.name().equals("justForTestScreenFinished");
	}
	
	
	private void handleStartThread(ThreadReference threadRef) {
//		ConsoleFactory.Println("============开始启动一条新线程 " + threadRef.name());
		
//		EventRequestManager mgr = mTargetVM.eventRequestManager();
//		StepRequest stepReq = mgr.createStepRequest(threadRef, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
//		stepReq.addCountFilter(1);
//		stepReq.setSuspendPolicy(EventRequest.SUSPEND_ALL);
//		stepReq.enable();
	}
	
	/**
	 * 通过线程名称，打印当前线程堆栈信息
	 * @param threadname：线程名称
	 * @return：打印符合要求的线程个数
	 */
	public int printThreadInfoByName(String threadname) {
		int count = 0;
		for (ThreadReference threadRef : mTargetVM.allThreads()) {
			if (threadRef.name().contains(threadname)) {
				printStackSnapShot(threadRef);
				++count;
			}
		}
		return count;
	}
	
	public int printThreadInfoHistoryByName(String threadName) {
		int count = 0;
		for (ThreadReference threadRef : mTargetVM.allThreads()) {
			if (threadRef.name().contains(threadName)) {
				printHistoryStackSnapShot(threadRef);
				++count;
			}
		}
		return count;
	}
	
	public void pauseThreadByName(String threadName) {
		for (ThreadReference threadRef : mTargetVM.allThreads()) {
			if (threadRef.name().contains(threadName)) {
				printStackSnapShot(threadRef);
				threadRef.suspend();
				ConsoleFactory.Println("============线程 " + threadRef.name() + "已暂停");
			}
		}
	}
	
	/**
	 * 打印当前顶部栈的变量值
	 * @param threadRef
	 */
	private void printTopFrameVariables(ThreadReference threadRef) {
		try {
			threadRef.suspend();
			if(threadRef.frameCount() > 0)
			{
				StackFrame frame = threadRef.frame(0);
				List<Field> fields = frame.thisObject().referenceType().allFields();
				for (Field field : fields) {
					ConsoleFactory.Println(field.name() + "\t" + field.typeName() + "\t" + frame.thisObject().getValue(field));
				}
			}
		}
		catch(Exception e) {
			//ignore
		} finally {
			threadRef.resume();
		}
	} 
	
	/**
	 * 添加变量监控字段
	 * @param className：被监控的字段所在的类名
	 * @param filedName：被监控的字段
	 */
	public void addWatchVariable(String className, String filedName) {
		
		ConsoleFactory.Println("添加监控参数：" + className + ", " + filedName);
		
		ClassPrepareRequest watchRequest = mTargetVM.eventRequestManager().createClassPrepareRequest();
		watchRequest.addClassFilter(className);
		watchRequest.enable();
		mWatchFiledsMap.put(className, filedName);
		
//		Field filed = ((ClassPrepareEvent)event).referenceType().fieldByName(fieldName);
//		ModificationWatchpointRequest modificationWatchpointRequest =
//				mTargetVM.eventRequestManager().createModificationWatchpointRequest(filed);
//		modificationWatchpointRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
//		modificationWatchpointRequest.setEnabled(true);
		
		
	     // Create BreakpointEvent
        /*BreakpointRequest breakpointRequest = eventRequestManager
                .createBreakpointRequest(location);
        breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        breakpointRequest.enable();
        
        BreakpointEvent breakpointEvent = (BreakpointEvent) event;
        ThreadReference threadReference = breakpointEvent.thread();
        StackFrame stackFrame = threadReference.frame(0);
        LocalVariable localVariable = stackFrame
                .visibleVariableByName("str");
        Value value = stackFrame.getValue(localVariable);
        String str = ((StringReference) value).value();
        System.out.println("The local variable str at line 10 is " + str
                + " of " + value.type().name());
        eventSet.resume();*/
		
	}
	
	/**
	 * 打印当前整个进程的信息
	 */
	public void printInfoForCurProcessVM() {
		ConsoleFactory.Println("=======================当前调试进程信息如下：");
		int totalThreadCnt = mTargetVM.allThreads().size();
		String totalThreadInfo = "========共有线程数量=" + totalThreadCnt; 
		int sysThreadCnt = 0;
		for (ThreadGroupReference tgRef : mTargetVM.topLevelThreadGroups()) {
			if ("system".equalsIgnoreCase(tgRef.name())) {
				sysThreadCnt = tgRef.threads().size();
				break;
			}
		}
		String sysThreadInfo = "，其中系统线程数=" + sysThreadCnt;
		int androidThreadCnt = 2;
		String androidThreadInfo = "，android框架启动线程数=" + androidThreadCnt;
		String appThreadInfo = "，应用本身启动线程=" + (totalThreadCnt - sysThreadCnt - androidThreadCnt);
		ConsoleFactory.Println(totalThreadInfo + sysThreadInfo + androidThreadInfo + appThreadInfo);
		if (mTargetVM.allThreads().size() > 0) {
			for (ThreadReference threadRef : mTargetVM.allThreads()) {
				String attributeInfo = "";
				if (threadRef.name().equals("Binder_1") || threadRef.name().equals("Binder_2")) {
					attributeInfo = "-（Android框架线程）";
				}
				ConsoleFactory.Println("===线程组=" + threadRef.threadGroup().name() + ", 线程=" + threadRef.name() + attributeInfo);
			}
		}
		
		printNewAddedThreadInfo();
		
		ConsoleFactory.Println("========mTargetVM.name()=" + mTargetVM.name() + "===================当前调试进程信息结束！！！");
	}
	
	/**
	 * 输出新增的线程信息
	 * @return：新增线程个数
	 */
	public int printNewAddedThreadInfo() {
		int count = traceMap.size();
		if (count <= 0) {
			return 0;
		}
		ConsoleFactory.Println("=======================相较初始调试状态，当前存活新增线程 【" + count +"】 个，信息如下：");
		int index = 0;
		for (ThreadTrace thread : traceMap.values()) {
			ConsoleFactory.Println("=====Top-" + (++index) + "==" + thread.getThreadName() + ", 已运行时长 " + thread.getCausedTime() + "ms");
		}
		return traceMap.size();
	}
	
	/**
	 * 打印当前线程栈帧信息
	 * @param threadRef
	 */
	private void printStackSnapShot(ThreadReference threadRef) {
		boolean isSuspended = threadRef.isSuspended();
		try {
			ConsoleFactory.Println("============线程 " + threadRef.name() + " 信息如下：");
			ConsoleFactory.Println("Thread Status: " + getThreadStatus(threadRef.status()));
			if (!isSuspended) {
				threadRef.suspend();
			}
			ConsoleFactory.Println("FrameCount: " + threadRef.frameCount());
			ThreadTrace threadTrace = getThreadTrace(threadRef);
			if (threadTrace != null) {
				ConsoleFactory.Println("已运行时长: " + threadTrace.getCausedTime() + "ms");
			}
			List<StackFrame> frames = threadRef.frames();
			for (StackFrame frame : frames) {
				ConsoleFactory.Println("Frame(" + frame.location() + ")");
			}
			ConsoleFactory.Println("============线程 " + threadRef.name() + " 信息打印结束！");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (!isSuspended) {
				threadRef.resume();
			}
		}
	}
	
	/**
	 * 打印指定线程栈帧历史信息
	 * @param threadRef
	 */
	private void printHistoryStackSnapShot(ThreadReference threadRef) {
		boolean isSuspended = threadRef.isSuspended();
		try {
			ConsoleFactory.Println("============线程 " + threadRef.name() + " 历史信息如下：");
			ConsoleFactory.Println("Thread Status: " + getThreadStatus(threadRef.status()));
			if (!isSuspended) {
				threadRef.suspend();
			}
//			ConsoleFactory.Println("FrameCount: " + threadRef.frameCount());
			ThreadTrace threadTrace = getThreadTrace(threadRef);
			if (threadTrace != null) {
				ConsoleFactory.Println("已运行时长: " + threadTrace.getCausedTime() + "ms");
			}
			threadTrace.printHistoryMethodStatckSnapShot();
			ConsoleFactory.Println("============线程 " + threadRef.name() + " 历史信息打印结束！");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (!isSuspended) {
				threadRef.resume();
			}
		}
	}
	
	private boolean isFilterThreadMethod(ThreadReference threadRef) {
		return false;
	}
	
	private String getThreadStatus(int status) {
		String statusResult = null;
		switch (status) {
			case -1: {
				statusResult = "THREAD_STATUS_UNKNOWN";
			}
				break;
			case 0: {
				statusResult = "THREAD_STATUS_ZOMBIE";
			}
				break;
			case 1: {
				statusResult = "THREAD_STATUS_RUNNING";
			}
				break;
			case 2: {
				statusResult = "THREAD_STATUS_SLEEPING";
			}
				break;
			case 3: {
				statusResult = "THREAD_STATUS_MONITOR";
			}
				break;
			case 4: {
				statusResult = "THREAD_STATUS_WAIT";
			}
				break;
			case 5: {
				statusResult = "THREAD_STATUS_NOT_STARTED";
			}
				break;
					
			default:
				break;
		}
		
		return statusResult;
	}
	
}
