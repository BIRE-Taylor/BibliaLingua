package es.tjon.bl.utils;
import es.tjon.bl.*;
import android.support.v4.content.*;
import android.content.*;
import android.preference.*;
import android.os.*;

public class UpdateUtil
{
	
	private static BaseActivity mContext;
	
	public static void initialize(BaseActivity context)
	{
		mContext=context;
		AsyncTask.execute(new Updater());
	}
	
	public static class Updater implements Runnable
	{
		@Override
		public void run()
		{
			boolean updatePrimary = mContext.getLastUpdate(mContext.getPrimaryLanguage())<System.currentTimeMillis()-6*24*60*60*1000;
			boolean updateSecondary = mContext.getLastUpdate(mContext.getSecondaryLanguage())<System.currentTimeMillis()-6*24*60*60*1000;
			mContext.updateInitialized();
		}
	}
}
