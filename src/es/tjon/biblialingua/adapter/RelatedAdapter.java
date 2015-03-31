package es.tjon.biblialingua.adapter;
import android.view.*;
import android.widget.*;
import es.tjon.biblialingua.data.book.*;
import es.tjon.biblialingua.database.*;
import es.tjon.biblialingua.utils.*;
import java.util.*;
import java.util.concurrent.*;
import android.util.*;
import android.support.v4.app.*;

public class RelatedAdapter extends BaseAdapter
{
	
	private static final String TAG = "es.tjon.biblialingua.adapter.RelatedAdapter";

	private String mUri;
	private BookDataContext mBDC;
	private ArrayList<String> mRefsIndex;
	private ListFragment mFragment;
	private ConcurrentSkipListMap<String, Reference> mRefs;
	
	public RelatedAdapter(ListFragment fragment)
	{
		mFragment=fragment;
	}
	
	public Fragment getFragment()
	{
		return mFragment;
	}

	public int getPosition(String reference)
	{
		if(mRefsIndex==null)
			return -1;
		return mRefsIndex.indexOf(reference);
	}
	
	public void update(BookDataContext bdc, String uri)
	{
		mBDC=bdc;
		if(mUri!=null&&mUri.equals(uri)&&mRefsIndex!=null&&!mRefs.isEmpty())
			return;
		showListView(false);
		mUri=uri;
		if(mRefs==null)
			mRefs=new ConcurrentSkipListMap<String,Reference>();
		if(mRefsIndex==null)
			mRefsIndex=new ArrayList<String>();
		mRefs.clear();
		mRefsIndex.clear();
		List<Reference> list = mBDC.getRefsByUrl(mUri);
		if(list==null)
		{
			notifyDataSetChanged();
			showListView(true);
			return;
		}
		for(Reference ref:list)
		{
			mRefs.put(ref.ref_name,ref);
			mRefsIndex.add(ref.ref_name);
			
		}
		notifyDataSetChanged();
		showListView(true);
	}

	private void showListView(boolean show)
	{
		try
		{
			mFragment.setListShown(show);
		}catch(IllegalStateException e){}
	}

	@Override
	public int getCount()
	{
		if(mRefsIndex==null)
			return 0;
		return mRefsIndex.size();
	}

	public Reference getReference(int position)
	{
		if(mRefsIndex==null||position>=mRefsIndex.size())
			return null;
		return mRefs.get(mRefsIndex.get(position));
	}
	
	@Override
	public Object getItem(int position)
	{
		return getReference(position);
	}

	@Override
	public long getItemId(int position)
	{
		Reference ref = getReference(position);
		if(ref==null)
			return -1;
		return ref._id;
	}

	@Override
	public View getView(int position, View view, ViewGroup group)
	{
		
		TextView tv;
		if(view==null)
			view=((LayoutInflater)mFragment.getActivity().getSystemService(FragmentActivity.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.simple_list_item_1,group,false);
		if(getReference(position)==null)
			return view;
		tv=(TextView) view.findViewById(android.R.id.text1);
		tv.setText(getReference(position).getSpan());
		tv.setMovementMethod(CustomLinkMovementMethod.getInstance(mFragment.getActivity(),(CustomLinkMovementMethod.LinkListener)mFragment.getActivity()));
		return view;
	}
	
}
