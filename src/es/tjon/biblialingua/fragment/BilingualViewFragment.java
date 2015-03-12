package es.tjon.biblialingua.fragment;
import android.graphics.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.adapter.*;
import es.tjon.biblialingua.data.book.*;
import es.tjon.biblialingua.listener.*;
import android.support.v4.app.*;
import java.net.*;
import android.content.*;
import android.preference.*;
import android.content.res.*;

public class BilingualViewFragment extends Fragment
{

	private static final String TAG = "es.tjon.biblialingua.BilingualViewFragment";
	public static final String KEY_STATE = TAG + ".state";

	private State mState;
	private BookInterface mActivity;

	private BookViewer mContentPrimary;
	private BookViewer mContentSecondary;
	private RelatedFragment mContentRelated;
	private View mRelatedView;

	private BookViewClient mViewClientPrimary;
	private BookViewClient mViewClientSecondary;
	
	private boolean mSecondaryLoaded=false;
	private boolean mStopped=false;

	public void showReference(String uri)
	{
		mContentRelated.scrollTo(uri);
	}

	public void scrollTo(double center, double scroll)
	{
		mState.center = center;
		mState.scroll = scroll;
		if (mActivity.isDisplayPrimary())
			mContentPrimary.scrollTo(center, scroll);
		if (mActivity.isDisplaySecondary())
			mContentSecondary.scrollTo(center, scroll);
		if (mActivity.isDisplayRelated())
			mContentRelated.scrollTo(mContentPrimary.findFirstRCAItem());
	}

	public void scrollTo(String uri, double center, float scroll)
	{
		if (mActivity.isDisplayPrimary())
			mContentPrimary.scrollTo(uri, center, scroll);
		if (mActivity.isDisplaySecondary())
			mContentSecondary.scrollTo(uri,center, scroll);
		if (mActivity.isDisplayRelated())
			mContentRelated.scrollTo(mContentPrimary.findFirstRCAItem());
	}

	@Override
	public void onConfigurationChanged( Configuration newConfig )
	{
		refreshDisplayMode();
		super.onConfigurationChanged( newConfig );
	}
	
	@Override
	public void onAttach(FragmentActivity activity)
	{
		if (activity instanceof BookInterface)
			mActivity = (BookInterface) activity;
		else
			throw new ClassCastException("Activity must be an instance of BookInterface.");
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		if (savedInstanceState == null||!savedInstanceState.containsKey(KEY_STATE))
			savedInstanceState = getArguments();
		if (savedInstanceState != null)
		{
			State state = savedInstanceState.getParcelable(KEY_STATE);
			if (state != null)
				mState = state;
			if (mState==null&&getArguments()!=null)
				getArguments().getParcelable(KEY_STATE);
			if (mState == null)
				mState = new State();
		}
		Log.i(TAG, "onCreate " + mState.mPrimaryNode.title);
		Log.i(TAG,"State "+(mState!=null));
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.main, container, false);
		v.findViewById(R.id.related).setTag("related." + mState.mPrimaryNode.title);
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		mContentPrimary = (BookViewer)view.findViewById(R.id.content);
		mViewClientPrimary = new BookViewClient((BookInterface)getActivity(), true);
		mContentPrimary.setWebViewClient(mViewClientPrimary);
		mContentPrimary.loadDataWithBaseURL(BookFragment.BASE_URL, Node.staticGenerateHtmlText(getActivity(), mState.mPrimaryCSS, mState.mPrimaryNode, false), "text/html", null, BookFragment.BASE_URL + (mState.mUri == null ?mState.mPrimaryNode.uri: mState.mUri));
		mContentSecondary = (BookViewer)view.findViewById(R.id.secondaryContent);
		if (getSecondaryNode() != null && BaseActivity.isDisplaySecondary())
		{
			loadSecondary();
		}
		mRelatedView = view.findViewById(R.id.related);
		mContentRelated = (RelatedFragment)((RelatedAdapter)((ListView)mRelatedView.findViewById(android.R.id.list)).getAdapter()).getFragment();
		refreshDisplayMode();
		super.onViewCreated(view, savedInstanceState);

	}

	@Override
	public void onResume()
	{
		Log.i(TAG,"Resuming state? "+(mState!=null));
		if(mContentPrimary==null)
		{
			Log.i(TAG,"Refinding views");
			mContentPrimary = (BookViewer)getView().findViewById(R.id.content);
			mContentRelated = (RelatedFragment)((RelatedAdapter)((ListView)mRelatedView.findViewById(android.R.id.list)).getAdapter()).getFragment();
			mContentSecondary = (BookViewer)getView().findViewById(R.id.secondaryContent);
		}
		refreshDisplayMode();
		super.onResume();
	}

	private void loadSecondary()
	{
		if (mSecondaryLoaded)
			return;
		mSecondaryLoaded = true;
		mViewClientSecondary = new BookViewClient((BookInterface)getActivity(), false);
		mContentSecondary.setWebViewClient(mViewClientSecondary);
		mContentSecondary.loadDataWithBaseURL(BookFragment.BASE_URL, Node.staticGenerateHtmlText(getActivity(), mState.mSecondaryCSS, getSecondaryNode(), true), "text/html", null, BookFragment.BASE_URL + getSecondaryNode().uri);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putParcelable(KEY_STATE, mState);
		super.onSaveInstanceState(outState);
	}

	public Node getPrimaryNode()
	{
		if(mState==null)
			mState=getArguments().getParcelable(KEY_STATE);
		if(mState==null)
			return null;
		return mState.mPrimaryNode;
	}
	
	public Node getSecondaryNode()
	{
		if(mState==null)
			mState=getArguments().getParcelable(KEY_STATE);
		if(mState==null)
			return null;
		if(mState.mSecondaryNode==null)
			mState=getArguments().getParcelable(KEY_STATE);
		return mState.mSecondaryNode;
	}

	public static class State implements Parcelable
	{
		public double center;
		public double scroll;

		public Node mPrimaryNode;
		public Node mSecondaryNode;

		public String[] mPrimaryCSS;
		public String[] mSecondaryCSS;

		public String mUri;

		@Override
		public int describeContents()
		{
			return 0;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags)
		{

			parcel.writeDouble(center);
			parcel.writeDouble(scroll);
			parcel.writeParcelable(mPrimaryNode, 0);
			parcel.writeParcelable(mSecondaryNode, 0);
			parcel.writeStringArray(mPrimaryCSS);
			parcel.writeStringArray(mSecondaryCSS);
		}

		public static final Parcelable.Creator<State> CREATOR = new Parcelable.Creator<State>()
		{

			@Override
			public BilingualViewFragment.State createFromParcel(Parcel parcel)
			{
				State state = new State();
				state.center = parcel.readDouble();
				state.scroll = parcel.readDouble();
				state.mPrimaryNode = parcel.readParcelable(null);
				state.mSecondaryNode = parcel.readParcelable(null);
				parcel.readStringArray(state.mPrimaryCSS);
				parcel.readStringArray(state.mSecondaryCSS);
				return state;
			}

			@Override
			public BilingualViewFragment.State[] newArray(int size)
			{
				return new State[size];
			}

		};

	}
	
	public boolean isVertical()
	{
		if(getActivity()==null)
			return false;
		boolean pref = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(SettingsFragment.PREFERENCE_DISPLAY,"horizontal").equals("vertical");
		boolean portrait = getActivity().getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT;
		return pref&&portrait;
	}

	@Override
	public void onStop( )
	{
		mStopped=true;
		super.onStop( );
	}
	
	public void refreshDisplayMode()
	{
		if(mStopped||getActivity()==null)
		{
			System.out.println("Not refreshing display mode "+getPrimaryNode().title);
			return;
		}
		if(Looper.getMainLooper().equals(Looper.myLooper()))
		{
			AsyncTask.execute(new Runnable(){

					@Override
					public void run()
					{
						refreshDisplayMode();
					}
				});
		}
		getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					refreshDisplay();
				}
		});
	}

	public void refreshDisplay()
	{
		if(mStopped||getActivity()==null)
		{
			Log.i(TAG,"Not Setting Display "+getPrimaryNode().title);
			return;
		}
		Log.i(TAG,"Setting Display "+getPrimaryNode().title);
		if (getView() == null||mContentPrimary==null)
		{
			Log.i(TAG,"View is null");
			return;
		}
		boolean dispFirst = mActivity.isDisplayPrimary();
		boolean dispSecond = mActivity.isDisplaySecondary();
		boolean dispRelated = mActivity.isDisplayRelated();
		if (dispSecond && getSecondaryNode() == null)
		{
			System.out.println("Disp Second Failed "+getPrimaryNode().title);
			dispSecond = false;
		}
		if (dispSecond)
		{
			loadSecondary();
		}
		View content = getView();
		FrameLayout.LayoutParams ll = (FrameLayout.LayoutParams) mContentPrimary.getLayoutParams();
		FrameLayout.LayoutParams lr = (FrameLayout.LayoutParams) mContentSecondary.getLayoutParams();
		FrameLayout.LayoutParams lb = (FrameLayout.LayoutParams) mRelatedView.getLayoutParams();
		if (content == null)
			return;
		int height = content.getHeight();
		if (height == 0)
		{
			Display display=null;
			if (mActivity != null)
				display = ((WindowManager)mActivity.getSystemService(FragmentActivity.WINDOW_SERVICE)).getDefaultDisplay();
			if (display != null)
			{
				Point size = new Point();
				display.getSize(size);
				height = (size.y - mActivity.getTopUiHeight());
			}
		}
		int width = content.getWidth();
		if (width == 0)
		{
			Display display=null;
			if (mActivity != null)
				display = ((WindowManager)mActivity.getSystemService(FragmentActivity.WINDOW_SERVICE)).getDefaultDisplay();
			if (display != null)
			{
				Point size = new Point();
				display.getSize(size);
				width = (size.x);
			}
		}
		if((getActivity().getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT&&width>height)||(getActivity().getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE&&width<height))
		{
			int x = width;
			width = height;
			height = x;
		}
		if (dispRelated)
		{
			double div = dispFirst&&dispSecond&&isVertical()?4:3;
			lb.height = (int) (height / div);
			height = (int)(height * ((div - 1) / div));
			lb.width = lb.FILL_PARENT;
			mContentRelated.load(mState.mPrimaryNode.uri);

		}
		if (isVertical())
		{
			if (dispSecond && dispFirst)
			{
				ll.height = lr.height = lr.topMargin = (int)((height-2) / 2.0);
				lr.topMargin+=2;
			}
			else if (dispFirst)
			{
				ll.height = dispRelated ? height : ll.FILL_PARENT;
				lr.height = 0;
			}
			else if (dispSecond)
			{
				ll.height = lr.topMargin = 0;
				lr.height = dispRelated ? height : lr.FILL_PARENT;
			}
			ll.width=ll.FILL_PARENT;
			lr.width=lr.FILL_PARENT;
		}
		else
		{
			if(!dispRelated)
				height=ll.MATCH_PARENT;
			ll.height = lr.height = height;
			lr.topMargin = 0;
			if (dispSecond && dispFirst)
			{
				ll.width = lr.width = width / 2;
				if (ll.width == 0)
				{
					ll.width = ll.MATCH_PARENT;
				}
			}
			else if (dispFirst)
			{
				ll.width = ll.MATCH_PARENT;
				lr.width = 0;
			}
			else if (dispSecond)
			{
				ll.width = 0;
				lr.width = lr.MATCH_PARENT;
			}
		}
		if (height == 0)
			ll.height = ll.MATCH_PARENT;
		if (width == 0)
			ll.width = ll.MATCH_PARENT;
		mContentPrimary.setLayoutParams(ll);
		mContentSecondary.setLayoutParams(lr);
		mRelatedView.setLayoutParams(lb);
		if(!dispRelated)
			mRelatedView.setVisibility(View.GONE);
		else
			mRelatedView.setVisibility(View.VISIBLE);
		AsyncTask.execute(new Runnable()
		{
			@Override
			public void run()
			{
				if(getActivity()==null)
					return;
				getActivity().runOnUiThread(new Runnable()
				{
					
					@Override
					public void run()						   
					{
						if(mStopped)
							return;
						if(mContentPrimary!=null&&BaseActivity.isDisplayPrimary())
							mContentPrimary.refreshMaps();
						if(mContentSecondary!=null&&BaseActivity.isDisplaySecondary())
						mContentSecondary.refreshMaps();
						if (BaseActivity.isDisplayRelated()&&mContentPrimary!=null&&mContentRelated!=null)
							mContentRelated.scrollTo(mContentPrimary.findFirstRCAItem()); 
					}
				});
			}
		});
	}

}
