package com.kpo.mcu.Util;

import android.util.Log;

public class Writelog {

	public static void d(String tag, String format, Object... args) {
		Log.d(tag, String.format(format, args));
	}

	public static void v(String tag, String format, Object... args) {
		Log.v(tag, String.format(format, args));
	}

	public static void w(String tag, String format, Object... args) {
		Log.w(tag, String.format(format, args));
	}

	public static void i(String tag, String format, Object... args) {
		Log.i(tag, String.format(format, args));
	}

	public static void e(String tag, String format, Object... args) {
		Log.e(tag, String.format(format, args));
	}

	public static void buffer(String tag, String msg, byte[] data, int offset, int count) {
		String str = " ";
		for (int i = 0; i < count; i++) {
			str += String.format("%02X ", data[offset + i]);
		}
		Log.d(tag, msg + str);
	}

	public static void buffer(String tag, String msg, byte[] data) {
		buffer(tag, msg, data, 0, (data != null) ? data.length : 0);
	}

	public static void exception(String tag, Exception e) {
		Log.e(tag, e.getClass().toString());
		StackTraceElement[] messages = e.getStackTrace();
		for (StackTraceElement t : messages) {
			Log.e(tag, t.toString());
		}
	}
}
