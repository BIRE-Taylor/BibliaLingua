package es.tjon.biblialingua.network;
import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import com.mobandme.ada.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.data.catalog.*;
import es.tjon.biblialingua.database.*;
import es.tjon.biblialingua.utils.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import es.tjon.biblialingua.listener.*;
import android.widget.*;
import android.preference.PreferenceManager;
import es.tjon.biblialingua.fragment.SettingsFragment;

public class DownloadService extends Service implements ProgressMonitor
{

	private static final int NOTIFICATION = 995678;

	private static final int NOTIFICATION_COMPLETE = 995679;
	
	private static final int NOTIFICATION_ERROR = 995680;
	
	private ArrayList<Book> completedBooks = new ArrayList<Book>();
	
	boolean running=false;

	private ApplicationDataContext adc;

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

	private ArrayList<DownloadTask> mDownloadQueue;

	private long time=0;

	private boolean empty;

	public static final String CHECK_UPDATES = "CHECKUPDATE";
	int updating = 0;

	public void finishedUpdating( UpdateTask task )
	{
		updating-=1;
	}

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

	public void onProgress(Book item, int progress)
	{
		if (mProgress==null||mProgress.second != progress)
		{
			mProgress = new Pair<Book,Integer>( item, progress );
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

	public void onFinish(Book item)
	{
		adc.downloadComplete(item);
		adc.queueProcessing(item);
		mDownloadQueue.remove(0);
		onProgress(null,0);
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
		if(isDone())
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
		showNotification();
	}
	
	public boolean isDone()
	{
		return mDownloadQueue.isEmpty()&&updating<=0;
	}
	
	public void notifyError(Book item)
	{
		Notification notification = new Notification.Builder(DownloadService.this)
			.setContentTitle("Downloading book failed")
			.setContentText(item.name)
			.setContentIntent(PendingIntent.getService(DownloadService.this,item.id,new Intent(DownloadService.this,DownloadService.class),PendingIntent.FLAG_UPDATE_CURRENT))
			.setSmallIcon(android.R.drawable.stat_notify_error)
			.build();

		NotificationManager nm = ((NotificationManager)getSystemService(NOTIFICATION_SERVICE));
		nm.notify(NOTIFICATION_ERROR, notification);
		stopForeground(true);
		nm.cancel(NOTIFICATION);
		stopSelf();
	}

	@Override
	public void onCreate()
	{
		try
		{
			if ( adc == null )
				adc = new ApplicationDataContext( this );
		} catch (AdaFrameworkException e)
		{}
		mClients = new ArrayList<Messenger>( );
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
        CharSequence text = "Downloading books...";
		String bigText = "";
		for(DownloadTask task : mDownloadQueue)
		{
			bigText += task.getName()+"\n";
		}
		String bookTitle =getProgress() != null && getProgress().first != null ?getProgress().first.getName(): "";

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
																new Intent(this, CatalogActivity.class), 0);
		int progress = getProgress() == null ?0: getProgress().second;
		Notification notification = new Notification.BigTextStyle(new Notification.Builder(this)
			.setContentTitle("Downloading " + bookTitle)
			.setContentText(text)
			.setContentInfo(mDownloadQueue.size() + " books left")
			.setProgress(100, progress, progress == 0 ||progress == 100?true: false)
			.setContentIntent(contentIntent)
			.setSmallIcon(android.R.drawable.stat_sys_download))
			.bigText(bigText)
			.setSummaryText(text)
			.build();

		startForeground(NOTIFICATION, notification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		try
		{
			if ( adc == null )
				adc = new ApplicationDataContext( this );
		} catch (AdaFrameworkException e)
		{}
		if ( !Util.getInstance( this ).isConnected( ) )
		{
			Toast.makeText(this,"Internet required to download or update.",Toast.LENGTH_SHORT).show();
			if(running&&exec!=null)
			{
				exec.shutdown();
			}
			stopSelf();
			return START_NOT_STICKY;
		}
		if (!running)
		{
			running = true;
			exec = Executors.newSingleThreadExecutor();
		}
		if(intent!=null)
		{
			if(intent.getBooleanExtra(CHECK_UPDATES,false))
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
				updating+=2;
				new UpdateTask( this, adc.getLanguage( prefs.getString( SettingsFragment.PREFERENCE_PRIMARY_LANGUAGE, "-1" ) ) ).check( );
				new UpdateTask( this, adc.getLanguage( prefs.getString( SettingsFragment.PREFERENCE_SECONDARY_LANGUAGE, "-1" ) ) ).check( );
			}
			checkNewDownloads(intent.getLongExtra(QUEUE_ITEM_ID,-1));
		}
		else
			checkNewDownloads(-1);
		return START_STICKY;
	}

	private void checkNewDownloads(long id)
	{
		try
		{
			ObjectSet<DownloadItem> queue = adc.downloadQueue;
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
