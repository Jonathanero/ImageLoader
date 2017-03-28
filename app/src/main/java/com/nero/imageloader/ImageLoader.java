package com.nero.imageloader;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lishiyan on 17/3/27.
 */

public class ImageLoader{

	public static final String TAG = "ImageLoader";

	public static final int MESSAGE_POST_RESULT = 1;

	public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
	public static final int CORE_POOL_SIZE = CPU_COUNT + 1;
	public static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
	public static final long KEEP_ALIVE = 10L;

	private static final int TAG_KEY_URI = R.id.imageloader_uri;
	public static final int DISK_CACHE_SIZE = 1024 * 1024 * 50; //50MB
	public static final int IO_BUFFER_SIZE = 8 * 1024; //8KB
	public static final int DISK_CACHE_INDEX = 0;
	private boolean mIsDiskLruCacheCreated = false;

	private static final ThreadFactory sThreadFactory = new ThreadFactory(){

		private final AtomicInteger mCount = new AtomicInteger(1);
		@Override
		public Thread newThread(final Runnable r){
			return new Thread(r,"ImageLoader#"+mCount.incrementAndGet());
		}
	};

	//使用线程池来来处理图片加载任务
	private static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
		CORE_POOL_SIZE,
		MAXIMUM_POOL_SIZE,
		KEEP_ALIVE,
		TimeUnit.SECONDS,
		new LinkedBlockingDeque<Runnable>(),sThreadFactory);

	private Handler mMainHandler = new Handler(Looper.myLooper()){
		@Override
		public void handleMessage(final Message msg){
			LoaderResult result = (LoaderResult) msg.obj;
			ImageView imageView = result.imageView;
			String uri = (String) imageView.getTag(TAG_KEY_URI);
			if (uri.equals(result.uri)) {
				imageView.setImageBitmap(result.bitmap);
			} else {
				Log.w(TAG, "set image bitmap,but url has changed, ignored!");
			}
		}
	};

	private Context mContext;
	private ImageResizer mImageResizer = new ImageResizer();
	private LruCache<String,Bitmap> mMemoryCache;
	private DiskLruCache mDiskLruCache;

	private ImageLoader(Context context){
		mContext = context.getApplicationContext();
		int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024); // xx KB
		int cacheSize = maxMemory / 8;
		mMemoryCache = new LruCache<String,Bitmap>(cacheSize){
			@Override
			protected int sizeOf(final String key,final Bitmap bitmap){
				return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
			}
		};

		File diskFileDir = getDiskCacheDir(mContext,"bitmap");
		if(!diskFileDir.exists()){
			diskFileDir.mkdirs();
		}
		if(getUsableSpace(diskFileDir) > DISK_CACHE_SIZE){
			try{
				mDiskLruCache = DiskLruCache.open(diskFileDir,1,1,DISK_CACHE_SIZE);
				mIsDiskLruCacheCreated = true;
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}

	public static ImageLoader build(Context context){
		return new ImageLoader(context);
	}

	private void addBitmapToMemoryCache(String key,Bitmap bitmap){
		if(getBitmapFromMemCache(key) == null){
			mMemoryCache.put(key,bitmap);
		}
	}

	public Bitmap getBitmapFromMemCache(String key){
		return mMemoryCache.get(key);
	}

	/**
	 * 从内存、磁盘或者网络加载图片的bitmap,并把bitmap绑定到ImageView
	 * @param uri       图片的uri
	 * @param imageView bitmap绑定的对象
	 */
	public void bindBitmap(final String uri,final ImageView imageView){
		bindBitmap(uri,imageView,0,0);
	}

	public void bindBitmap(final String uri,final ImageView imageView,
	                       final int reqWidth,final int reqHeight){

		imageView.setTag(TAG_KEY_URI,uri);
		Bitmap bitmap = loadBitmapFromMemCache(uri);
		if(bitmap != null){
			Log.d(TAG, "loadBitmapFromMemCache,url:" + uri);
			imageView.setImageBitmap(bitmap);
			return;
		}

		Runnable loadBitmapTask = new Runnable(){

			@Override
			public void run(){
				Bitmap bitmap = loadBitmap(uri,reqWidth,reqHeight);
				if(bitmap != null){
					LoaderResult result = new LoaderResult(imageView,uri,bitmap);
					mMainHandler.obtainMessage(MESSAGE_POST_RESULT,result).sendToTarget();
				}
			}
		};
		THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
	}

	/**
	 * 从内存、磁盘或者网络加载图片的bitmap
	 * @param uri 图片的uri
	 * @param reqWidth  请求的图片宽度
	 * @param reqHeight 请求的图片高度
	 * @return 图片的bitmap，可能为null
	 */
	public Bitmap loadBitmap(String uri,int reqWidth,int reqHeight){
		Bitmap bitmap = loadBitmapFromMemCache(uri);
		if(bitmap != null){
			Log.d(TAG, "loadBitmapFromMemCache,url:" + uri);
			return bitmap;
		}

		try{
			bitmap = loadBitmapFromDiskCache(uri,reqWidth,reqHeight);
			if(bitmap != null){
				Log.d(TAG, "loadBitmapFromDiskCache,url:" + uri);
				return bitmap;
			}

			bitmap = loadBitmapFromHttp(uri,reqWidth,reqHeight);
			Log.d(TAG, "loadBitmapFromHttp,url:" + uri);
		}catch(IOException e){
			e.printStackTrace();
		}

		if(bitmap == null && !mIsDiskLruCacheCreated){
			bitmap = downloadBitmapFromUrl(uri);
			Log.w(TAG, "encounter error, DiskLruCache not created!");
		}
		return bitmap;
	}

	private Bitmap loadBitmapFromMemCache(String url){
		final String key = hashKeyFromUrl(url);
		Bitmap bitmap = getBitmapFromMemCache(key);
		return bitmap;
	}

	private Bitmap loadBitmapFromHttp(String url,int reqWidth,int reqHeight) throws IOException{
		if(Looper.myLooper() == Looper.getMainLooper()){
			throw new RuntimeException("cannot access network form UI thread");
		}
		if(mDiskLruCache == null){
			return null;
		}

		String key = hashKeyFromUrl(url);
		DiskLruCache.Editor editor = mDiskLruCache.edit(key);
		if(editor != null){
			OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
			if(downloadUrlToStream(url,outputStream)){
				editor.commit();
			}else{
				editor.abort();
			}
			mDiskLruCache.flush();
		}
		return loadBitmapFromDiskCache(url,reqWidth,reqHeight);
	}

	private Bitmap loadBitmapFromDiskCache(String url,int reqWidth,int reqHeight) throws IOException{
		if(Looper.myLooper() == Looper.getMainLooper()){
			Log.w(TAG,"Load Bitmap from UI Thread,it's not recommemed!");
		}
		if(mDiskLruCache == null){
			return null;
		}

		Bitmap bitmap = null;
		String key = hashKeyFromUrl(url);
		DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
		if(snapShot != null){
			FileInputStream fileInputStream = (FileInputStream)snapShot.getInputStream(DISK_CACHE_INDEX);
			FileDescriptor fileDescriptor = fileInputStream.getFD();
			bitmap = mImageResizer.decodeBitmapFromFileDescriptor(fileDescriptor,reqWidth,reqHeight);
			if(bitmap != null){
				addBitmapToMemoryCache(key,bitmap);
			}
		}

		return bitmap;
	}

	public boolean downloadUrlToStream(String urlString,OutputStream outputStream){
		HttpURLConnection urlConnection = null;
		BufferedInputStream in = null;
		BufferedOutputStream out = null;

		try{
			URL url = new URL(urlString);
			urlConnection = (HttpURLConnection)url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(),IO_BUFFER_SIZE);
			out = new BufferedOutputStream(outputStream,IO_BUFFER_SIZE);

			int b;
			while((b = in.read()) != -1){
				out.write(b);
			}
			return true;
		}catch(IOException e){
			Log.e(TAG,e.getLocalizedMessage());
			//Log.e(TAG,"load bitmap from "+urlString+" failed");
		}finally{
			if(urlConnection != null){
				urlConnection.disconnect();
			}

			MyUtils.close(in);
			MyUtils.close(out);
		}
		return false;
	}

	private Bitmap downloadBitmapFromUrl(String urlString){

		Bitmap bitmap = null;
		HttpURLConnection urlConnection = null;
		BufferedInputStream in = null;

		try{
			URL url = new URL(urlString);
			urlConnection = (HttpURLConnection)url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(),IO_BUFFER_SIZE);
			bitmap = BitmapFactory.decodeStream(in);

		}catch(IOException e){

		}finally{
			if(urlConnection != null){
				urlConnection.disconnect();
			}

			MyUtils.close(in);
		}

		return bitmap;
	}

	//使用url的MD5 hash值来作为缓存的key值
	private String hashKeyFromUrl(String url){
		String cacheKey;
		try{
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(url.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		}catch(NoSuchAlgorithmException e){
			cacheKey = String.valueOf(url.hashCode());
		}

		return cacheKey;
	}

	private String bytesToHexString(byte[] digest){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < digest.length; i++){
			String hex = Integer.toHexString(0xFF & digest[i]);
			if(hex.length() == 1){
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	private File getDiskCacheDir(Context context,String uniqueName){
		boolean externalStorageAvailable = Environment.
			getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
		final String cachepPath;
		if(externalStorageAvailable){
			cachepPath = context.getExternalCacheDir().getPath();
		}else{
			cachepPath = context.getCacheDir().getPath();
		}

		return new File(cachepPath+File.separator+uniqueName);
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private long getUsableSpace(File path){
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD){
			return path.getUsableSpace();
		}

		final StatFs stats = new StatFs(path.getPath());
		return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
	}

	private static class LoaderResult {
		public ImageView imageView;
		public String uri;
		public Bitmap bitmap;

		public LoaderResult(ImageView imageView, String uri, Bitmap bitmap) {
			this.imageView = imageView;
			this.uri = uri;
			this.bitmap = bitmap;
		}
	}
}
