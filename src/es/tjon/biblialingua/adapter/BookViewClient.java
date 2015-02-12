package es.tjon.biblialingua.adapter;
import android.content.*;
import android.webkit.*;
import android.widget.*;
import es.tjon.biblialingua.data.book.*;
import es.tjon.biblialingua.database.*;
import es.tjon.biblialingua.utils.*;
import es.tjon.biblialingua.fragment.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.listener.*;
import android.util.*;

public class BookViewClient extends WebViewClient
{

	private BookInterface mContext;

	private boolean mMaster;

	private static final String TAG = "es.tjon.biblialingua.adapter.BookViewClient";

	public BookViewClient(BookInterface c, boolean master)
	{
		mContext = c;
		mMaster = master;
	}

	public void setMaster(boolean isMaster)
	{
		mMaster=isMaster;
	}

	@Override
	public void onPageFinished(WebView view, String url)
	{
		if(view.getOriginalUrl()!=null&&view.getOriginalUrl().contains("http://")&&view.getOriginalUrl().contains("."))
		{
			((BookViewer)view).requestScrollToUri(view.getOriginalUrl(),true);
		}
		else
		{
			((BookViewer)view).requestScrollToUri(url,true);
		}
		super.onPageFinished(view, url);
	}
	
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url)
	{
		Log.d(TAG,"Clicked on "+url);
		return mContext.openUrl(url);
	}


	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		Toast.makeText(mContext, "Oh no! " + description, Toast.LENGTH_SHORT).show();
	}
	
	
}
