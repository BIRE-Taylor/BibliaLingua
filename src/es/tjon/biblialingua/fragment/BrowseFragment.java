package es.tjon.biblialingua.fragment;

import android.os.*;
import es.tjon.biblialingua.database.*;
import es.tjon.biblialingua.data.catalog.*;
import es.tjon.biblialingua.data.book.*;
import es.tjon.biblialingua.fragment.BrowseFragment.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.biblialingua.adapter.*;
import android.view.View.*;
import android.view.*;
import android.widget.AdapterView.*;
import android.widget.*;
import es.tjon.biblialingua.*;
import android.support.v4.app.*;
import android.util.Log;

public class BrowseFragment extends ListFragment
{

	private static final String TAG = "es.tjon.biblialingua.BrowseFragment";
	
	private BrowseFragment.BookController mActivity;

	private BookDataContext bdc;

	private static final String BUNDLE_PARENT = "es.tjon.sl.BrowseFragment.parent";

	private boolean mListViewAvailable=false;

	private long mParent=0;

	public boolean isListViewAvailable()
	{
		return mListViewAvailable;
	}

	public void open(Node node)
	{
		mParent=node.getID();
		((BookAdapter)getListAdapter()).setParent(node.getID());
		getListView().setSelection(0);
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		if(savedInstanceState!=null)
			mParent=savedInstanceState.getLong(BUNDLE_PARENT);
		super.onCreate( savedInstanceState );
	}
	
	@Override
	public void start(BrowseActivity activity)
	{
		mActivity = activity;
		new AsyncTask()
		{

			@Override
			protected Object doInBackground(Object[] p1)
			{
				mActivity.mBookPrimary=((BrowseActivity)mActivity).getAppDataContext().getBook(((BrowseActivity)mActivity).getPrimaryLanguage(),((BrowseActivity)mActivity).mUri);
				bdc = new BookDataContext((BrowseActivity)mActivity,mActivity.mBookPrimary);
				return null;
			}
			
			public void onPostExecute(Object object)
			{
				((BrowseActivity)mActivity).getActionBar().setTitle(mActivity.mBookPrimary.name);
				setListAdapter(new BookAdapter((BaseActivity)mActivity,BrowseFragment.this,bdc));
				Log.i(TAG,"Parent "+mParent);
				if(mParent!=0)
				{
					((BookAdapter)getListAdapter()).setParent(mParent);
				}
			}
			
		}.execute();
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		getListView().setOnItemClickListener(new OnItemClickListener()
			{

				@Override
				public void onItemClick(AdapterView<?> p1, View view, int position, long id)
				{
					Node node = (Node)getListAdapter().getItem(position);
					if(node==null)
						return;
					mActivity.onNodeSelected(node);
				}

			});
		super.onViewCreated(view, savedInstanceState);
		mListViewAvailable=true;
	}

	@Override
	public void onResume()
	{
		if(mParent!=0&&getListAdapter()!=null&&getListAdapter().getCount()==1)
			getActivity().onBackPressed();
		super.onResume();
	}
	
	public void onBackPressed()
	{
		Node node=null;
		try
		{
			Long parent=null;
			if(getListAdapter()!=null)
				parent=(((BookAdapter)getListAdapter()).getParent());
			if(parent==null||parent==0)
			{
				getActivity().finish();
				return;
			}
			node = bdc.nodes.getElementByID(parent);
			
		}
		catch (AdaFrameworkException e)
		{}
		if (node == null)
		{
			getActivity().finish();
			return;
		}
		if(node.content==null||node.content.isEmpty())
		{
			((BookAdapter)getListAdapter()).setParent(node.parent_id);
			Long parent=(((BookAdapter)getListAdapter()).getParent());
			node=null;
			if(parent!=null&&parent!=0)
			{
				try
				{
					node = bdc.nodes.getElementByID(parent);
				}
				catch (AdaFrameworkException e)
				{}
			}
			if(node==null)
			{
				getActivity().getActionBar().setTitle(mActivity.mBookPrimary.name);
			}
			else
			{
				getActivity().getActionBar().setTitle(node.short_title);
			}
		}
		else
		{
			getActivity().finish();
			return;
		}
	}
	
	public interface BookController
	{
		Book mBookPrimary;
		
		Node mNode;

		public void onNodeSelected(Node node);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		if(getListAdapter()!=null)
		{
			mParent = ((BookAdapter)getListAdapter()).getParent();
			if(getListAdapter().getCount()==1)
			{
				try
				{
					Node node = bdc.nodes.getElementByID( mParent );
					if(node!=null)
						mParent=node.parent_id;
				}
				catch (AdaFrameworkException e)
				{}
			}
		}
		outState.putLong(BUNDLE_PARENT,mParent);
		Log.i(TAG,"SaveParent "+mParent);
		super.onSaveInstanceState(outState);
	}
	
	
}