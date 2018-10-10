package uark.edu.detection;

import android.app.Application;

//import cn.sharesdk.framework.ShareSDK;


public class Detection extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		Conf.init(getApplicationContext());
//		ShareSDK.initSDK(this);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
//		ShareSDK.stopSDK(this);
	}
}