package es.tjon.bl.utils;

import android.content.*;
import android.os.*;
import android.util.*;
import com.mobandme.ada.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.bl.*;
import es.tjon.bl.data.catalog.*;
import es.tjon.bl.network.*;
import java.io.*;
import java.util.*;
import android.app.*;

public class BookUtil
{
	Context context;
	
	ArrayList<BookDownloadService.ProgressMonitor> monitors=null;
	
	public BookUtil(Context context)
	{
		this.context=context;
	}
	
	public void requestBook( Book item, BookDownloadService.ProgressMonitor callback)
	{
		if(item==null)
			return;
		if(Looper.getMainLooper().equals(Looper.myLooper()))
		{
			AsyncTask task = new AsyncTask()
			{

				@Override
				protected Object doInBackground(Object[] p1)
				{
					requestBook((Book)p1[0],(BookDownloadService.ProgressMonitor)p1[1]);
					return null;
				}

			};
			task.execute(item,callback);
			return;
		}
		DownloadItem newDI = new DownloadItem(item);
		newDI.setStatus(DownloadItem.STATUS_NEW);
		Book second = ((BaseActivity)context).getAppDataContext().getBook(((BaseActivity)context).getSecondaryLanguage(),item.gl_uri);
		ObjectSet<DownloadItem> downloadQueue = ((BaseActivity)context).getAppDataContext().downloadQueue;
		DownloadItem newDISecond = new DownloadItem(second);
		newDISecond.setStatus(DownloadItem.STATUS_NEW);
		try
		{
			if(!((BaseActivity)context).getAppDataContext().hasDownload(newDI))
			{
				downloadQueue.save(newDI);
			}
			if(second!=null&&!((BaseActivity)context).getAppDataContext().hasDownload(newDISecond))
			{
				downloadQueue.save(newDISecond);
			}
			Intent i = new Intent(context, BookDownloadService.class);
			i.putExtra(BookDownloadService.QUEUE_ITEM_ID,item.getID());
			context.startService(i);
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
		}

		((Activity)context).runOnUiThread(new Runnable()
			{
				
				Object object;
				
				public Runnable setup(Object obj)
				{
					object = obj;
					return this;
				}

				@Override
				public void run()
				{
					{
						if(monitors==null)
						{
							monitors=new ArrayList<BookDownloadService.ProgressMonitor>();
						}
						if(!monitors.contains((BookDownloadService.ProgressMonitor)object))
							monitors.add((BookDownloadService.ProgressMonitor)object);
						if(!mIsBound)
						{
							doBindService();
						}
					}
				}
				
			
		}.setup(callback));
		
	}
	
	public void removeMonitor(BookDownloadService.ProgressMonitor monitor)
	{
		if(monitors!=null)
			monitors.remove(monitor);
	}
	
	public boolean doesExist(Book book)
	{
		if(book==null)
			return false;
		return getFile(book).exists();
	}
	
	public File getFile(Book book)
	{
		if(book==null)
			return null;
		return new File(context.getDir("files",0),book.gl_uri+"/"+book.file);
	}
	
	public static File getDir(Book book, Context context)
	{
		return new File(context.getDir("files",0),book.gl_uri+"/");
	}
	
	public static File getFile(Book book, Context context)
	{
		if(book==null)
			return null;
		return new File(context.getDir("files",0),book.gl_uri+"/"+book.file);
	}
	
	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;

	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case BookDownloadService.MSG_SET_PROGRESS:
					Pair<Book, Integer> result = (Pair<Book,Integer>)msg.obj;
					if(result==null)
						return;
					for (BookDownloadService.ProgressMonitor monitor : monitors)
					{
						monitor.onProgress(result.first,result.second);
					}
					break;
				case BookDownloadService.MSG_NOTIFY_COMPLETE:
					Book book = (Book)msg.obj;
					for (BookDownloadService.ProgressMonitor monitor : monitors)
					{
						monitor.onFinish(book);
					}
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	
	
	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
									   IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = new Messenger(service);

			// We want to monitor the service for as long as we are
			// connected to it.
			try {
				Message msg = Message.obtain(null,
											 BookDownloadService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even
				// do anything with it; we can count on soon being
				// disconnected (and then reconnected if it can be restarted)
				// so there is no need to do anything here.
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;

			// As part of the sample, tell the user what happened.
			//Toast.makeText(context, R.string.remote_service_disconnected,
			//			   Toast.LENGTH_SHORT).show();
		}
	};

	void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		context.bindService(new Intent(context, 
							   BookDownloadService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}
	
	public void unBind()
	{
		doUnbindService();
	}

	void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
												 BookDownloadService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service
					// has crashed.
				}
			}

			// Detach our existing connection.
			context.unbindService(mConnection);
			mIsBound = false;
		}
	}
	
}
