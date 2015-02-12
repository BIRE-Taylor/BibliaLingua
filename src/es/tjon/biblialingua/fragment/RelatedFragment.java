package es.tjon.biblialingua.fragment;


import android.os.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.data.book.*;
import es.tjon.biblialingua.database.*;
import es.tjon.biblialingua.adapter.*;
import es.tjon.biblialingua.data.catalog.*;
import android.widget.*;
import android.view.*;
import android.database.*;
import android.util.*;
import android.support.v4.app.*;

public class RelatedFragment extends ListFragment
{
	
	private static final String TAG = "es.tjon.biblialingua.fragment.RelatedFragment";

	private BaseActivity mActivity;
	private BookDataContext mBDC;

	private RelatedAdapter mRA;

	private String mUri;
	private String mReference;

	private boolean mLoadPending=false;

	private static final String URI="RF.uri";

	private boolean mListViewAvailable=false;

	private boolean mScrollPending=false;

	private Toast mToast;

	private boolean loadPending;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		mRA=new RelatedAdapter(this);
		setListAdapter(mRA);
		if(mLoadPending)
		{
			mRA.update(mBDC,mUri);
		}
		if(savedInstanceState!=null)
		{
			load(savedInstanceState.getString(URI,null));
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		mListViewAvailable=true;
	}
	
	@Override
	public void onAttach(FragmentActivity activity)
	{
		mActivity = (BaseActivity)activity;
		super.onAttach(activity);
		if(loadPending)
			load(mUri);
	}
	
	public void load(String uri)
	{
		if(mActivity==null&&getParentFragment()!=null)
			mActivity=(BaseActivity) getParentFragment().getActivity();
		Log.i(TAG,"Load "+uri);
		loadPending=false;
		if(uri==null)
			return;
		if(mBDC==null||!uri.contains(mBDC.getUri()))
		{
			if(mActivity==null)
			{
				mUri=uri;
				loadPending=true;
				return;
			}
			Book book = mActivity.getAppDataContext().getBook(mActivity.getPrimaryLanguage(), uri);
			Log.i(TAG,(book!=null)?"Book "+book.name:"Bppk null");
			if(book==null)
				return;
			BookDataContext BDC = new BookDataContext(getActivity(), book);
			if(BDC==null)
				return;
			mBDC=BDC;
		}
		if(mUri==null||!mUri.contains(uri))
			mUri=uri;
		if(mRA==null)
			mLoadPending=true;
		else
		{
			mLoadPending=false;
			mRA.update(mBDC,mUri);
		}
	}
	
	public boolean isListViewAvailable()
	{
		return mListViewAvailable;
	}
	
	public void scrollTo(String reference)
	{
		scrollTo(reference,false);
	}
	
	public void scrollTo(String reference, boolean select)
	{
		if(mReference==null||!mReference.equals(reference))
		{
			mReference=reference;
			if(mListViewAvailable)
			{
				mScrollPending=false;
				int pos= mRA.getPosition(mReference);
				getListView().smoothScrollToPositionFromTop(pos,0);
				if(select)
					getListView().setSelection(pos);
			}
			else
			{
				mScrollPending=true;
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		if(mUri!=null)
			outState.putString(URI,mUri);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy()
	{
		if(mToast!=null)
			mToast.cancel();
		super.onDestroy();
	}
	
	


}