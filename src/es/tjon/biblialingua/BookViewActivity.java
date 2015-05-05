package es.tjon.biblialingua;
import android.content.*;
import android.content.res.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.support.v4.view.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import es.tjon.biblialingua.adapter.*;
import es.tjon.biblialingua.data.book.*;
import es.tjon.biblialingua.data.catalog.*;
import es.tjon.biblialingua.database.*;
import es.tjon.biblialingua.fragment.*;
import es.tjon.biblialingua.listener.*;
import es.tjon.biblialingua.utils.*;
import java.util.*;
import android.graphics.*;
import android.content.pm.*;
import org.jsoup.*;


public class BookViewActivity extends BookInterface implements CustomLinkMovementMethod.LinkListener, View.OnSystemUiVisibilityChangeListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener
{

	public static final String TAG = "es.tjon.biblialingua.BookViewActivity";
	public static final String KEY_URI = TAG + ".uri";
	private static final String BOOKPRIMARYDISPLAY = TAG + ".BookPrimaryDisplay";
	private static final String BOOKSECONDARYDISPLAY = TAG + ".BookSecondaryDisplay";
	private static final String BOOKRELATED = TAG + ".BookRelatedFragment";


	ViewPager mPager;
	Book mBookPrimary;
	Book mBookSecondary;
	String mCurrentUri;
	BilingualViewFragmentAdapter mAdapter;

	private SubMenu mDisplayMenuItem;
	private MenuItem mPrimaryMenuItem;
	private MenuItem mSecondaryMenuItem;
	private MenuItem mBilingualMenuItem;
	private MenuItem mRelatedMenuItem;
	private SubMenu mMediaMenuItem;
	private MenuItem mListen;
	private MenuItem mListenInstrumental;
	private MenuItem mWatch;

	boolean mFullscreen = true;

	private BookDataContext mBDCSecondary;

	private BookDataContext mBDCPrimary;

	private GestureDetector mGestureDetector;

	private boolean mLinkClicked=false;

	private boolean mPaused=false;

	private MediaUtil mMedia;

	private MediaController mMediaController;

	private boolean mBilingual=false;

	private VideoView mVideoView;

	private boolean mWatching=false;

	private boolean mListening=false;

	private boolean mLockFullscreen=false;

	public void showProgress(boolean visibility)
	{
		findViewById(R.id.pagerProgressBar).setVisibility(visibility ?View.VISIBLE: View.GONE);
	}

	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager);
		setFullscreen(false);
		if (savedInstanceState == null)
		{
			mCurrentUri = getIntent().getStringExtra(KEY_URI);
		}
		else
		{
			mCurrentUri = savedInstanceState.getString(KEY_URI);
			setDisplayMode(savedInstanceState.getBoolean(BOOKPRIMARYDISPLAY), savedInstanceState.getBoolean(BOOKSECONDARYDISPLAY), savedInstanceState.getBoolean(BOOKRELATED));
		}
		if (mCurrentUri == null)
		{
			finish();
			return;
		}
		getBookUtil();
		mVideoView = (VideoView)findViewById(R.id.pagerVideoView);
		if (mPager == null)
		{
			mPager = (ViewPager)findViewById(R.id.pager);
			mPager.setOffscreenPageLimit(2);
			mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
				{
					@Override
					public void onPageScrolled(int p1, float p2, int p3)
					{
						if (!mFullscreen)
							setFullscreen(true);
					}

					@Override
					public void onPageSelected(int position)
					{
						mCurrentUri = mAdapter.getPageUri(position);
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
	
	public static String html2text(String html) {
		return Jsoup.parse(html).text();
	}

	@Override
	public void setTitle(CharSequence title)
	{
		super.setTitle(html2text(title.toString()));
	}

	public void start()
	{
		if (Looper.getMainLooper().equals(Looper.myLooper()))
		{
			new AsyncTask()
			{

				protected void onPreExecute()
				{
					if (mAdapter == null)
						mAdapter = new BilingualViewFragmentAdapter(getSupportFragmentManager(), BookViewActivity.this);
					mPager.setAdapter(mAdapter);
					getBookUtil();
				}

				@Override
				protected Object doInBackground(Object[] p1)
				{
					start();
					return null;
				}

				protected void onPostExecute(Object object)
				{
					if (mPaused)
						return;
					getActionBar().setSubtitle(mBookPrimary.name);
					mAdapter.setUri(mCurrentUri, mPager);
					mAdapter.update(mBDCPrimary, mBDCSecondary, mCurrentUri);
				}
			}.execute();
			return;
		}
		if (mPaused)
			return;
		mBookPrimary = getAppDataContext().getBook(getPrimaryLanguage(), mCurrentUri);
		if (mBookPrimary == null)
			return;
		mBDCPrimary = new BookDataContext(this, mBookPrimary);
		mBookSecondary = getAppDataContext().getBook(getSecondaryLanguage(), mCurrentUri);
		if (mBookSecondary == null || getBookUtil() == null || !getBookUtil().doesExist(mBookSecondary))
			mBDCSecondary = null;
		else
			mBDCSecondary = new BookDataContext(BookViewActivity.this, mBookSecondary);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev)
	{
		mGestureDetector.onTouchEvent(ev);
		return super.dispatchTouchEvent(ev);
	}

	public void onPostCreate(Bundle savedInstanceState)
	{
		mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener()
			{

				@Override
				public boolean onDown(MotionEvent e)
				{
					return true;
				}

				@Override
				public boolean onSingleTapUp(MotionEvent e)
				{
					if (mFullscreen == false && e.getY() < getActionBar().getHeight() * 2)
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
										if (!mLinkClicked)
											setFullscreen(!mFullscreen);
										mLinkClicked = false;
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
	protected void onResume()
	{
		mPaused = false;
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		mPaused = true;
		if (mMedia != null)
			mMedia.release();
		mMedia = null;
		mMediaController = null;
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		mDisplayMenuItem = menu.addSubMenu("Display");
		mPrimaryMenuItem = mDisplayMenuItem.add(getPrimaryLanguage().name);
		mSecondaryMenuItem = mDisplayMenuItem.add(getSecondaryLanguage().name);
		mBilingualMenuItem = mDisplayMenuItem.add("Bilíngüe");
		mPrimaryMenuItem.setVisible(mBilingual);
		mSecondaryMenuItem.setVisible(mBilingual);
		mBilingualMenuItem.setVisible(mBilingual);
		mMediaMenuItem = menu.addSubMenu("Media");
		mListen = mMediaMenuItem.add("Listen");
		mListenInstrumental = mMediaMenuItem.add("Instrumental");
		mWatch = mMediaMenuItem.add("Watch");
		mRelatedMenuItem = mDisplayMenuItem.add("Related");
		setupMenu();
		return super.onCreateOptionsMenu(menu);
	}



	public void setBilingual(boolean secondary)
	{
		mBilingual = secondary;
		setupMenu();
	}

	public void setMedia(boolean audio, boolean instrumental, boolean video)
	{
		if (mListen != null)
			mListen.setVisible(audio);
		if (mListenInstrumental != null)
			mListenInstrumental.setVisible(instrumental);
		if (mWatch != null)
			mWatch.setVisible(video);
		if (mMediaMenuItem != null)
			mMediaMenuItem.getItem().setEnabled(audio || video || instrumental);
	}


	private void setupMenu()
	{
		if (mPrimaryMenuItem != null)
			mPrimaryMenuItem.setVisible(isDisplaySecondary() && mBilingual);
		if (mSecondaryMenuItem != null)
			mSecondaryMenuItem.setVisible(isDisplayPrimary() && mBilingual);
		if (mBilingualMenuItem != null)
			mBilingualMenuItem.setVisible(!(isDisplaySecondary() && isDisplayPrimary()) && mBilingual);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item == null)
			return super.onOptionsItemSelected(item);
		if (item.equals(mListen))
		{
			listen();
			return true;
		}
		if (item.equals(mListenInstrumental))
		{
			listenInstrumental();
			return true;
		}
		if (item.equals(mWatch))
		{
			watch();
			return true;
		}
		if (item.equals(mRelatedMenuItem))
		{
			setDisplayMode(isDisplayPrimary(), isDisplaySecondary(), !isDisplayRelated());
		}
		else if (item.equals(mBilingualMenuItem))
		{
			setDisplayMode(true, true, isDisplayRelated());
		}
		else if (item.equals(mPrimaryMenuItem))
			setDisplayMode(true, false, isDisplayRelated());
		else if (item.equals(mSecondaryMenuItem))
		{
			setDisplayMode(false, true, isDisplayRelated());
		}
		setupMenu();
		return super.onOptionsItemSelected(item);
	}

	private void watch()
	{
		if (mListening || mWatching)
			closeMedia();
		List<Media> media = mAdapter.getCurrentMedia();
		String data = null;
		for (Media m : media)
		{
			if (m.type.equals(Media.TYPE_VIDEO_MP4))
			{
				data = m.link;
				break;
			}
		}
		playVideo(data);
	}

	@Override
	public void playVideo(String string)
	{
		if (!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			runOnUiThread(new Runnable()
						  {

							  private String mString;

							  @Override
							  public void run()
							  {
								  playVideo(mString);
							  }
							  
							  public Runnable setup(String string)
							  {
								  mString = string;
								  return this;
							  }}.setup(string));
			return;
		}
		mWatching = true;
		mMediaController = new MediaController(this);
		mMediaController.setMediaPlayer(mVideoView);
		mMediaController.setAnchorView(mVideoView);
		mVideoView.setMediaController(mMediaController);
		mVideoView.setOnErrorListener(this);
		mVideoView.setOnPreparedListener(this);
		mVideoView.setOnCompletionListener(this);
		mVideoView.setVideoPath(string);
		mVideoView.setKeepScreenOn(true);
		prepVideo();
		findViewById(R.id.pagerProgressBar).setVisibility(View.VISIBLE);
//		Intent i = new Intent();
//		i.setAction(Intent.ACTION_VIEW);
//		i.setData(Uri.parse(data));
//		System.out.println(data);
//		startActivity(i);
	}
	
	private void prepVideo()
	{
		mVideoView.setVisibility(View.VISIBLE);
		findViewById(R.id.touchScreen).setBackgroundColor(Color.BLACK);
	}

	private void showVideo()
	{
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		setFullscreenLock(true);
		findViewById(android.R.id.content).setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
			| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
			| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
			getActionBar().hide();
	}

	private void hideVideo()
	{
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		mVideoView.setVisibility(View.GONE);
		findViewById(R.id.touchScreen).setBackgroundColor(Color.argb(0, 0, 0, 0));
		setFullscreenLock(false);
		setImmersive(false);
	}
	
	private void listenInstrumental()
	{
		if (mListening || mWatching)
			closeMedia();
		List<Media> media = mAdapter.getCurrentMedia();
		String data = null;
		for (Media m : media)
		{
			if (m.type.equals(Media.TYPE_AUDIO_MP3)&&m.link.contains("instrumental-"))
			{
				data = m.link.replace("vocal-and-","");
				break;
			}
		}
		mListening = true;
		findViewById(R.id.pagerProgressBar).setVisibility(View.VISIBLE);
		mMedia = new MediaUtil(this);
		mMedia.start(data, this, this, this);
//		Intent i = new Intent();
//		i.setAction(Intent.ACTION_VIEW);
//		i.setDataAndType(Uri.parse(data),"audio/mpeg3");
//		System.out.println(data);
//		startActivity(i);
	}

	private void listen()
	{
		if (mListening || mWatching)
			closeMedia();
		List<Media> media = mAdapter.getCurrentMedia();
		String data = null;
		for (Media m : media)
		{
			if (m.type.equals(Media.TYPE_AUDIO_MP3))
			{
				data = m.link;
				break;
			}
		}
		mListening = true;
		findViewById(R.id.pagerProgressBar).setVisibility(View.VISIBLE);
		mMedia = new MediaUtil(this);
		mMedia.start(data, this, this, this);
//		Intent i = new Intent();
//		i.setAction(Intent.ACTION_VIEW);
//		i.setDataAndType(Uri.parse(data),"audio/mpeg3");
//		System.out.println(data);
//		startActivity(i);
	}

	@Override
	public void onPrepared(MediaPlayer p1)
	{
		if (mListening)
		{
			mMediaController = mMedia.getController();
			mMediaController.setAnchorView(findViewById(R.id.pager));
		}
		if (mWatching)
		{
			showVideo();
			mVideoView.requestFocus();
			mVideoView.start();
		}
		mMediaController.show(0);
		findViewById(R.id.pagerProgressBar).setVisibility(View.GONE);
	}

	@Override
	public void onCompletion(MediaPlayer p1)
	{
		closeMedia();
	}

	@Override
	public boolean onError(MediaPlayer p1, int p2, int p3)
	{
		Toast.makeText(this, "Media playback failed", Toast.LENGTH_SHORT).show();
		findViewById(R.id.pagerProgressBar).setVisibility(View.GONE);
		Log.v(TAG,"Playback failed");
		closeMedia();
		return true;
	}

	private void closeMedia()
	{
		Log.v(TAG,"Playback closed");
		if (mListening)
		{
			if (mMedia != null)
				mMedia.release();
			mMedia = null;
			mListening=false;
		}
		if (mWatching)
		{
			hideVideo();
			mVideoView.stopPlayback();
			mVideoView.suspend();
			mWatching=false;
		}
		findViewById(R.id.pagerProgressBar).setVisibility(View.GONE);
		if (mMediaController != null)
			mMediaController.hide();
		mMediaController = null;
	}

	@Override
	public boolean openUrl(String url)
	{
		mLinkClicked = true;
		setFullscreen(true);
		if (!url.contains(BookFragment.BASE_URL) && url.charAt(0) != '/')
			return true;
		url = url.replace(BookFragment.BASE_URL, "/");
		if (url.contains("f_"))
		{
			if (!isDisplayRelated())
				setDisplayMode(isDisplayPrimary(), isDisplaySecondary(), true);
			((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).showReference(url.replace("/", "").replace("f_", ""));
			return true;
		}
		loadUrl(url);
		return true;
	}

	private void loadUrl(String uri)
	{
		if (uri != null && uri.contains(mBookPrimary.gl_uri))
		{
			mCurrentUri = uri;
			mAdapter.setUri(uri, mPager);
			return;
		}
		if (uri != null)
		{
			mCurrentUri = uri;
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
		if (!isDisplayPrimary())
			setDisplayRelated(false);
		if (mPager != null && mAdapter != null)
		{
			System.out.println("Refresh Display " + ((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).getPrimaryNode().title);
			((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).refreshDisplayMode();
		}
	}

	private void setFullscreen(boolean fullscreen)
	{
		if (mFullscreen == fullscreen || mLockFullscreen)
			return;
		mFullscreen = fullscreen;
		if (mMediaController != null)
		{
			if (mFullscreen)
			{
				mMediaController.hide();
			}
			else
			{
				mMediaController.show(0);
			}
		}
		if (Build.VERSION.SDK_INT < 21)
		{
			findViewById(android.R.id.content).setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);
			if (fullscreen)
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

	public void setFullscreenLock(boolean lock)
	{
		mLockFullscreen = lock;
	}

	@Override
	public void onSystemUiVisibilityChange(int visibility)
	{
		if(mLockFullscreen)
			return;
		if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == View.SYSTEM_UI_FLAG_FULLSCREEN)
		{
			getActionBar().hide();
		}
		else
		{
			getActionBar().show();
		}
	}

	@Override
	public void onBackPressed()
	{
		if (mWatching||mListening)
		{
			closeMedia();
			return;
		}
		super.onBackPressed();
	}

	@Override
	public void scrollTo(String url, double center, float scroll)
	{
		if (!mFullscreen)
			setFullscreen(true);
		((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).scrollTo(url, center, scroll);
	}

	@Override
	public void scrollTo(double center, double scroll)
	{
		if (!mFullscreen)
			setFullscreen(true);
		((BilingualViewFragment)mAdapter.getItem(mPager.getCurrentItem())).scrollTo(center, scroll);
	}

	@Override
	public String getUri()
	{
		return mCurrentUri;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putString(KEY_URI, mCurrentUri);
		super.onSaveInstanceState(outState);
	}

}
