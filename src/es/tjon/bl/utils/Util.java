package es.tjon.bl.utils;
import android.app.*;
import android.content.*;
import android.net.*;
import es.tjon.bl.*;
import android.widget.*;
import android.os.*;

public class Util
{
	
	BaseActivity context;
	
	private static Util instance = null;

	private Dialog mLoadingDialog = null;
	
	public static Util getInstance(BaseActivity context)
	{
		if(context==null)
			return instance;
		if(instance==null)
			instance = new Util();
		instance.context = context;
		return instance;
	}

	public boolean isConnected()
	{
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
	
	public boolean isConnectionWithFail()
	{
		if (!isConnected())
		{
			context.runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						new AlertDialog.Builder(context)
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
									context.initialize();
								}
							})
							.show();
					}
					
				
			});
			return false;
		}
		return true;
	}

	public boolean allowMobile()
	{
		return true;
	}

	private Dialog getLoadingDialog()
	{
		if(mLoadingDialog!=null)
			return mLoadingDialog;
		Dialog loading = new Dialog(context);
		loading.setContentView(R.layout.loadingdialog);
		return loading;
	}
	
	public void showLoadingDialog(String text)
	{
		setLoadingDialogText(text);
		getLoadingDialog().show();
	}
	
	public void dismissLoadingDialog()
	{
		if(!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			context.runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						dismissLoadingDialog();
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
	
	public void setLoadingDialogText(CharSequence message)
	{
		if(!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			context.runOnUiThread(new Runnable()
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
						setLoadingDialogText(message);
					}
				}.setup(message));
			return;
		}
		((TextView)getLoadingDialog().findViewById(R.id.loadingdialogTextView)).setText(message);
	}
}
