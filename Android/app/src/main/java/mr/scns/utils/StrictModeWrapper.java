package mr.scns.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.util.Log;

public class StrictModeWrapper {

	 
	public static void permitAll() {
		//if (android.os.Build.VERSION.SDK_INT > 9) {
			//StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			//StrictMode.setThreadPolicy(policy);
		//}
		try {
	        Class<?> strictModeClass = Class.forName("android.os.StrictMode", true, Thread.currentThread()
	                .getContextClassLoader());

	        Class<?> threadPolicyClass = Class.forName("android.os.StrictMode$ThreadPolicy", true, Thread
	                .currentThread().getContextClassLoader());

	        Class<?> threadPolicyBuilderClass = Class.forName("android.os.StrictMode$ThreadPolicy$Builder", true,
	                Thread.currentThread().getContextClassLoader());

	        Method setThreadPolicyMethod = strictModeClass.getMethod("setThreadPolicy", threadPolicyClass);

	        Method detectAllMethod = threadPolicyBuilderClass.getMethod("detectAll");
	        Method penaltyMethod = threadPolicyBuilderClass.getMethod("penaltyLog");
	        Method buildMethod = threadPolicyBuilderClass.getMethod("build");

	        Constructor<?> threadPolicyBuilderConstructor = threadPolicyBuilderClass.getConstructor();
	        Object threadPolicyBuilderObject = threadPolicyBuilderConstructor.newInstance();

	        Object obj = detectAllMethod.invoke(threadPolicyBuilderObject);

	        obj = penaltyMethod.invoke(obj);
	        Object threadPolicyObject = buildMethod.invoke(obj);
	        setThreadPolicyMethod.invoke(strictModeClass, threadPolicyObject);

	    } catch (Exception ex) {
	        Log.w("StricModeWrapper", ex);
	    }
	}

}
