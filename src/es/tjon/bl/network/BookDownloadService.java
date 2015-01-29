package es.tjon.bl.network;
import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import com.mobandme.ada.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.bl.*;
import es.tjon.bl.data.catalog.*;
import es.tjon.bl.database.*;
import es.tjon.bl.utils.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class BookDownloadService extends Service
{

	private static final int NOTIFICATION = 995678;

	private static final int NOTIFICATION_COMPLETE = 995679;
	
	private static final int NOTIFICATION_ERROR = 995680;
	
	private ArrayList<Book> completedBooks = new ArrayList<Book>();
	
	boolean running=false;

	private ApplicationDataContext adc;

	private ObjectSet<DownloadItem> queue;

	private ExecutorService exec;

	public static final int MSG_REGISTER_CLIENT = 2;

	public static final int MSG_REQUEST_PROGRESS = 3;

	public static final int MSG_NOTIFY_COMPLETE = 5;

	public static final int MSG_SET_PROGRESS =4;

	public static final int MSG_UNREGISTER_CLIENT = 1;
	
	public static final String QUEUE_ITEM_ID = "ITEMID";

	public ArrayList<Messenger> mClients;

	final Messenger mMessenger = new Messenger(new IncomingHandler());

	private Pair<Book,Integer> mProgress;

	private ArrayList<BookDownloadService.DownloadTask> mDownloadQueue;

	private long time=0;

	private boolean empty;

	@Override
	public IBinder onBind(Intent p1)
	{
		return mMessenger.getBinder();
	}

	class IncomingHandler extends Handler
	{
        @Override
        public void handleMessage(Message msg)
		{
            switch (msg.what)
			{
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_REQUEST_PROGRESS:
                    for (int i=mClients.size() - 1; i >= 0; i--)
					{
                        try
						{
							Message m = Message.obtain(null, MSG_SET_PROGRESS);
							m.obj = getProgress();
                            mClients.get(i).send(m);
                        }
						catch (RemoteException e)
						{
                            mClients.remove(i);
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
	}

	private Pair<Book,Integer> getProgress()
	{
		return mProgress;
	}

	private void setProgress(Pair<Book,Integer> progress)
	{
		if (mProgress==null||mProgress.second != progress.second)
		{
			mProgress = progress;
	 		showNotification();
			for (int i=mClients.size() - 1; i >= 0; i--)
			{
				try
				{
					Message m = Message.obtain(null, MSG_SET_PROGRESS);
					m.obj = getProgress();
					mClients.get(i).send(m);
				}
				catch (RemoteException e)
				{
					// The client is dead.  Remove it from the list;
					// we are going through the list from back to front
					// so this is safe to do inside the loop.
					mClients.remove(i);
				}
			}
		}
	}

	public void notifyComplete(Book item)
	{

		completedBooks.add(item);
		
		String notificationText="";
		
		for(Book current : completedBooks)
		{
			notificationText+=current.name+"\n";
		}
		
		Notification notification = new Notification.Builder(this)
			.setContentTitle("Download Complete")
			.setContentText(notificationText)
			.setContentInfo(completedBooks.size() + " books")
			.setContentIntent(PendingIntent.getActivity(this,item.id,new Intent(this,CatalogActivity.class),PendingIntent.FLAG_UPDATE_CURRENT))
			.setSmallIcon(android.R.drawable.stat_sys_download_done)
			.build();
			
		NotificationManager nm = ((NotificationManager)getSystemService(NOTIFICATION_SERVICE));
		nm.notify(NOTIFICATION_COMPLETE, notification);
		if(mDownloadQueue.isEmpty())
			nm.cancel(NOTIFICATION);

		for (int i=mClients.size() - 1; i >= 0; i--)
		{
			try
			{
				Message m = Message.obtain(null, MSG_NOTIFY_COMPLETE);
				m.obj = item;
				mClients.get(i).send(m);
			}
			catch (RemoteException e)
			{
				// The client is dead.  Remove it from the list;
				// we are going through the list from back to front
				// so this is safe to do inside the loop.
				mClients.remove(i);
			}
		}
	}

	@Override
	public void onCreate()
	{

		mClients = new ArrayList<Messenger>();
		mDownloadQueue = new ArrayList<DownloadTask>();
		showNotification();
		super.onCreate();
	}

	private void showNotification()
	{
		if(mDownloadQueue.size()==0)
		{
			if(empty=true)
			{
				stopSelf();
				return;
			}
			empty=true;
			stopForeground(true);
			checkNewDownloads(-1);
			return;
		}
		empty=false;
		// In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Downloading books...";
		String bookTitle =getProgress() != null && getProgress().first != null ?getProgress().first.getName(): "";

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
																new Intent(this, CatalogActivity.class), 0);
		int progress = getProgress() == null ?0: getProgress().second;
		Notification notification = new Notification.Builder(this)
			.setContentTitle("Downloading " + bookTitle)
			.setContentText(text)
			.setContentInfo(mDownloadQueue.size() + " books left")
			.setProgress(100, progress, progress == 0 ||progress == 100?true: false)
			.setContentIntent(contentIntent)
			.setSmallIcon(android.R.drawable.stat_sys_download)
			.build();

		startForeground(NOTIFICATION, notification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (!running)
		{
			running = true;
			exec = Executors.newSingleThreadExecutor();
		}
		if(intent!=null)
			checkNewDownloads(intent.getLongExtra(QUEUE_ITEM_ID,-1));
		else
			checkNewDownloads(-1);
		return START_STICKY;
	}

	private void checkNewDownloads(long id)
	{
		try
		{
			if (adc == null)
				adc = new ApplicationDataContext(this);
			queue = adc.downloadQueue;
			queue.fill("time");
			if (mDownloadQueue.isEmpty())
			{
				ArrayList<DownloadTask> dQueue = new ArrayList<DownloadTask>();
				for (DownloadItem item:queue)
				{
					if(item.time>time)
						time=item.time;
					dQueue.add(new DownloadTask(this, item.item));
				}
				this.mDownloadQueue.addAll(dQueue);
				exec.invokeAll(dQueue);
			}
			else
			{
				long newTime=0;
				for (DownloadItem item:queue)
				{
					if(item.time>time)
					{
						if(item.time>newTime)
							newTime=item.time;
						DownloadTask newdt = new DownloadTask(this, item.item);
						mDownloadQueue.add(newdt);
						exec.submit(newdt);
					}
				}
				if(newTime>time)
					time=newTime;
			}
			showNotification();
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
			return;
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
			return;
		}
	}

	public class DownloadTask implements Callable<Pair<Book,Boolean>>
	{

		Context context;
		Book item;

		public DownloadTask(Context c, Book item)
		{
			context = c;
			this.item = item;
		}

		@Override
		public Pair<Book,Boolean> call()
		{
			try
			{
				File temp = downloadFile();
				if (temp != null)
				{
					try
					{
						BookUtil.getDir(item,BookDownloadService.this).mkdirs();
						File book = BookUtil.getFile(item, BookDownloadService.this);
						ZLib.decompressFile(temp, book);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						return null;
					}
				}
				else
				{
					stopForeground(true);
					stopSelf();
					return null;
				}
				if(temp!=null)
					temp.delete();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
			adc.downloadComplete(item);
			mDownloadQueue.remove(this);
			setProgress(null);
			notifyComplete(item);
			showNotification();
			return new Pair<Book,Boolean>(item, false);
		}

		File downloadFile() throws IOException
		{
			int bytesWritten = 0;
			URL url = new URL(item.url);
			HttpURLConnection conn=null;
			BufferedInputStream in=null;
			try
			{
			 conn = (HttpURLConnection) url.openConnection();
			 in = new BufferedInputStream(url.openStream());
			}
			catch(UnknownHostException e)
			{
				Notification notification = new Notification.Builder(BookDownloadService.this)
					.setContentTitle("Download Failed")
					.setContentText(item.name)
					.setContentIntent(PendingIntent.getService(BookDownloadService.this,item.id,new Intent(BookDownloadService.this,BookDownloadService.class),PendingIntent.FLAG_UPDATE_CURRENT))
					.setSmallIcon(android.R.drawable.stat_notify_error)
					.build();

				NotificationManager nm = ((NotificationManager)getSystemService(NOTIFICATION_SERVICE));
				nm.notify(NOTIFICATION_ERROR, notification);
				nm.cancel(NOTIFICATION);
				stopForeground(true);
				BookDownloadService.this.stopSelf();
				return null;
			}
			File tempFile = File.createTempFile(item.name, null, getFilesDir());
			OutputStream out = new FileOutputStream(tempFile);
			byte[] TEMP = new byte[1024];
			int size = conn.getContentLength();
			int read=0;
			while ((read = in.read(TEMP)) != -1)
			{
				out.write(TEMP, 0, read);
				bytesWritten += read;
				try
				{
				publishProgress(bytesWritten * 100.0 / size);
				}catch(Exception e)
				{
					e.printStackTrace(System.err);
				}
			}
			out.flush();
			out.close();
			in.close();
			conn.disconnect();
			return tempFile;
		}

		private void publishProgress(double progress)
		{
			setProgress(new Pair<Book,Integer>(item, (int)progress));
		}


	}

	public interface ProgressMonitor
	{
		public void onProgress(Book book, int progress);
		public void onFinish(Book book);
	}

	@Override
	public void onDestroy()
	{
		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION);
		if(exec!=null)
		{
			exec.shutdown();
			exec.shutdownNow();
		}
		super.onDestroy();
	}
	
	
}
