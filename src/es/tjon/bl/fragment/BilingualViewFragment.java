package es.tjon.bl.fragment;
import android.graphics.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import es.tjon.bl.*;
import es.tjon.bl.adapter.*;
import es.tjon.bl.data.book.*;
import es.tjon.bl.listener.*;
import android.support.v4.app.*;
import java.net.*;

public class BilingualViewFragment extends Fragment
{

	private static final String TAG = "es.tjon.bl.BilingualViewFragment";
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

	public void scrollTo(double center, double scroll)
	{
		mState.center = center;
		mState.scroll = scroll;
		if (mState.mDisplayPrimary)
			mContentPrimary.scrollTo(center, scroll);
		if (mState.mDisplaySecondary)
			mContentSecondary.scrollTo(center, scroll);
		if (mState.mDisplayRelated)
			mContentRelated.scrollTo(mContentPrimary.findFirstRCAItem());
	}

	public void scrollTo(String uri, double center, float scroll)
	{
		if (mState.mDisplayPrimary)
			mContentPrimary.scrollTo(uri, center, scroll);
		if (mState.mDisplaySecondary)
			mContentSecondary.scrollTo(uri,center, scroll);
		if (mState.mDisplayRelated)
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
		if (savedInstanceState == null)
			savedInstanceState = getArguments();
		if (savedInstanceState != null)
		{
			State state = savedInstanceState.getParcelable(KEY_STATE);
			if (state != null)
				mState = state;
			if (mState == null)
				mState = new State();
		}
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
		if (mState.mSecondaryNode != null && mState.mDisplaySecondary)
		{
			loadSecondary();
		}
		mRelatedView = view.findViewById(R.id.related);
		mContentRelated = (RelatedFragment)((RelatedAdapter)((ListView)mRelatedView.findViewById(android.R.id.list)).getAdapter()).getFragment();
		setDisplayMode(mState.mDisplayPrimary, mState.mDisplaySecondary, mState.mDisplayRelated, null);
		super.onViewCreated(view, savedInstanceState);

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


		public boolean mDisplayPrimary;
		public boolean mDisplaySecondary;
		public boolean mDisplayRelated;

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
			parcel.writeInt(mDisplayPrimary ?0: 1);
			parcel.writeInt(mDisplaySecondary ?0: 1);
			parcel.writeInt(mDisplayRelated ?0: 1);
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
				state.mDisplayPrimary = parcel.readInt() == 1;
				state.mDisplaySecondary = parcel.readInt() == 1;
				state.mDisplayRelated = parcel.readInt() == 1;
				return state;
			}

			@Override
			public BilingualViewFragment.State[] newArray(int size)
			{
				return new State[size];
			}

		};

	}

	public void setDisplayMode(boolean primary, boolean secondary, boolean related, String reference)
	{
		if (mState == null)
			return;
		mState.mDisplayPrimary = primary;
		mState.mDisplaySecondary = secondary;
		mState.mDisplayRelated = related;
		if (mState.mDisplaySecondary && mState.mSecondaryNode == null)
		{
			mState.mDisplaySecondary = false;
		}
		if (mContentPrimary == null || mContentPrimary.getParent() == null)
		{
			Log.i(TAG, "Missing view " + mState.mPrimaryNode.title + (isCreated ?" view created": " view not created"));
			return;
		}
		if (mState.mDisplaySecondary)
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
		if (mState.mDisplayRelated)
		{
			lb.height = (int) (height / 3.0);
			height = (int)(height * (2.0 / 3));
			lb.width = lb.FILL_PARENT;
			mContentRelated.load(mState.mPrimaryNode.uri);

		}
		if (false)// (mVertical)
		{
			if (mState.mDisplaySecondary && mState.mDisplayPrimary)
			{
				ll.height = lr.height = (int)(height / 2.0);

			}
			else if (mState.mDisplayPrimary)
			{
				ll.height = height;
				lr.height = 0;
			}
			else if (mState.mDisplaySecondary)
			{
				ll.height = 0;
				lr.height = height;
			}
		}
		else
		{
			ll.height = lr.height = height;
			if (mState.mDisplaySecondary && mState.mDisplayPrimary)
			{
				ll.width = lr.width = width / 2;
				if (ll.width == 0)
				{
					ll.width = ll.MATCH_PARENT;
				}
			}
			else if (mState.mDisplayPrimary)
			{
				ll.width = ll.MATCH_PARENT;
				lr.width = 0;
			}
			else if (mState.mDisplaySecondary)
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
		if(!mState.mDisplayRelated)
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
																							   {mContentSecondary.onFinishRender(mState.mPrimaryNode.uri);}if (mRef == null&&mState.mDisplayRelated) mContentRelated.scrollTo(mContentPrimary.findFirstRCAItem()); else if(mState.mDisplayRelated) mContentRelated.scrollTo(mRef);}}.setup(mRef));}}.setup(reference));
	}

}
