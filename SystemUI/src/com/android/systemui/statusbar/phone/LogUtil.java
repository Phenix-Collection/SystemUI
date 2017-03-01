package com.android.systemui.statusbar.phone;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;

import android.util.Log;

/**
 * 日志输出类
 * 
 * @Description:
 * @author mare
 * @date 2016年3月4日
 * @time 下午5:40:23
 */
public class LogUtil {

	/**
	 * 关闭日志输出
	 */
	private static boolean OPEN_LOG = true;
	/**
	 * 关闭DEBUG日志输出
	 */
	private static boolean DEBUG = true;
	private static FileOutputStream fos;
	/**
	 * TAG 名称
	 */
	private static String tag = "[app@mare@]";
	private String mClassName;
	private static LogUtil log;
	private static final String USER_NAME = "@mare@";

	private LogUtil(String name) {
		mClassName = name;
	}

	/**
	 * Get The Current Function Name
	 * 
	 * @return Name
	 */
	private String getFunctionName() {
		StackTraceElement[] sts = Thread.currentThread().getStackTrace();
		if (sts == null) {
			return null;
		}
		for (StackTraceElement st : sts) {
			if (st.isNativeMethod()) {
				continue;
			}
			if (st.getClassName().equals(Thread.class.getName())) {
				continue;
			}
			if (st.getClassName().equals(this.getClass().getName())) {
				continue;
			}
			tag = st.getClassName();
			return "[ " + Thread.currentThread().getName() + ": "
					+ st.getFileName() + ":" + st.getLineNumber() + " "
					+ st.getMethodName() + " ]";
		}
		return null;
	}

	public static void i(Object str) {
		print(Log.INFO, str);
	}

	public static void d(Object str) {
		print(Log.DEBUG, str);
	}

	public static void v(Object str) {
		print(Log.VERBOSE, str);
	}

	public static void w(Object str) {
		print(Log.WARN, str);
	}

	public static void e(Object str) {
		print(Log.ERROR, str);
	}

	/**
	 * 用于区分不同接口数据 打印传入参数
	 * 
	 * @param index
	 * @param str
	 */

	private static void print(int index, Object str) {
		if (!OPEN_LOG) {
			return;
		}
		if (log == null) {
			log = new LogUtil(USER_NAME);
		}
		String name = log.getFunctionName();
		if (name != null) {
			str = name + " - " + str;
		}

		// Close the debug log When DEBUG is false
		if (!DEBUG) {
			if (index <= Log.DEBUG) {
				return;
			}
		}
		switch (index) {
		case Log.VERBOSE:
			Log.v(tag, str.toString());
			break;
		case Log.DEBUG:
			Log.d(tag, str.toString());
			break;
		case Log.INFO:
			Log.i(tag, str.toString());
			break;
		case Log.WARN:
			Log.w(tag, str.toString());
			break;
		case Log.ERROR:
			Log.e(tag, str.toString());
			break;
		default:
			break;
		}
		
		
	}

	public static void setDebug(boolean flag) {
		DEBUG = flag;
	}

	public static void saveLog(StringBuffer sb, String dir, String fileName) {
		try {
			FileOutputStream fos = new FileOutputStream(
					new File(dir, fileName), true);
			fos.write(sb.toString().getBytes());
			fos.close();
		} catch (Exception e) {
			closeSilently(fos);
		}
	}

	protected static void closeSilently(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Throwable e) {
				// ignored
			}
		}
	}
}
