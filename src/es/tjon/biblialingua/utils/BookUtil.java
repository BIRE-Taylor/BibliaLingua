package es.tjon.biblialingua.utils;

import android.content.*;
import android.os.*;
import android.util.*;
import com.mobandme.ada.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.data.catalog.*;
import es.tjon.biblialingua.network.*;
import java.io.*;
import java.util.*;
import android.app.*;
import es.tjon.biblialingua.listener.*;
import es.tjon.biblialingua.database.ApplicationDataContext;

public class BookUtil
{
	Context context;
	private static final String TAG = "es.tjon.biblialingua.util.BookUtil";
	
	ArrayList<ProgressMonitor> monitors=null;
	
	public BookUtil(Context context)
	{
		this.context=context;
	}

	public static boolean doesExist(Folder folder, BaseActivity mContext)
	{
		if(folder==null)
			return false;
		Log.i(TAG,folder.name);
		Book[] books=mContext.getAppDataContext().getBooks(folder);
		for(Book book: books)
		{
			Log.i(TAG,book.name+doesExist(book,mContext));
			if(doesExist(book,mContext))
			{
				return true;
			}
		}
		return false;
	}

	public static boolean doesExist(Book book, Context mContext)
	{
		if(book==null)
			return false;
		File file = getFile(book,mContext);
		return file!=null&&file.exists();
	}

	public void deleteAll( ArrayList<CatalogItem> items )
	{
		ArrayList<Book> books = getBooks( items );
		for(Book book : books)
		{
			if(doesExist(book))
				getFile(book).delete();
		}
	}

	public void requestAll( ArrayList<CatalogItem> items )
	{
		ArrayList<Book> books = getBooks( items );
		for ( Book book : books )
		{
			if(!doesExist(book))
				requestBook(book,null);
		}
	}

	private ArrayList<Book> getBooks( ArrayList<CatalogItem> items )
	{
		ApplicationDataContext adc = null;
			try
			{
				adc = new ApplicationDataContext( context );
			}
			catch (AdaFrameworkException e)
			{return null;}
		ArrayList<Book> books=new ArrayList<Book>( );
		for(CatalogItem item : items)
		{
			if(item instanceof Book)
			{
				books.add((Book)item);
				
			}
			else if(item instanceof Folder)
			{
				Book[] folder = adc.getBooks((Folder)item);
				if(folder!=null)
					books.addAll(Arrays.asList(folder));
			}
		}
		return books;
	}
	
	public void requestBook( Book item, ProgressMonitor callback)
	{
		if(item==null)
			return;
		if(Looper.getMainLooper().equals(Looper.myLooper()))
		{
			AsyncTask<Object,Object,Object> task = new AsyncTask<Object,Object,Object>()
			{

				@Override
				protected Object doInBackground(Object[] p1)
				{
					requestBook((Book)p1[0],(ProgressMonitor)p1[1]);
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
			Intent i = new Intent(context, DownloadService.class);
			i.putExtra(DownloadService.QUEUE_ITEM_ID,item.getID());
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
							monitors=new ArrayList<ProgressMonitor>();
						}
						if(!monitors.contains((ProgressMonitor)object)&&object!=null)
							monitors.add((ProgressMonitor)object);
						if(!mIsBound)
						{
							doBindService();
						}
					}
				}
				
			
		}.setup(callback));
		
	}
	
	public void removeMonitor(ProgressMonitor monitor)
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
		return getFile(book,context);
	}
	
	public static File getDir(Book book, Context context)
	{
		return new File(context.getExternalFilesDir("books"),book.gl_uri+"/");
	}
	
	public static File getFile(Book book, Context context)
	{
		if(book==null||context==null||context.getExternalFilesDir("books")==null||book.language==null)
			return null;
		return new File(context.getExternalFilesDir("books"),book.gl_uri+"/"+book.language.id+"."+book.file.replace("."+book.file_version,""));
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
				case DownloadService.MSG_SET_PROGRESS:
					Pair<Book, Integer> result = (Pair<Book,Integer>)msg.obj;
					if(result==null)
						return;
					for (ProgressMonitor monitor : monitors)
					{
						monitor.onProgress(result.first,result.second);
					}
					break;
				case DownloadService.MSG_NOTIFY_COMPLETE:
					Book book = (Book)msg.obj;
					for (ProgressMonitor monitor : monitors)
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
											 DownloadService.MSG_REGISTER_CLIENT);
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
							   DownloadService.class), mConnection, Context.BIND_AUTO_CREATE);
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
												 DownloadService.MSG_UNREGISTER_CLIENT);
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
