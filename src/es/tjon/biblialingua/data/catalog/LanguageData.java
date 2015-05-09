package es.tjon.biblialingua.data.catalog;

import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import com.mobandme.ada.*;
import com.mobandme.ada.annotations.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.database.*;
import es.tjon.biblialingua.fragment.*;
import es.tjon.biblialingua.network.*;
import es.tjon.biblialingua.utils.*;
import java.util.*;
import com.android.volley.Response;

public class LanguageData implements Response.Listener<LanguageData>
{

	public static ArrayList<Language> mLanguages = null;
	public ArrayList<Language> languages = null;
	public int count = 0;
	public boolean success = false;
	public static boolean mSelectPrimary = false;
	public static  boolean mSelectSecondary = false;

	private static BaseActivity context = null;

	private static boolean langsDownloaded;
	
	public static void initialize(BaseActivity context)
	{
		LanguageData.context=context;
		runInit();
	}

	public static void runInit()
	{
		if (Looper.getMainLooper().equals(Looper.myLooper()))
		{
			new AsyncTask(){

				@Override
				protected Object doInBackground(Object[] p1)
				{
					runInit();
					return null;
				}
			}.execute();
			return;
		}
		ApplicationDataContext adc = context.getAppDataContext();
		if (mLanguages == null || mLanguages.size() < 1)
		{
			try
			{
				if (adc.languages.isEmpty())
					adc.languages.fill("l_engName");
				mLanguages = new ArrayList<Language>(adc.languages);
			}
			catch (AdaFrameworkException e)
			{e.printStackTrace();}
			if (!langsDownloaded && (mLanguages == null || mLanguages.size() < 1))
			{
				try
				{
					if (Util.getInstance(context).isConnectionWithFail())
						RestClient.query(context, new LanguageData(), new RestClient.ParameterSet(RestClient.Actions.QUERY_LANGUAGES));
					return;
				}
				catch (Exception e)
				{
					e.printStackTrace();
					context.finish();
					return;
				}
			}
		}
		if(mLanguages==null||mLanguages.size()<1)
		{
			context.finish();
			return;
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (langsDownloaded||!prefs.contains(SettingsFragment.PREFERENCE_PRIMARY_LANGUAGE))
		{
			langsDownloaded=false;
			if(mSelectPrimary)
				return;
			mSelectPrimary=true;
			selectPrimaryLanguage();
			return;
		}
		else
			context.setPrimaryLanguage(adc.getLanguage(prefs.getString(SettingsFragment.PREFERENCE_PRIMARY_LANGUAGE, "-1")));
		if (langsDownloaded||!prefs.contains(SettingsFragment.PREFERENCE_SECONDARY_LANGUAGE))
		{
			if(mSelectSecondary)
				return;
			mSelectSecondary=true;
			selectSecondaryLanguage();
			return;
		}
		else
			context.setSecondaryLanguage(adc.getLanguage(prefs.getString(SettingsFragment.PREFERENCE_SECONDARY_LANGUAGE, "-1")));
		if(Looper.getMainLooper().equals(Looper.myLooper()))
		{
			context.languageInitialized();
			return;
		}
		else
		{
			context.runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						LanguageData.context.languageInitialized();
					}
					
				
			});
		}
	}
	
	public static void selectPrimaryLanguage()
	{
		CharSequence[] items = null;
			items = new CharSequence[mLanguages.size()];
			int i = 0;
			for (Language lang : mLanguages)
			{
				items[i] = lang.eng_name;
				i++;
			}
		context.runOnUiThread(new Runnable()
					  {

						  private CharSequence[] items;
						  public Runnable setup(CharSequence[] items)
						  {
							  this.items = items;
							  return this;
						  }

						  @Override
						  public void run()
						  {
							  new AlertDialog.Builder(context)
								  .setTitle(R.string.selectPrimaryLanguage)
								  .setItems(items, new DialogInterface.OnClickListener()
								  {

									  @Override
									  public void onClick(DialogInterface dialog, int language)
									  {
										  setPrimaryLanguage(language);
										  mSelectPrimary=false;
										  runInit();
									  }


								  }).setCancelable(false)
								  .show();
						  }


					  }.setup(items));
	}

	public static void setPrimaryLanguage(int language)
	{
		Language lang = mLanguages.get(language);
		PreferenceManager.getDefaultSharedPreferences(context).edit().putString(SettingsFragment.PREFERENCE_PRIMARY_LANGUAGE, (lang!=null?lang.id:(-1)) + "").apply();
		context.setPrimaryLanguage(lang);
	}

	public static void selectSecondaryLanguage()
	{

		CharSequence[] items = new CharSequence[mLanguages.size() + 1];
			items[0] = "None";
			int i = 1;
			for (Language lang : mLanguages)
			{
				items[i] = lang.eng_name;
				i++;
			}
		context.runOnUiThread(new Runnable()
					  {

						  private CharSequence[] items;
						  public Runnable setup(CharSequence[] items)
						  {
							  this.items = items;
							  return this;
						  }

						  @Override
						  public void run()
						  {
							  new AlertDialog.Builder(context)
								  .setTitle(R.string.selectSecondaryLanguage)
								  .setItems(items, new DialogInterface.OnClickListener()
								  {

									  @Override
									  public void onClick(DialogInterface dialog, int language)
									  {
										  mSelectSecondary=false;
										  setSecondaryLanguage(language-1);
										  runInit();
									  }


								  }).setCancelable(false)
								  .show();
						  }


					  }.setup(items));
	}

	public static void setSecondaryLanguage(int language)
	{
		Language lang = mLanguages.get(language);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putString(SettingsFragment.PREFERENCE_SECONDARY_LANGUAGE, (lang!=null?lang.id:(context.getPrimaryLanguage().id)) + "").apply();
		context.setSecondaryLanguage(lang==null?context.getPrimaryLanguage():lang);
	}

	@Override
	public void onResponse(LanguageData result)
	{
		mLanguages=result.languages;
		if (result == null || !result.success)
			return;
		ApplicationDataContext adc = context.getAppDataContext();
		try
		{
			ObjectSet<Language> langs = adc.languages;
			langs.fill();
			for (Language l : langs)
			{
				l.setStatus(com.mobandme.ada.Entity.STATUS_DELETED);
			}
			langs.save();
			langs.clear();
			langs.addAll(result.languages);
			for (Language l : langs)
			{
				l.setStatus(com.mobandme.ada.Entity.STATUS_NEW);
			}
			langs.save();
			langs.fill();
			System.out.println(langs.size() + " languages");
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
			context.finish();
			return;
		}
		langsDownloaded=true;
		runInit();
	}
}
