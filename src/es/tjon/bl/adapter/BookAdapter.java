package es.tjon.bl.adapter;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.mobandme.ada.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.bl.data.book.*;
import es.tjon.bl.database.*;
import java.util.*;
import android.text.*;
import es.tjon.bl.fragment.*;
import es.tjon.bl.*;
import android.support.v4.app.*;

public class BookAdapter
extends BaseAdapter
{
    private BookDataContext mBDC;
    private FragmentActivity mContext;
    private ArrayList<Node> mItems;
    private Long parentID;

	private ListFragment mFragment;

    public BookAdapter(BaseActivity context, ListFragment fragment, BookDataContext bookDataContext)
	{
		parentID = new Long(0);
        mContext = context;
		mFragment = fragment;
        mBDC = bookDataContext;
        mItems = new ArrayList<Node>();
        update();
    }

    private void update()
	{
		if(Looper.getMainLooper().equals(Looper.myLooper()))
		{
			if(((BrowseFragment)mFragment).isListViewAvailable())
				mFragment.setListShown(false);
			new AsyncTask()
			{

				@Override
				protected Object doInBackground(Object[] p1)
				{
					update();
					return null;
				}

			}.execute();
			return;
		}
        try
		{
            ObjectSet<Node> nodes = mBDC.nodes;
			nodes.clear();
			nodes.fill("parent_id=?", new String[]{parentID==null?"0":parentID.toString()}, "display_order");
			ArrayList<Node> result = new ArrayList<Node>(nodes);
			update(result);
        }
        catch (AdaFrameworkException e)
		{
            e.printStackTrace();
        }
    }
	
	public void update(ArrayList<Node> nodes)
	{
		if(!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			mContext.runOnUiThread(new Runnable()
			{
				
				ArrayList<Node> nodes;

				public Runnable setup(ArrayList<Node> nodes)
				{
					this.nodes = nodes;
					return this;
				}

					@Override
					public void run()
					{
						update(nodes);
					}
				
			}.setup(nodes));
			return;
		}
		mItems = nodes;
		if(((BrowseFragment)mFragment).isListViewAvailable())
			mFragment.setListShown(true);
		notifyDataSetChanged();
		if(nodes.size()==1)
		{
			((BrowseFragment.BookController)mContext).onNodeSelected(nodes.get(0));
		}
	}

    @Override
    public int getCount()
	{
        if (mItems != null)
			return mItems.size();
        return 0;
    }

    @Override
    public Node getItem(int position)
	{
        if (mItems == null)
			return null;
        if (mItems.size() <= position)
			return null;
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position)
	{
        if (mItems == null)
			return -1;
        if (mItems.size() <= position)
			return -2;
		if (mItems.get(position) == null)
			return -3;
        return (mItems.get(position)).id;
    }

    public Long getParent()
	{
        return parentID;
    }

    @Override
    public View getView(int n, View view, ViewGroup viewGroup)
	{
		if (view == null)
        	view = ((LayoutInflater)mContext.getSystemService("layout_inflater")).inflate(android.R.layout.simple_list_item_1, null);
        Node node = getItem(n);
        ((TextView)view).setText(Html.fromHtml(node.title));
        return view;
    }

    public void setParent(Long l)
	{
        parentID = l;
        update();
    }
}

