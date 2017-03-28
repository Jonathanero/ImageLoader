package com.nero.imageloader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;

/**
 * Created by lishiyan on 17/3/27.
 */

public class ImageResizer{

	public static final String TAG = "ImageResizer";

	public ImageResizer(){

	}

	public Bitmap decodeBitmapFromResource(Resources res,int resId,int reqWidth,int reqHeight){
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res,resId,options);

		options.inSampleSize = calculateInSampleSize(options,reqWidth,reqHeight);

		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res,resId,options);
	}

	public Bitmap decodeBitmapFromFileDescriptor(FileDescriptor fd,int reqWidth,int reqHeight){
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(fd,null,options);

		options.inSampleSize = calculateInSampleSize(options,reqWidth,reqHeight);

		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFileDescriptor(fd,null,options);
	}

	//图片的裁剪
	private int calculateInSampleSize(BitmapFactory.Options options,int reqWidth,int reqHeight){
		if(reqWidth == 0 || reqHeight == 0){
			return 1;
		}

		final int width = options.outWidth;
		final int height = options.outHeight;

		int inSampleSize = 1;

		if(height > reqHeight || width > reqWidth){
			final int halfWidth = width / 2;
			final int halfHeight = height / 2;

			while((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth){
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}
}
