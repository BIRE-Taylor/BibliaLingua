package es.tjon.biblialingua.utils;

import android.content.*;
import android.graphics.*;
import android.util.*;
import com.android.volley.*;
import com.android.volley.toolbox.*;
import java.io.*;

public class MySingleton
{
	private static MySingleton mInstance;
	private RequestQueue mRequestQueue;
	private ImageLoader mImageLoader;
	private static Context mCtx;

	private MySingleton(Context context)
	{
		mCtx = context;
		mRequestQueue = getRequestQueue();

		mImageLoader = new ImageLoader(mRequestQueue,
			new ImageLoader.ImageCache() {
				private final LruCache<String, Bitmap>
				cache = new LruCache<String, Bitmap>(20);
				private final FileCache fCache = new FileCache();
				@Override
				public Bitmap getBitmap(String url)
				{
					Bitmap value = cache.get(url);
					if(value==null)
						value = fCache.get(url);
					return value;
				}

				@Override
				public void putBitmap(String url, Bitmap bitmap)
				{
					cache.put(url, bitmap);
					fCache.put(url, bitmap);
				}
			});
			
	}

	public static synchronized MySingleton getInstance(Context context)
	{
		if (mInstance == null)
		{
			mInstance = new MySingleton(context);
		}
		return mInstance;
	}

	public RequestQueue getRequestQueue()
	{
		if (mRequestQueue == null)
		{
			// getApplicationContext() is key, it keeps you from leaking the
			// Activity or BroadcastReceiver if someone passes one in.
			mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
		}
		return mRequestQueue;
	}

	public <T> void addToRequestQueue(Request<T> req)
	{
		getRequestQueue().add(req);
	}

	public ImageLoader getImageLoader()
	{
		return mImageLoader;
	}
	
	public class FileCache
	{
		public void put(String key, Bitmap image)
		{
			File cacheFile = new File(mCtx.getCacheDir(),new String(key.hashCode()+""));
			try
			{
				image.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(cacheFile,false));
			}
			catch (FileNotFoundException e)
			{}
		}
		
		public Bitmap get(String key)
		{
			File cacheFile = new File(mCtx.getCacheDir(),new String(key.hashCode()+""));
			if(!cacheFile.isFile()||!cacheFile.canRead())
				return null;
			return BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
		}
	}
}
