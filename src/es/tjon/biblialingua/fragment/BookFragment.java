package es.tjon.biblialingua.fragment;

import android.graphics.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.webkit.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.adapter.*;
import es.tjon.biblialingua.data.book.*;
import es.tjon.biblialingua.data.catalog.*;
import es.tjon.biblialingua.database.*;
import java.util.*;
import android.view.View.*;
import es.tjon.biblialingua.listener.*;
import java.util.prefs.*;
import android.support.v4.app.*;

public class BookFragment extends Fragment
{

	private BookInterface mActivity;

	private Node mNode;
	private String mUri=null;

	private static final String BUNDLE_BOOK="es.tjon.sl.BookFragment.bookid";
	private static final String BUNDLE_URI="es.tjon.sl.BookFragmeny.uri";

	public static final String BASE_URL = "http://sl.tmjon.es/";

	private BookViewer mWebView;
	private boolean mWebViewAvailable = false;

	private boolean mMaster=false;

	private BookViewClient mViewClient;

	private String[] mCSS;

	private boolean mLoad=false;

	
	
	@Override
	public void onAttach(FragmentActivity activity)
	{
		mActivity = (BookInterface) activity;
		super.onAttach(activity);
	}

	@Override
	public BookViewer getWebView()
	{
		return mWebView;
	}
	
	public void setMaster(boolean isMaster)
	{
		mMaster=isMaster;
		if(mViewClient!=null)
		{
			mViewClient.setMaster(isMaster);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if(mWebView!=null)
			mWebView.destroy();
		BookViewer wv = new BookViewer((BookInterface)getActivity());
		mWebView = wv;
		mWebViewAvailable = true;
		if(((BookInterface)getActivity()).getColorScheme().equals("Night"))
			wv.setBackgroundColor(Color.BLACK);
		if(((BookInterface)getActivity()).getColorScheme().equals("Sepia"))
			wv.setBackgroundColor(Color.rgb(250,230,175));
		if (wv.getParent() != null)
		{
			((ViewGroup)wv.getParent()).removeView(wv);
		}
		return wv;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		if(mLoad)
		{
			mLoad=false;
			run();
		}
		if (savedInstanceState != null)
			mUri = savedInstanceState.getString(BUNDLE_URI, null);
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onResume()
	{
		getWebView().onResume();
		super.onResume();
	}

	@Override
	public void onPause()
	{
		getWebView().onPause();
		super.onPause();
	}

	@Override
	public void onDestroy()
	{
		getWebView().destroy();
		super.onDestroy();
	}
	
	public void load(Node node, String[] css)
	{
		mUri = node.uri;
		mNode=node;
		mCSS =css;
		run();
	}

	private void run()
	{
		if(!mWebViewAvailable)
		{
			mLoad=true;
			return;
		}
		if(!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					BookFragment.this.run();
				}

			});
			return;
		}
		mViewClient = new BookViewClient((BookInterface)getActivity(), mMaster);
		getWebView().setWebViewClient(mViewClient);
		getWebView().loadDataWithBaseURL(BASE_URL, Node.staticGenerateHtmlText(getActivity(), mCSS, mNode, false), "text/html", null, BASE_URL+mUri);
	}
	
	public void scrollTo(double l, double t)
	{
		if(mWebView!=null)
			mWebView.scrollTo(l,t);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		if (mNode != null)
			outState.putString(BUNDLE_URI, mNode.uri);
		super.onSaveInstanceState(outState);
	}

	

}
