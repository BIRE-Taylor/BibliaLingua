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

	private boolean isCreated=false;
	private boolean mSecondaryLoaded=false;

	public BilingualViewFragment()
	{
		super();
	}

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
			if (mState == null)
				mState = new State();
		}
		Log.i(TAG,"State "+(mState!=null));
		Log.i(TAG, "onCreate " + mState.mPrimaryNode.title);
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
		isCreated = true;
		mContentPrimary = (BookViewer)view.findViewById(R.id.content);
		mViewClientPrimary = new BookViewClient((BookInterface)getActivity(), true);
		mContentPrimary.setWebViewClient(mViewClientPrimary);
		mContentPrimary.loadDataWithBaseURL(BookFragment.BASE_URL, Node.staticGenerateHtmlText(getActivity(), mState.mPrimaryCSS, mState.mPrimaryNode), "text/html", null, BookFragment.BASE_URL + (mState.mUri == null ?mState.mPrimaryNode.uri: mState.mUri));
		mContentSecondary = (BookViewer)view.findViewById(R.id.secondaryContent);
		if (mState.mSecondaryNode != null && BaseActivity.isDisplaySecondary())
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
		super.onResume();
	}

	private void loadSecondary()
	{
		if (mSecondaryLoaded)
			return;
		mSecondaryLoaded = true;
		mViewClientSecondary = new BookViewClient((BookInterface)getActivity(), false);
		mContentSecondary.setWebViewClient(mViewClientSecondary);
		mContentSecondary.loadDataWithBaseURL(BookFragment.BASE_URL, Node.staticGenerateHtmlText(getActivity(), mState.mSecondaryCSS, mState.mSecondaryNode), "text/html", null, BookFragment.BASE_URL + mState.mSecondaryNode.uri);
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

	public void refreshDisplayMode()
	{
		Log.i(TAG,"Setting Display");
		if (mState == null)
		{
			Log.i(TAG,"mState null");
			return;
		}
		boolean dispFirst=mActivity.isDisplayPrimary();
		boolean dispSecond = mActivity.isDisplaySecondary();
		boolean dispRelated = mActivity.isDisplayRelated();
		if (dispSecond && mState.mSecondaryNode == null)
		{
			dispSecond = false;
		}
		if (mContentPrimary == null || mContentPrimary.getParent() == null)
		{
			Log.i(TAG, "Missing view " + mState.mPrimaryNode.title + (isCreated ?" view created": " view not created"));
			return;
		}
		if (dispSecond)
		{
			loadSecondary();
		}
		View content = (View)mContentPrimary.getParent().getParent();
		ViewGroup.LayoutParams ll = mContentPrimary.getLayoutParams();
		ViewGroup.LayoutParams lr = mContentSecondary.getLayoutParams();
		ViewGroup.LayoutParams lb = mRelatedView.getLayoutParams();
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
		if (dispRelated)
		{
			lb.height = (int) (height / 3.0);
			height = (int)(height * (2.0 / 3));
			lb.width = lb.FILL_PARENT;
			mContentRelated.load(mState.mPrimaryNode.uri);

		}
		if (false)// (mVertical)
		{
			if (dispSecond && dispFirst)
			{
				ll.height = lr.height = (int)(height / 2.0);

			}
			else if (dispFirst)
			{
				ll.height = height;
				lr.height = 0;
			}
			else if (dispSecond)
			{
				ll.height = 0;
				lr.height = height;
			}
		}
		else
		{
			ll.height = lr.height = height;
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
		AsyncTask.execute(new Runnable(){String mRef;public Runnable setup(String reference)
														  {mRef = reference;return this;}@Override
														  public void run()
														  {getActivity().runOnUiThread(new Runnable(){String mRef;public Runnable setup(String reference)
																						   {mRef = reference;return this;}@Override
																						   public void run()
																						   { mContentPrimary.onFinishRender(mState.mPrimaryNode.uri); if (mContentSecondary != null)
																							   {mContentSecondary.onFinishRender(mState.mPrimaryNode.uri);}if (mRef == null&&BaseActivity.isDisplayRelated()) mContentRelated.scrollTo(mContentPrimary.findFirstRCAItem()); else if(BaseActivity.isDisplayRelated()) mContentRelated.scrollTo(mRef);}}.setup(mRef));}}.setup(mContentPrimary.findFirstRCAItem()));
	}

}
