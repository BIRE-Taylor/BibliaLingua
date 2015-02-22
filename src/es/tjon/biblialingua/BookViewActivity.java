package es.tjon.biblialingua;
import android.graphics.*;
import android.os.*;
import android.support.v4.view.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import es.tjon.biblialingua.adapter.*;
import es.tjon.biblialingua.data.catalog.*;
import es.tjon.biblialingua.database.*;
import es.tjon.biblialingua.fragment.*;
import es.tjon.biblialingua.listener.*;
import es.tjon.biblialingua.utils.*;
import android.support.v4.app.*;
import android.app.*;
import android.graphics.drawable.*;


public class BookViewActivity extends BookInterface implements CustomLinkMovementMethod.LinkListener, View.OnSystemUiVisibilityChangeListener
{
	
	public static final String TAG = "es.tjon.biblialingua.BookViewActovity";
	public static final String KEY_URI = TAG+".uri";
	private static final String BOOKPRIMARYDISPLAY = TAG+".BookPrimaryDisplay";
	private static final String BOOKSECONDARYDISPLAY = TAG+".BookSecondaryDisplay";
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
	
	boolean mFullscreen = true;

	private BookDataContext mBDCSecondary;

	private BookDataContext mBDCPrimary;

	private GestureDetector mGestureDetector;

	private boolean mLinkClicked=false;

	public void showProgress(boolean visibility)
	{
		findViewById(R.id.pagerProgressBar).setVisibility(visibility?View.VISIBLE:View.GONE);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager);
		setFullscreen(false);
		if(savedInstanceState==null)
		{
			mCurrentUri=getIntent().getStringExtra(KEY_URI);
		}
		else
		{
			mCurrentUri=savedInstanceState.getString(KEY_URI);
			setDisplayMode(savedInstanceState.getBoolean(BOOKPRIMARYDISPLAY),savedInstanceState.getBoolean(BOOKSECONDARYDISPLAY),savedInstanceState.getBoolean(BOOKRELATED));
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
					{
						if(!mFullscreen)
							setFullscreen(true);
					}

					@Override
					public void onPageSelected(int position)
					{
						mCurrentUri=mAdapter.getPageUri(position);
						getActionBar().setTitle(mAdapter.getPageTitle(position));
						((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).refreshDisplayMode();
					}

					@Override
					public void onPageScrollStateChanged(int p1)
					{}
				});
		}
		mPager.setOnSystemUiVisibilityChangeListener(this);
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
					if(mAdapter==null)
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

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev)
	{
		mGestureDetector.onTouchEvent(ev);
		return super.dispatchTouchEvent(ev);
	}
	
	public void onPostCreate(Bundle savedInstanceState)
	{
		mGestureDetector = new GestureDetector(this,new GestureDetector.SimpleOnGestureListener()
			{
				
				@Override
				public boolean onDown(MotionEvent e)
				{
					return true;
				}
				
				@Override
				public boolean onSingleTapUp(MotionEvent e)
				{
					if(mFullscreen==false&&e.getY()<getActionBar().getHeight()*2)
						return true;
					new AsyncTask()
					{
						@Override
						protected Object doInBackground(Object[] p1)
						{
							runOnUiThread(new Runnable(){
									@Override
									public void run()
									{
										if(!mLinkClicked)
											setFullscreen(!mFullscreen);
										mLinkClicked=false;
									}
								});
							return null;
						}
					}.execute();
					return true;
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
			mPrimaryMenuItem.setVisible(isDisplaySecondary());
		if (mSecondaryMenuItem != null)
			mSecondaryMenuItem.setVisible(isDisplayPrimary());
		if (mBilingualMenuItem != null)
			mBilingualMenuItem.setVisible(!(isDisplaySecondary() && isDisplayPrimary()));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		getActionBar().hide();
		setFullscreen(true);
		if (item == null)
			return super.onOptionsItemSelected(item);
		if (item.equals(mRelatedMenuItem))
		{
			setDisplayMode(isDisplayPrimary(),isDisplaySecondary(),!isDisplayRelated());
		}
		else if (item.equals(mBilingualMenuItem))
		{
			setDisplayMode(true, true,isDisplayRelated());
		}
		else if (item.equals(mPrimaryMenuItem))
			setDisplayMode(true, false,isDisplayRelated());
		else if (item.equals(mSecondaryMenuItem))
		{
			setDisplayMode(false, true,isDisplayRelated());
		}
			setupMenu();
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean openUrl(String url)
	{
		mLinkClicked=true;
		setFullscreen(true);
		if (!url.contains(BookFragment.BASE_URL) && url.charAt(0) != '/')
			return true;
		url = url.replace(BookFragment.BASE_URL, "/");
		if (url.contains("f_"))
		{
			Log.i(TAG,"Open reference "+url);
			if(!isDisplayRelated())
				setDisplayMode(isDisplayPrimary(),isDisplaySecondary(),true);
			((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).showReference( url.replace("/","").replace("f_", ""));
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

	public int getTopUiHeight()
	{
		return 0;
	}

	private void setDisplayMode(boolean primary, boolean secondary, boolean related)
	{
		setDisplayPrimary(primary);
		setDisplaySecondary(secondary);
		setDisplayRelated(related);
		if (!isDisplaySecondary())
			setDisplayPrimary(true);
		if(!isDisplayPrimary())
			setDisplayRelated(false);
		if(mPager!=null&&mAdapter!=null)
			((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).refreshDisplayMode();
	}

	private void setFullscreen(boolean fullscreen)
	{
		if(mFullscreen==fullscreen)
			return;
		mFullscreen = fullscreen;
		if(Build.VERSION.SDK_INT<21)
		{
			findViewById(android.R.id.content).setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				);
			if(fullscreen)
			{
				findViewById(android.R.id.content).setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_FULLSCREEN
				);
			}
			else
			{
				findViewById(android.R.id.content).setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				);
			}
			return;
		}
		if (mFullscreen)
		{
			findViewById(android.R.id.content).setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
				| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
				| View.SYSTEM_UI_FLAG_IMMERSIVE);
		}
		else
		{
			findViewById(android.R.id.content).setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		}
	}
	
	@Override
	public void onSystemUiVisibilityChange(int visibility)
	{
		if((visibility&View.SYSTEM_UI_FLAG_FULLSCREEN)==View.SYSTEM_UI_FLAG_FULLSCREEN)
		{
			getActionBar().hide();
		}
		else
		{
			getActionBar().show();
		}
	}

	@Override
	public void scrollTo(String url, double center, float scroll)
	{
		if(!mFullscreen)
			setFullscreen(true);
		((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).scrollTo(url,center,scroll);
	}

	@Override
	public void scrollTo(double center, double scroll)
	{
		if(!mFullscreen)
			setFullscreen(true);
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
