package es.tjon.biblialingua;

import android.content.*;
import android.os.*;
import android.preference.*;
import android.util.*;
import android.view.*;
import com.mobandme.ada.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.data.catalog.*;
import es.tjon.biblialingua.database.*;
import es.tjon.biblialingua.fragment.*;
import es.tjon.biblialingua.network.*;
import es.tjon.biblialingua.utils.*;
import android.widget.*;
import android.text.*;
import java.util.*;
import android.support.v4.app.*;
import android.support.v7.app.*;
import android.hardware.display.*;
import android.support.v4.hardware.display.*;
import android.app.*;
import android.graphics.*;
import android.text.method.*;
import com.instabug.library.*;

public class BaseActivity extends FragmentActivity
{
	
	private ApplicationDataContext mAppDataContext = null;
	private BookUtil mBookUtil=null;
	private Util mUtil = null;

	private static Language mPrimaryLanguage=null;
	private static Language mSecondaryLanguage=null;
	private static String mColorScheme="Night";

	private static boolean mInitialized = false;
	private static boolean mFileInitialized = false;
	private static boolean mLanguageInitialized=false;
	private static boolean mCatalogInitialized=false;
	private static boolean mUpdateInitialized=false;

	private static ArrayList<Class<? extends BaseActivity>> mSaveClass= new ArrayList<Class<? extends BaseActivity>>();
	private static ArrayList<Bundle> mSaveData=new ArrayList<Bundle>();

	private static boolean mExit = false;

	private static boolean mDisplayPrimary = true;
	private static boolean mDisplaySecondary = false;
	private static boolean mDisplayRelated = false;

	private static boolean mHideNotDownloaded = false;


	public void fileInitialized()
	{
		mFileInitialized=true;
		initialize();
	}

	public void updateInitialized()
	{
		mUpdateInitialized=true;
		initialize();
	}
	
	public void exit()
	{
		mExit=true;
		finish();
		
	}

	public void setColorScheme(String colorScheme)
	{
		mColorScheme=colorScheme;
		switch(mColorScheme)
		{
			case "Night":
				setTheme(R.style.Dark);
				break;
			case "Sepia":
				setTheme(R.style.Sepia);
				//findViewById(android.R.id.content).setBackgroundColor(Color.rgb(250,230,175));
				break;
			case "Day":
				setTheme(R.style.Light);
				break;
			default:
				setTheme(R.style.Dark);
				break;
		}
	}
	
	public String getColorScheme()
	{
		return mColorScheme;
	}
	
	public void setDisplayPrimary(boolean display)
	{
		mDisplayPrimary=display;
	}
	
	public void setDisplaySecondary(boolean display)
	{
		mDisplaySecondary=display;
	}
	
	public void setDisplayRelated(boolean display)
	{
		mDisplayRelated=display;
	}
	
	public static boolean isDisplayPrimary()
	{
		return mDisplayPrimary;
	}
	
	public static boolean isDisplaySecondary()
	{
		return mDisplaySecondary;
	}
	
	public static boolean isDisplayRelated()
	{
		return mDisplayRelated;
	}

	public void setLastUpdate(Language language, long timeMillis)
	{
		PreferenceManager.getDefaultSharedPreferences(this)
			.edit().putLong("updated"+language.code_three,timeMillis);
	}
	
	public Long getLastUpdate(Language language)
	{
		return PreferenceManager.getDefaultSharedPreferences(this).getLong("updated"+language.code_three,0);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		if(mExit)
		{
			if(isBaseActivity())
			{
				mExit=false;
			}
			finish();
			super.onCreate(savedInstanceState);
			return;
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(isBaseActivity())
		{
			if(getActionBar()!=null)
				getActionBar().hide();
			setContentView(R.layout.splash);
		}
		else
		{
			setColorScheme(prefs.getString(SettingsFragment.PREFERENCE_COLOR, "Night"));
		}
		if(savedInstanceState!=null)
		{
			setPrimaryLanguage(getAppDataContext().getLanguage(prefs.getString(SettingsFragment.PREFERENCE_PRIMARY_LANGUAGE, "-1")));
			setSecondaryLanguage(getAppDataContext().getLanguage(prefs.getString(SettingsFragment.PREFERENCE_SECONDARY_LANGUAGE, "-1")));
			if(mPrimaryLanguage!=null)
				mInitialized=true;
		}
		
		if(!checkInit()&&!checkSaveClass()&&mInitialized&&isBaseActivity())
		{
			Intent i = new Intent(this, CatalogActivity.class);
			startActivity(i);
		}
		if(!isBaseActivity())
		{
			if(Build.VERSION.SDK_INT>=21)
			{
				getWindow().setNavigationBarColor(Color.rgb(10,1,50));
				getWindow().setStatusBarColor(Color.rgb(10,1,50));
				getActionBar().setHomeButtonEnabled(true);
			}
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId()==android.R.id.home)
		{
			startActivity(new Intent(this, BaseActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean isBaseActivity()
	{
		return !(this instanceof CatalogActivity)&&!(this instanceof BrowseActivity)&&!(this instanceof SettingsActivity)&&!(this instanceof BookViewActivity)&&!(this instanceof OsisActivity);
	}

	public boolean checkSaveClass()
	{
		if (mInitialized && mSaveClass.size() > 0)
		{
			Intent i = new Intent(this, mSaveClass.remove(0));
			if (mSaveData.size() > 0)
				i.putExtras(mSaveData.remove(0));
			startActivity(i);
			return true;
		}
		return false;
	}

	@Override
	protected void onResume()
	{
		if(mExit)
		{
			if(isBaseActivity())
				mExit=false;
			finish();
			super.onResume();
			return;
		}
		checkInit();
		super.onResume();
	}

	public boolean checkInit()
	{
		if (!mInitialized && !isBaseActivity())
		{
			mSaveClass.add(this.getClass());
			mSaveData.add(new Bundle());
			onSaveInstanceState(mSaveData.get(mSaveData.size() - 1));
			this.finish();
			return true;
		}
		else if (!mInitialized)
		{
			//getUtil().showLoadingDialog("Initializing");
			initialize();
			return true;
		}
		return false;
	}

	public void invalidateInit()
	{
		mInitialized = false;
		mLanguageInitialized = false;
		mCatalogInitialized = false;
	}

	public void initialize()
	{
		if (mInitialized)
			return;
		
		if(!mFileInitialized)
		{
			setLoadingMessage(R.string.loadingFiles);
			ApplicationDataContext.initialize(this);
			return;
		}
		if (!mLanguageInitialized)
		{
			setLoadingMessage(R.string.loadingLanguage);
			LanguageData.initialize(this);
			return;
		}
		if(getPrimaryLanguage()==null)
			System.err.println("Language not initialoized");
		if (!mCatalogInitialized)
		{
			setLoadingMessage(R.string.loadingCatalog);
			CatalogData.initialize(this);
			return;
		}
		if (!mUpdateInitialized)
		{
			UpdateUtil.initialize(this);
		}
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mHideNotDownloaded=prefs.getBoolean(SettingsFragment.PREFERENCE_HIDE_NOT_DOWNLOADED,false);
		Intent service = new Intent(this, DownloadService.class);
		service.putExtra(DownloadService.CHECK_UPDATES,true);
		startService(service);
		mInitialized = true;
		initFinished();
	}
	
	public void setLoadingMessage(String loading)
	{
		if(!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			runOnUiThread(new Runnable()
						  {

							  private String mLoading;

							  public Runnable setup(String loading)
							  {
								  mLoading=loading;
								  return this;
							  }

							  @Override
							  public void run()
							  {
								  setLoadingMessage(mLoading);
							  }
						  }.setup(loading));
			return;
		}
		((TextView)findViewById(R.id.splashTextView)).setText(loading);
	}

	public void setLoadingMessage(int loading)
	{
		if(!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			runOnUiThread(new Runnable()
						  {

							  private int mLoading;

							  public Runnable setup(int loading)
							  {
								  mLoading=loading;
								  return this;
							  }

					@Override
					public void run()
					{
						setLoadingMessage(mLoading);
					}
				}.setup(loading));
				return;
		}
		((TextView)findViewById(R.id.splashTextView)).setText(loading);
	}
	
	

	private void initFinished()
	{
		if (!checkSaveClass())
		{
			Intent i = new Intent(this, CatalogActivity.class);
			startActivity(i);
		}
	}

	public void languageInitialized()
	{
		mLanguageInitialized = true;
		initialize();
	}

	public void catalogInitialized()
	{
		mCatalogInitialized = true;
		initialize();
	}

	public ApplicationDataContext getAppDataContext()
	{
		try
		{
			if (mAppDataContext == null)
				mAppDataContext = new ApplicationDataContext(this);
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
		}
		return mAppDataContext;
	}

	public BookUtil getBookUtil()
	{
		if (mBookUtil == null && Looper.getMainLooper().equals(Looper.myLooper()))
			mBookUtil = new BookUtil(this);
		return mBookUtil;
	}

	public Util getUtil()
	{
		if (mUtil == null)
			mUtil = Util.getInstance(this);
		return mUtil;
	}

	public static Language getPrimaryLanguage()
	{
		return mPrimaryLanguage;
	}

	public static Language getSecondaryLanguage()
	{
		return mSecondaryLanguage;
	}

	public static void setPrimaryLanguage(Language language)
	{
		mPrimaryLanguage = language;
	}

	public static void setSecondaryLanguage(Language language)
	{
		mSecondaryLanguage = language;
	}
	
	public void setHideNotDownloaded(boolean hide)
	{
		mHideNotDownloaded=hide;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit().putBoolean(SettingsFragment.PREFERENCE_HIDE_NOT_DOWNLOADED,hide).apply();
	}
	
	public boolean getHideNotDownloaded()
	{
		return mHideNotDownloaded;
	}
	
	@Override
	public void onBackPressed()
	{
		if (mInitialized == false && !isBaseActivity())
		{
			this.finish();
		}
		else
			super.onBackPressed();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if(this instanceof SettingsActivity)
			return false;
		MenuInflater inflator = getMenuInflater();
		inflator.inflate(R.menu.menu,menu);
		return true;
	}

	@Override
	protected void onDestroy()
	{
		if (mBookUtil != null)
			mBookUtil.unBind();
		mBookUtil=null;
		if(mUtil!=null)
			mUtil.cleanup();
		mUtil=null;
		super.onDestroy();
	}

	public void openSettings(MenuItem item)
	{
		startActivity(new Intent(this, SettingsActivity.class));
	}
	
	public void exit(MenuItem item)
	{
		exit();
	}

	@Override
	public void finish()
	{
		if(mInitialized)
		{
			super.finish();
			return;
		}
		getUtil().failInit();
	}

}
