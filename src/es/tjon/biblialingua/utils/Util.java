package es.tjon.biblialingua.utils;
import android.app.*;
import android.content.*;
import android.net.*;
import es.tjon.biblialingua.*;
import android.widget.*;
import android.os.*;
import es.tjon.biblialingua.network.*;

public class Util
{
	
	BaseActivity bContext;
	
	Service sContext;
	
	private static Util instance = null;

	private Dialog mLoadingDialog = null;

	private static Util sInstance = null;

	public static Util getInstance(Service context)
	{
		if(context==null)
			return sInstance;
		if(sInstance==null)
			sInstance = new Util();
		sInstance.sContext = context;
		return sInstance;
	}
	
	public static Util getInstance(BaseActivity context)
	{
		if(context==null)
			return instance;
		if(instance==null)
			instance = new Util();
		instance.bContext = context;
		return instance;
	}

	public boolean isConnected()
	{
		if(bContext==null&&sContext==null)
			return false;
		Context context=bContext==null?sContext:bContext;
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService("connectivity");
        if (connectivityManager == null) return false;
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) return false;
        int n = networkInfo.getType();
        if (n != 1 && n <= 6)
		{
			if (!allowMobile()) return false;
        }
        if (networkInfo.isConnected()) return true;
        if (n != 13) return false;
        return true;
    }
	
	public boolean isConnectionWithFail() throws Exception
	{
		if(bContext==null)
			throw new Exception("Must be called from an Activity context");
		if (!isConnected())
		{
			failInit();
			return false;
		}
		return true;
	}

	public void failInit()
	{
		bContext.runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					new AlertDialog.Builder(bContext)
						.setTitle("Initiatialization failed")
						.setMessage("Please connect to the internet and try again.")
						.setCancelable(false)
						.setNegativeButton("Exit", 
						new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								System.exit(0);
							}
						})
						.setPositiveButton("Retry",
						new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface p1, int p2)
							{
								bContext.initialize();
							}
						})
						.show();
				}


			});
	}

	public boolean allowMobile()
	{
		return true;
	}

	private Dialog getLoadingDialog() throws Exception
	{
		if (bContext == null)
			throw new Exception("Must be called from an Activity context");
		if (mLoadingDialog != null)
			return mLoadingDialog;
		Dialog loading = new Dialog(bContext);
		loading.setContentView(R.layout.loadingdialog);
		return loading;
	}
	
	public void showLoadingDialog(String text) throws Exception
	{
		setLoadingDialogText(text);
		getLoadingDialog().show();
	}
	
	public void dismissLoadingDialog() throws Exception
	{
		if(bContext==null)
			throw new Exception("Must be called from an Activity context");
		if(!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			bContext.runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						try
						{
							dismissLoadingDialog();
						}
						catch (Exception e)
						{}
					}
				});
			return;
		}
		getLoadingDialog().dismiss();
		mLoadingDialog=null;
	}
	
	public void cleanup()
	{
		if(mLoadingDialog!=null)
		{
			if(mLoadingDialog.isShowing())
				mLoadingDialog.dismiss();
			mLoadingDialog=null;
		}
		instance=null;
	}
	
	public void setLoadingDialogText(CharSequence message) throws Exception
	{
		if(bContext==null)
			throw new Exception("Must be called from an Activity context");
		if(!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			bContext.runOnUiThread(new Runnable()
				{

					private CharSequence message;
					
					public Runnable setup(CharSequence message)
					{
						this.message = message;
						return this;
					}

					@Override
					public void run()
					{
						try
						{
							setLoadingDialogText(message);
						}
						catch (Exception e)
						{}
					}
				}.setup(message));
			return;
		}
		((TextView)getLoadingDialog().findViewById(R.id.loadingdialogTextView)).setText(message);
	}
}
