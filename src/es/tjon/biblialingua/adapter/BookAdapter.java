package es.tjon.biblialingua.adapter;

import android.graphics.*;
import android.os.*;
import android.support.v4.app.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.mobandme.ada.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.data.book.*;
import es.tjon.biblialingua.database.*;
import es.tjon.biblialingua.fragment.*;
import java.util.*;

public class BookAdapter
extends BaseAdapter implements HeaderListView.HeaderAdapter
{

    private BookDataContext mBDC;
    private FragmentActivity mContext;
    private ArrayList<List<Node>> mItems;
    private Long parentID=new Long(0);

	private ListFragment mFragment;

	private boolean mIsTwoLine;

	private SortedMap<String,Integer> index;

	private int mPinnedHeaderBackgroundColor;

	private int mPinnedHeaderTextColor;

    public BookAdapter(BaseActivity context, ListFragment fragment, BookDataContext bookDataContext)
	{
        mContext = context;
		mFragment = fragment;
        mBDC = bookDataContext;
		mPinnedHeaderBackgroundColor = mContext.getResources().getColor(R.color.pinned_header_background);
		mPinnedHeaderTextColor = mContext.getResources().getColor(R.color.pinned_header_text);
        mItems = new ArrayList<List<Node>>();
        update();
    }

    private void update()
	{
		if (Looper.getMainLooper().equals(Looper.myLooper()))
		{
			if (((BrowseFragment)mFragment).isListViewAvailable())
				setListShown(false);
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
			nodes.fill("parent_id=?", new String[]{parentID == null ?"0": parentID.toString()}, "display_order");
			ArrayList<List<Node>> result = new ArrayList<List<Node>>();
			if (nodes.size() > 0 && nodes.get(0).section_name == null)
			{
				result.add(new ArrayList<Node>(nodes));
			}
			else
			{
				String section=null;
				ArrayList<Node> current=null;
				for (Node node:nodes)
				{
					if (section == null || !section.equals(node.section_name))
					{
						if (current != null && !current.isEmpty())
							result.add(current);
						current = new ArrayList<Node>();
						section = node.section_name;
					}
					current.add(node);
				}
				if(current!=null)
					result.add(current);
			}
			Node n = nodes.get(0);
			mIsTwoLine = n != null && n.subtitle != null && !n.subtitle.isEmpty();
			update(result);
        }
        catch (AdaFrameworkException e)
		{
            e.printStackTrace();
        }
    }

	private void setListShown(boolean show)
	{
		try
		{
		//	mFragment.setListShown(show);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{}
	}

	public void update(ArrayList<List<Node>> nodes)
	{
		if (!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			mContext.runOnUiThread(new Runnable()
								   {

									   ArrayList<List<Node>> nodes;

									   public Runnable setup(ArrayList<List<Node>> nodes)
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
		if (((BrowseFragment)mFragment).isListViewAvailable())
			setListShown(true);
		notifyDataSetChanged();
		if (nodes.size() == 1 && nodes.get(0).size() == 1)
		{
			((BrowseFragment.BookController)mContext).onNodeSelected(nodes.get(0).get(0));
		}
	}

    @Override
    public int getCount()
	{
		int count=0;
        if (mItems != null)
		{
			for (List<Node> list:mItems)
				count += list.size();
			return count;
		}
        return 0;
    }

    @Override
    public Node getItem(int position)
	{
        if (mItems == null)
			return null;
        for (List<Node> list:mItems)
		{
			if (position < list.size())
			{
				return list.get(position);
			}
			position -= list.size();
		}
		return null;
    }

    @Override
    public long getItemId(int position)
	{
        if (mItems == null)
			return -1;
		Node item = getItem(position);
        if (item == null)
			return  -2;
		return item.getID();
    }

    public Long getParent()
	{
        return parentID;
    }

    @Override
    public View getView(int n, View view, ViewGroup viewGroup)
	{
		boolean viewTwoLine = (view != null && view.findViewById(android.R.id.text2) != null);
		if (view == null || (mIsTwoLine && viewTwoLine) || (!mIsTwoLine && !viewTwoLine))
		{
        	view = ((LayoutInflater)mContext.getSystemService("layout_inflater")).inflate(mIsTwoLine ?es.tjon.biblialingua.R.layout.list_item_2: es.tjon.biblialingua.R.layout.list_item_1, null);
			if (mIsTwoLine)
			{
				view.findViewById(android.R.id.text1).setPadding(15, 15, 15, 5);
				view.findViewById(android.R.id.text2).setPadding(15, 5, 15, 15);
			}
		}
        Node node = getItem(n);
		if (!mIsTwoLine)
        	((TextView)view.findViewById(android.R.id.text1)).setText(Html.fromHtml(node.title));
		else
		{
			((TextView)view.findViewById(android.R.id.text1)).setText(Html.fromHtml(node.title));
			((TextView)view.findViewById(android.R.id.text2)).setText(Html.fromHtml(node.subtitle));
		}
		bindSectionHeader(view, n);
        return view;
    }

	private void bindSectionHeader(View itemView, int position)
	{
		final TextView headerView = (TextView) itemView.findViewById(R.id.header_text);
		final View dividerView = itemView.findViewById(R.id.list_divider);

		final int section = getSectionForPosition(position);
		if (getPositionForSection(section) == position && mItems.size() != 1)
		{
			String title = mItems.get(section).get(0).section_name;
			headerView.setText(title);
			headerView.setVisibility(View.VISIBLE);
			dividerView.setVisibility(View.GONE);
		}
		else
		{
			headerView.setVisibility(View.GONE);
			dividerView.setVisibility(View.GONE);
		}

		// remove the divider for the last item in a section
		if (getPositionForSection(section + 1) - 1 == position)
		{
			dividerView.setVisibility(View.VISIBLE);
		}
		else
		{
			dividerView.setVisibility(View.GONE);
		}
	}

    public void setParent(Long l)
	{
        parentID = l;
        update();
    }

	@Override
	public int getPinnedHeaderState(int position)
	{
		if (mItems == null || mItems.size() < 2 )
		{
			return PINNED_HEADER_GONE;
		}

		if (position < 0)
		{
			return PINNED_HEADER_GONE;
		}

		// The header should get pushed up if the top item shown
		// is the last item in a section for a particular letter.
		int section = getSectionForPosition(position);
		int nextSectionPosition = getPositionForSection(section + 1);

		if (nextSectionPosition != -1 && position == nextSectionPosition - 1)
		{
			return PINNED_HEADER_PUSHED_UP;
		}

		return PINNED_HEADER_VISIBLE;
	}


	@Override
	public void configurePinnedHeader(View v, int position, int alpha)
	{
		TextView header = (TextView) v;

		final String title = getItem(position).section_name;

		header.setText(title);
		if (alpha == 255)
		{
			header.setBackgroundColor(mPinnedHeaderBackgroundColor);
			header.setTextColor(mPinnedHeaderTextColor);
		}
		else
		{
			header.setBackgroundColor(Color.argb(alpha, 
												 Color.red(mPinnedHeaderBackgroundColor),
												 Color.green(mPinnedHeaderBackgroundColor),
												 Color.blue(mPinnedHeaderBackgroundColor)));
			header.setTextColor(Color.argb(alpha, 
										   Color.red(mPinnedHeaderTextColor),
										   Color.green(mPinnedHeaderTextColor),
										   Color.blue(mPinnedHeaderTextColor)));
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView p1, int p2)
	{
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (view instanceof HeaderListView)
		{
			((HeaderListView) view).configureHeaderView(firstVisibleItem);
		}		
	}

	@Override
	public List<Node>[] getSections()
	{
		if (mItems == null)
			return null;
		return mItems.toArray(new ArrayList[mItems.size()]);
	}

	@Override
	public int getPositionForSection(int section)
	{
		if (mItems == null || mItems.size() <= section)
			return -1;
		int count=0;
		for (int i = 0;i < section;i++)
		{
			count += mItems.get(i).size();
		}
		return count;
	}

	@Override
	public int getSectionForPosition(int position)
	{

		if (mItems == null)
			return -1;
		for (List<Node> list:mItems)
		{
			if (position < list.size())
			{
				return mItems.indexOf(list);
			}
			position -= list.size();
		}
		return -1;
	}

}
