package com.daqi.debug;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

public class ThreadTrace {

	final ThreadReference mThreadRef;
	final VirtualMachine mVM;

	static final String blank = "   ";
	static final String divide = "====== ";
	String baseIndent = "";//blank;
	static final String threadDelta = "\t";
	static final String sysSeparator = System.getProperty("line.separator");
	
	private long beginTime = System.currentTimeMillis();
	private String mStartThreadLog = null;
	
	StringBuffer logRecord;
	Stack<Method> methodStack;
	Stack<Method> mHistoryMethodStack;		// 线程执行历史堆栈信息
	private final int mHistoryMethodStackSize = 30;		// 缓存历史堆栈信息最多个数
	
	private static int sThreadCount = 0;	//记录线程创建个数
	private static int sDeathThreadCount = 0;	//记录已死线程数量
	private final int mThreadNO;			//当前线程编号

	ThreadTrace(ThreadReference thread, VirtualMachine vm) {
		mThreadRef = thread;
		mVM = vm;
		logRecord = new StringBuffer(256);
		methodStack = new Stack<Method>();
		mHistoryMethodStack = new Stack<Method>();
		
		if (!mThreadRef.name().contains("SharedPreferencesImpl")) {
			++sThreadCount;
		}
		mThreadNO = sThreadCount;
	}

	private void println(String str) {
		logRecord.append(this.baseIndent + str);
		logRecord.append(sysSeparator);
		System.out.println(this.baseIndent + str);
//		LogHelper.println(str/*getLogRecord()*/);
	}

	public String getLogRecord() {
		return logRecord.toString();
	}
	
	public long getCausedTime() {
		return System.currentTimeMillis() - beginTime;
	}
	
	public String getThreadName() {
		return mThreadRef.name();
	}

	void threadStartEvent(ThreadStartEvent event) {
		mStartThreadLog = "============开始启动一条新线程 " + event.thread().name() + " START at " + curTime(); 
		println(mStartThreadLog);
	}

	void threadDeathEvent(ThreadDeathEvent event) {
		++sDeathThreadCount;
		long causedTime = getCausedTime();
		int methodCount = methodStack.size();
		LogHelper.println(mStartThreadLog);
		printThreadStackBeforeDeath();
		mHistoryMethodStack.clear();
		LogHelper.println(divide + mThreadRef.name() + " --- 调用方法次数 " + methodCount + "，于 " + curTime() + " 结束，共计运行时长 = " + causedTime + "ms");
		LogHelper.println("");
		LogHelper.flush();
	}

	void methodEntryEvent(MethodEntryEvent event) {
//		println("         " + methodInfo(event.method()));
		if (methodStack.size() == 0) {
			increaseIndent();
		}
		methodStack.push(event.method());
		
		if (mHistoryMethodStack.size() < mHistoryMethodStackSize) {
			mHistoryMethodStack.push(event.method());
		}
	}

	void methodExitEvent(MethodExitEvent event) {
//		println("Exit Method:" + event.method().name());
		decreaseIndent();
	}
	
	void printThreadStackBeforeDeath() {
		while (methodStack.size() > 0
				&& !methodStack.peek().declaringType().name().contains("com.")) {
			methodStack.pop();
		}
		printCurMethodStackSnapShot();
		methodStack.clear();
	}
	
	void printCurMethodStackSnapShot() {
		Method methodObj = null;
		while (methodStack.size() > 0) {
			methodObj = methodStack.pop();
			LogHelper.println(twoBlank() + methodInfo(methodObj));
		}
	}
	
	void printHistoryMethodStatckSnapShot() {
		Method methodObj = null;
		int index = mHistoryMethodStack.size() - 1;
		while (index >= 0) {
			methodObj = mHistoryMethodStack.get(index);
			LogHelper.println(twoBlank() + methodInfo(methodObj));
			--index;
		}
		LogHelper.flush();
	}
	
	String curTime() {
		return new SimpleDateFormat("HH:mm:ss").format(new Date());//设置日期格式
	}
	
	String methodInfo(Method methodObj) {
		StringBuilder info = new StringBuilder();
		info.append(methodObj.name()).append("(");
		try {
//			for (String argment : methodObj.argumentTypeNames()) {
//				info.append(argment).append(", ");
//			}
			
			for (LocalVariable var : methodObj.arguments()) {
				info.append(var.name() + "=" + var.typeName());
			}
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		};
		info.append(")").append(" at ").append(methodObj.declaringType().name());
		if (methodObj.location() != null) {
			try {
//				info.append("   for file=" + methodObj.location().sourceName());
				info.append("(" + methodObj.location().lineNumber() + ")");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return info.toString();
	}
	
	void exceptionEvent(ExceptionEvent event) {
		increaseIndent();
		println(event.exception() + " catch: " + event.catchLocation());
		increaseIndent();
		printStackSnapShot();
		decreaseIndent();
		decreaseIndent();
		// Step to the catch
		EventRequestManager mgr = mVM.eventRequestManager();
		StepRequest req = mgr.createStepRequest(mThreadRef, StepRequest.STEP_MIN,
				StepRequest.STEP_INTO);
		req.addCountFilter(1); // next step only
		req.setSuspendPolicy(EventRequest.SUSPEND_ALL);
		req.enable();
	}

	// Step to exception catch
	void stepEvent(StepEvent event) {
		// when exception happens, adjust the indent
		while (methodStack.capacity() > 0
				&& methodStack.peek() != event.location().method()) {
			this.decreaseIndent();
			methodStack.pop();
		}
		EventRequestManager mgr = mVM.eventRequestManager();
		mgr.deleteEventRequest(event.request());
	}
	
	private void printVisiableVariables() {
		try {
			mThreadRef.suspend();
			if (mThreadRef.frameCount() > 0) {
				// retrieve current method frame
				StackFrame frame = mThreadRef.frame(0);
				if (frame.thisObject()!= null) {
					List<Field> fields = frame.thisObject().referenceType()
							.allFields();
					increaseIndent();
					for (Field field : fields) {
						println(field.name() + "\t" + field.typeName() + "\t"
								+ frame.thisObject().getValue(field));
					}
					decreaseIndent();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mThreadRef.resume();
		}
	}

	private void printStackSnapShot() {
		try {
			mThreadRef.suspend();
			println("Thread Status:" + mThreadRef.status());
			println("FrameCount in thread:" + mThreadRef.frameCount());
			List<StackFrame> frames = mThreadRef.frames();
			for (StackFrame frame : frames) {
				println("Frame(" + frame.location() + ")");
				if (frame.thisObject() != null) {
					increaseIndent();
					println("");
					List<Field> fields = frame.thisObject().referenceType()
							.allFields();
					for (Field field : fields) {
						println(field.name() + "\t" + field.typeName() + "\t"
								+ frame.thisObject().getValue(field));
					}
					decreaseIndent();
				}
				List<LocalVariable> lvs = frame.visibleVariables();
				increaseIndent();
				println("");
				for (LocalVariable lv : lvs) {
					println(lv.name() + "\t" + lv.typeName() + "\t"
							+ frame.getValue(lv));
				}
				decreaseIndent();
			}
		} catch (Exception e) {
			// ignore the exception
		} finally {
			mThreadRef.resume();
		}
	}

	private String twoBlank() {
		return blank + blank;
	}
	
	public void increaseIndent() {
		baseIndent += threadDelta;
	}

	public void decreaseIndent() {
		if (baseIndent.length() <= threadDelta.length()) {
			return;
		}
		baseIndent = baseIndent.substring(0,
				baseIndent.length() - threadDelta.length());
	}

}
