package es.tjon.bl;
import android.graphics.*;
import android.os.*;
import android.support.v4.view.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import es.tjon.bl.adapter.*;
import es.tjon.bl.data.catalog.*;
import es.tjon.bl.database.*;
import es.tjon.bl.fragment.*;
import es.tjon.bl.listener.*;
import es.tjon.bl.utils.*;

public class BookViewActivity extends BookInterface implements CustomLinkMovementMethod.LinkListener
{
	
	public static final String TAG = "es.tjon.bl.BookViewActovity";
	public static final String KEY_URI = TAG+".uri";
	private static final String BOOKPRIMARYDISPLAY = TAG+".BookPrimaryDisplay";
	private static final String BOOKSECONDARYDISPLAY = TAG+".BookSecondaruDisplay";
	private static final String BOOKRELATEDDISPLAY = TAG+".BookRelatedDisplay";
	private static final String BOOKPRIMARY = TAG+".BookPrimaryFragment";
	private static final String BOOKSECONDARY = TAG+".BookSecondaryFragment";
	private static final String BOOKRELATED = TAG+".BookRelatedFragment";
	
	
	ViewPager mPager;
	Book mBookPrimary;
	Book mBookSecondary;
	String mCurrentUri;
	BilingualViewFragmentAdapter mAdapter;

	private MenuItem mPrimaryMenuItem;
	private MenuItem mSecondaryMenuItem;
	private MenuItem mBilingualMenuItem;
	private MenuItem mRelatedMenuItem;
	
	boolean mFullscreen = false;
	boolean mDisplayPrimary = true;
	boolean mDisplaySecondary = false;
	boolean mDisplayRelated = false;

	private BookDataContext mBDCSecondary;

	private BookDataContext mBDCPrimary;

	public void showProgress(boolean visibility)
	{
		findViewById(R.id.pagerProgressBar).setVisibility(visibility?View.VISIBLE:View.GONE);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager);
		if(savedInstanceState==null)
		{
			mCurrentUri=getIntent().getStringExtra(KEY_URI);
		}
		else
		{
			mCurrentUri=savedInstanceState.getString(KEY_URI);
		}
		if(mCurrentUri==null)
		{
			finish();
			return;
		}
		getBookUtil();
		
		if(mPager==null)
		{
			mPager = (ViewPager)findViewById(R.id.pager);
			mPager.setOffscreenPageLimit(2);
			mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
				{
					@Override
					public void onPageScrolled(int p1, float p2, int p3)
					{}

					@Override
					public void onPageSelected(int position)
					{
						mCurrentUri=mAdapter.getPageUri(position);
						getActionBar().setTitle(mAdapter.getPageTitle(position));
						((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).setDisplayMode(mDisplayPrimary,mDisplaySecondary,mDisplayRelated,null);
					}

					@Override
					public void onPageScrollStateChanged(int p1)
					{}
				});
		}
		start();
  	}
	
	public void start()
	{
		if(Looper.getMainLooper().equals(Looper.myLooper()))
		{
			new AsyncTask()
			{
				
				protected void onPreExecute()
				{
					mAdapter = new BilingualViewFragmentAdapter(getSupportFragmentManager(),BookViewActivity.this);
					mPager.setAdapter(mAdapter);
				}
				
				@Override
				protected Object doInBackground(Object[] p1)
				{
					start();
					return null;
				}
				
				protected void onPostExecute( Object object)
				{
					mAdapter.setUri(mCurrentUri,mPager);
					mAdapter.update(mBDCPrimary,mBDCSecondary,mCurrentUri);
				}
			}.execute();
			return;
		}
		mBookPrimary=getAppDataContext().getBook(getPrimaryLanguage(),mCurrentUri);
		mBDCPrimary=new BookDataContext(this,mBookPrimary);
		mBookSecondary=getAppDataContext().getBook(getSecondaryLanguage(),mCurrentUri);
		if(mBookSecondary==null||!getBookUtil().doesExist(mBookSecondary))
			mBDCSecondary=null;
		else
			mBDCSecondary=new BookDataContext(this,mBookSecondary);
	}
	
	public void onPostCreate(Bundle savedInstanceState)
	{
		View touch = findViewById(R.id.touchScreen);
		touch.setOnTouchListener(new OnTouchListener()
			{
				long lastTouch=0;

				@Override
				public boolean onTouch(View view, MotionEvent ev)
				{
					if (ev.getActionMasked() == ev.ACTION_DOWN && ev.getDownTime() - lastTouch < 200)
					{ 
						lastTouch=0;
						setFullscreen(!mFullscreen);
						return true;
					}
					if (ev.getActionMasked() == ev.ACTION_DOWN)
						lastTouch = ev.getDownTime();
					if(ev.getActionMasked()==ev.ACTION_MOVE)
						lastTouch = 0;
					return false;
				}
			});
		super.onPostCreate(savedInstanceState);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		mPrimaryMenuItem = menu.add(getPrimaryLanguage().name);
		mSecondaryMenuItem = menu.add(getSecondaryLanguage().name);
		mBilingualMenuItem = menu.add("Bilíngüe");
		mRelatedMenuItem = menu.add("Related");
		setupMenu();
		return super.onCreateOptionsMenu(menu);
	}

	private void setupMenu()
	{
		if (mPrimaryMenuItem != null)
			mPrimaryMenuItem.setVisible(mDisplaySecondary);
		if (mSecondaryMenuItem != null)
			mSecondaryMenuItem.setVisible(mDisplayPrimary);
		if (mBilingualMenuItem != null)
			mBilingualMenuItem.setVisible(!(mDisplayPrimary && mDisplaySecondary));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item == null)
			return super.onOptionsItemSelected(item);
		if (item.equals(mRelatedMenuItem))
		{
			setDisplayRelated(!mDisplayRelated);
		}
		else if (item.equals(mBilingualMenuItem))
		{
			setDisplayMode(true, true);
		}
		else if (item.equals(mPrimaryMenuItem))
			setDisplayMode(true, false);
		else if (item.equals(mSecondaryMenuItem))
		{
			setDisplayMode(false, true);
		}
			setupMenu();
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean openUrl(String url)
	{
		if (!url.contains(BookFragment.BASE_URL) && url.charAt(0) != '/')
			return true;
		url = url.replace(BookFragment.BASE_URL, "/");
		if (url.contains("f_"))
		{
			Log.i(TAG,"Open reference "+url);
			setDisplayRelated(true, url.replace("/","").replace("f_", ""));
			return true;
		}
		loadUrl(url);
		return true;
	}

	private void loadUrl(String uri)
	{
		Log.i(TAG,"Load URI "+uri);
		if(uri!=null&&uri.contains(mBookPrimary.gl_uri))
		{
			Log.i(TAG,"Turning to page");
			mCurrentUri=uri;
			mAdapter.setUri(uri,mPager);
			return;
		}
		if(uri!=null)
		{
			Log.i(TAG,"Opening page");
			mCurrentUri=uri;
			start();
		}
	}
	
	private void setDisplayMode(boolean primary, boolean secondary)
	{
		setDisplayMode(primary,secondary,mDisplayRelated, null);
	}

	private void setDisplayRelated(boolean related)
	{
		setDisplayMode(mDisplayPrimary,mDisplaySecondary,related, null);
	}

	private void setDisplayRelated(boolean related, String reference)
	{
		setDisplayMode(mDisplayPrimary,mDisplaySecondary,related, reference);
	}

	public int getTopUiHeight()
	{
		if(mFullscreen)
			return 0;
		View content = findViewById(android.R.id.content);
		Rect rect = new Rect();
		content.getGlobalVisibleRect(rect);
		return rect.top;
	}

	private void setDisplayMode(boolean primary, boolean secondary, boolean related, String reference)
	{
		mDisplayPrimary = primary;
		mDisplaySecondary = secondary;
		mDisplayRelated = related;
		if (!mDisplaySecondary)
			mDisplayPrimary = true;
		if(!mDisplayPrimary)
			mDisplayRelated=false;
		((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).setDisplayMode(mDisplayPrimary,mDisplaySecondary,mDisplayRelated,reference);
		mAdapter.setDisplayMode(mDisplayPrimary,mDisplaySecondary,mDisplayRelated,reference);
	}

	private void setFullscreen(boolean fullscreen)
	{
		if(mFullscreen==fullscreen)
			return;
		mFullscreen = fullscreen;
		if (mFullscreen)
		{
			findViewById(R.id.pager).setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			getActionBar().hide();
		}
		else
		{
			findViewById(R.id.pager).setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			getActionBar().show();
		}
		mPager.dispatchConfigurationChanged(getResources().getConfiguration());
	}

	@Override
	public void scrollTo(double center, double scroll)
	{
		((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).scrollTo(center,scroll);
	}

	@Override
	public String getUri()
	{
		return mCurrentUri;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putString(KEY_URI,mCurrentUri);
		super.onSaveInstanceState(outState);
	}
	
}
