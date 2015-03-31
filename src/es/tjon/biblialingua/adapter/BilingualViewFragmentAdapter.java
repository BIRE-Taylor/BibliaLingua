package es.tjon.biblialingua.adapter;
import es.tjon.biblialingua.data.book.*;
import es.tjon.biblialingua.data.catalog.*;
import es.tjon.biblialingua.database.*;
import java.util.*;
import android.os.*;
import es.tjon.biblialingua.fragment.*;
import com.mobandme.ada.exceptions.*;
import java.util.concurrent.*;
import android.support.v4.view.*;
import android.view.*;
import android.util.*;
import es.tjon.biblialingua.*;
import android.support.v4.app.*;

public class BilingualViewFragmentAdapter extends FragmentStatePagerAdapter
{
	private BookDataContext mPrimaryBDC;
	private BookDataContext mSecondaryBDC;
	
	private static final String TAG = "es.tjon.biblialingua.BVFAdapter";
	
	private ArrayList<String> mIndex;
	private SortedMap<String, Node> mPrimaryNodes;
	private SortedMap<String, Node> mSecondaryNodes;
	private SortedMap<String, BilingualViewFragment> mFragments=new ConcurrentSkipListMap<String,BilingualViewFragment>();

	private ViewPager mPager;

	private String mCurrentUri;

	private String[] mSecondaryCSS;

	private String[] mPrimaryCss;
	
	private List<Media> mCurrentMedia;

	private FragmentManager mFragmentManager;

	private BookViewActivity mActivity;

	private boolean mSetUri;

	private BilingualViewFragment mCurrent;

	public BilingualViewFragmentAdapter(FragmentManager fragmentManager, BookViewActivity bva)
	{
		super(fragmentManager);
		mFragmentManager = fragmentManager;
		mActivity = bva;
	}
	
	public List<Media> getCurrentMedia()
	{
		if(mCurrent==null||mCurrentMedia==null||mCurrent.getPrimaryNode()==null||mCurrentMedia.size()==0||!mCurrent.getPrimaryNode().uri.equals(mCurrentMedia.get(0).uri))
			return null;
		return mCurrentMedia;
	}

	public BilingualViewFragment getCurrent()
	{
		return mCurrent;
	}

	public String getPageUri(int position)
	{
		if(mIndex==null)
			return null;
		return mIndex.get(position);
	}

	@Override
	public CharSequence getPageTitle(int position)
	{
		if(mIndex==null||mPrimaryNodes==null)
			return super.getPageTitle(position);
		return mPrimaryNodes.get(mIndex.get(position)).title;
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object)
	{
		mCurrent = (BilingualViewFragment)object;
		mActivity.setBilingual(mCurrent!=null&&mCurrent.getSecondaryNode()!=null);
		mActivity.setMedia(false,false);
		if(mPrimaryBDC!=null&&mCurrent!=null&&mCurrent.getPrimaryNode()!=null)
		{
			AsyncTask.execute(new Runnable()
				{

					@Override
					public void run()
					{
						mCurrentMedia = mPrimaryBDC.getMediaByUrl(mCurrent.getPrimaryNode().uri);
						mActivity.runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									boolean audio=false;
									boolean video=false;
									if(mCurrentMedia==null||mCurrentMedia.isEmpty())
									{
										mActivity.setMedia(false,false);
										return;
									}
									for(Media media : mCurrentMedia)
									{
										if(media.type.equals(media.TYPE_AUDIO_MP3))
											audio=true;
										if(media.type.equals(media.TYPE_VIDEO_MP3U8)||media.type.equals(media.TYPE_VIDEO_MP4))
											video=true;
										if(audio&&video)
											break;
									}
									mActivity.setMedia(audio,video);
								}
						});
					}
				});
		}
		super.setPrimaryItem(container, position, object);
	}

	public void setUri(String currentUri, ViewPager pager)
	{
		if(currentUri==null)
		{
			mSetUri=false;
			return;
		}
		currentUri = currentUri.split("\\.")[0];
		if(mIndex!=null&&mIndex.contains(currentUri))
		{
			pager.setCurrentItem(mIndex.indexOf(currentUri));
			mActivity.getActionBar().setTitle(mPrimaryNodes.get(currentUri).title);
			mSetUri=false;
		}
		else
		{
			mSetUri=true;
			mPager=pager;
			mCurrentUri=currentUri;
		}
	}
	
	public void update(BookDataContext primaryBDC, BookDataContext secondaryBDC, String currentUri)
	{
		if(primaryBDC!=null)
			mPrimaryBDC=primaryBDC;
		if(secondaryBDC!=null)
			mSecondaryBDC=secondaryBDC;
		if(currentUri!=null)
			mCurrentUri=currentUri;
		if(mPrimaryBDC==null)
			Log.e(TAG,"mPrimaryBDC NULL");
		update();
	}

	private ArrayList<String> update()
	{
		if(Looper.getMainLooper().equals(Looper.myLooper()))
		{
			new AsyncTask<Object,Object,ArrayList<String>>()
			{
				
				public void onPreExecute()
				{
					mActivity.showProgress(true);
					mIndex=null;
					notifyDataSetChanged();
	
				}

				@Override
				protected ArrayList<String> doInBackground(Object[] p1)
				{
					return update();
				}
				
				public void onPostExecute(ArrayList<String> object)
				{
					if(object == null)
						return;
					mIndex = object;
					notifyDataSetChanged();
					if(mSetUri)
						setUri(mCurrentUri,mPager);
					mActivity.showProgress(false);
				}
				
			}.execute();
			return null;
		}
		BookDataContext pbdc = mPrimaryBDC;
		BookDataContext sbdc = mSecondaryBDC;
		if(pbdc==null)
			return null;
		String uri = mCurrentUri.substring(0,mCurrentUri.lastIndexOf("/"));
		ArrayList<String> index = new ArrayList<String>();
		mPrimaryNodes = new ConcurrentSkipListMap<String, Node>();
		mSecondaryNodes = new ConcurrentSkipListMap<String, Node>();
		mFragments.clear();
		try
		{
			pbdc.nodes.fill("NOT(content IS NULL OR trim(content) = '') AND uri LIKE '"+uri+"%'",new String[]{},null, null);
			List<Node> nodes = new ArrayList<Node>(mPrimaryBDC.nodes);
			for(Node node : nodes)
			{
				mPrimaryNodes.put(node.uri,node);
				index.add(node.uri);
			}
			if(sbdc!=null)
			{
				try
				{
					sbdc.nodes.fill("NOT(content IS NULL OR trim(content) = '') AND uri LIKE '"+uri+"%'",new String[]{},null, null);
				nodes.clear();
				nodes.addAll(mSecondaryBDC.nodes);
				for(Node node : nodes)
				{
					mSecondaryNodes.put(node.uri,node);
				}
				}
				catch(PopulateObjectSetException e)
				{}
			}
			return index;
		}
		catch (AdaFrameworkException e)
		{}
		return null;
	}
	
	@Override
	public Fragment getItem(int position)
	{
		if(mFragments!=null&&mFragments.containsKey(mIndex.get(position)))
		{
			return mFragments.get(mIndex.get(position));
		}
		BilingualViewFragment fragment = new BilingualViewFragment();
		BilingualViewFragment.State state = new BilingualViewFragment.State();
		state.mPrimaryNode = mPrimaryNodes.get(mIndex.get(position));
		if(mSecondaryNodes!=null)
			state.mSecondaryNode=mSecondaryNodes.get(mIndex.get(position));
		state.mPrimaryCSS=getPrimaryCss();
		state.mSecondaryCSS=getSecondaryCss();
		if(mCurrentUri.contains(state.mPrimaryNode.uri)&&mCurrentUri.contains("."))
			state.mUri=mCurrentUri;
		Bundle args = new Bundle();
		args.putParcelable(BilingualViewFragment.KEY_STATE,state);
		fragment.setArguments(args);
		mFragments.put(mIndex.get(position),fragment);
		return fragment;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		Fragment fragment = (Fragment) super.instantiateItem(container, position);
		if(fragment instanceof BilingualViewFragment)
		{
			String uri = mIndex.get(position);
			mFragments.remove(uri);
			mFragments.put(uri,(BilingualViewFragment)fragment);
		}
		return fragment;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object)
	{
		mFragments.remove(mIndex.get(position));
		super.destroyItem(container, position, object);
	}
	
	

	@Override
	public int getItemPosition(Object object)
	{
		if(mFragments.containsValue(object))
		{
			if(mIndex==null)
				return POSITION_NONE;
			for(String key : mFragments.keySet())
			{
				if(mFragments.get(key).equals(object))
				{
					if(mIndex.contains(key))
						return mIndex.indexOf(key);
					return POSITION_NONE;
				}
			}
		}
		return POSITION_NONE;
	}

	private String[] getPrimaryCss()
	{
		if(mPrimaryCss!=null)
			return mPrimaryCss;
		if(mPrimaryBDC!=null)
			mPrimaryCss=mPrimaryBDC.getCss();
		if(mPrimaryCss==null)
			mPrimaryCss=getSecondaryCss();
		return mPrimaryCss;
	}

	private String[] getSecondaryCss()
	{
		if(mSecondaryCSS!=null)
			return mSecondaryCSS;
		if(mSecondaryBDC!=null)
			mSecondaryCSS=mSecondaryBDC.getCss();
		if(mSecondaryCSS==null)
			mSecondaryCSS=getPrimaryCss();
		return mSecondaryCSS;
	}

	@Override
	public int getCount()
	{
		if(mIndex==null)
			return 0;
		return mIndex.size();
	}

}
