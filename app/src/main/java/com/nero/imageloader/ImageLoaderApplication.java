package com.nero.imageloader;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by lishiyan on 17/3/28.
 */

public class ImageLoaderApplication extends Application{
	@Override
	public void onCreate(){
		super.onCreate();

		LeakCanary.install(this);
	}
}
