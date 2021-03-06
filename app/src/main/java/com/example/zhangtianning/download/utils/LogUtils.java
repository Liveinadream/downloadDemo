package com.example.zhangtianning.download.utils;


import android.util.Log;

import com.example.zhangtianning.download.BuildConfig;
import com.example.zhangtianning.download.download.FileDownload;


public class LogUtils {

	private final static int I = 1, D = 2, E = 3;

	private static void print(int mod, String tag, String msg) {
		switch (mod) {
		case I:
			Log.i(tag, msg);
			break;
		case D:
			Log.d(tag, msg);
			break;
		case E:
			Log.e(tag, msg);
			break;
		}
	}

	public static void E(String tag, String message) {
		if (!BuildConfig.DEBUG)
			return;
		if (message != null) {
			print(E, tag, message);
		}
	}


	public static void D(String tag, String message) {
		if (!BuildConfig.DEBUG)
			return;
		if (message != null) {
			print(D, tag, message);
		}
	}

	/*public static void E(String tag, String message) {
		if (!Constant.LOG_PRINT)
			return;
		if (message != null) {
			print(E, tag, message);
		}
	}*/

	public static void I(String tag, String message) {
		if (!BuildConfig.DEBUG)
			return;
		if (message != null) {
			print(I, tag, message);
		}
	}

	/**
	 * 打印日志
	 * @param tag
	 * @param message
	 */
	public static void println(String tag, String message){
		if (!BuildConfig.DEBUG){
			return;
		}
		System.out.println(tag+"====="+message);
	}


}