package es.tjon.biblialingua.fragment;

import android.os.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.data.catalog.*;
import es.tjon.biblialingua.database.*;
import java.util.*;
import android.preference.*;

public class SettingsFragment extends PreferenceFragment
{

	public static final String PREFERENCE_PRIMARY_LANGUAGE="primaryLanguage";
	
	public static final String PREFERENCE_SECONDARY_LANGUAGE="secondaryLanguage";
	
	public static final String PREFERENCE_COLOR="colorScheme";
	
	public static final String PREFERENCE_DISPLAY="portraitVertical";

	private ApplicationDataContext adc;
	
	private ListPreference primaryLanguage;

	private ListPreference secondaryLanguage;
	
	private ListPreference colorScheme;
	
	private ListPreference portraitDisplay;

	private ArrayList<CharSequence> entryValues;

	private ArrayList<CharSequence> entries;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		primaryLanguage = (ListPreference)findPreference(PREFERENCE_PRIMARY_LANGUAGE);
		secondaryLanguage = (ListPreference)findPreference(PREFERENCE_SECONDARY_LANGUAGE);
		colorScheme = (ListPreference)findPreference(PREFERENCE_COLOR);
		portraitDisplay = (ListPreference)findPreference(PREFERENCE_DISPLAY);
		adc = ((BaseActivity)getActivity()).getAppDataContext();
		entries = new ArrayList<CharSequence>();
		entryValues = new ArrayList<CharSequence>();
		try
		{
			adc.languages.fill();
			for(Language l : adc.languages)
			{
				entries.add(l.eng_name);
				entryValues.add(new Long(l.id).toString());
			}
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
		}
		if(entries.isEmpty())
		{
			entries.add("English");
			entryValues.add("-1");
		}
		primaryLanguage.setEntries(entries.toArray(new CharSequence[entries.size()]));
		primaryLanguage.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
		primaryLanguage.setSummary(primaryLanguage.getEntry());
		secondaryLanguage.setEntries(entries.toArray(new CharSequence[entries.size()]));
		secondaryLanguage.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
		secondaryLanguage.setSummary(secondaryLanguage.getEntry());
		colorScheme.setSummary(colorScheme.getEntry());
		portraitDisplay.setSummary(portraitDisplay.getValue().equals(getResources().getStringArray(R.array.portraitDisplayValues)[1])?getResources().getString(R.string.belowPrimary):getResources().getString(R.string.besidePrimary));
		primaryLanguage.setOnPreferenceChangeListener(new ListPreference.OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					if(preference.equals(primaryLanguage))
					{
						((BaseActivity)getActivity()).invalidateInit();
						primaryLanguage.setSummary(entries.get(entryValues.indexOf(newValue)));
						return true;
					}
					return false;
				}
		});
		secondaryLanguage.setOnPreferenceChangeListener(new ListPreference.OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					if(preference.equals(secondaryLanguage))
					{
						((BaseActivity)getActivity()).invalidateInit();
						secondaryLanguage.setSummary(entries.get(entryValues.indexOf(newValue)));
						return true;
					}
					return false;
				}
			});
			colorScheme.setOnPreferenceChangeListener(new ListPreference.OnPreferenceChangeListener()
			{

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					((BaseActivity)getActivity()).invalidateInit();
					((BaseActivity)SettingsFragment.this.getActivity()).setColorScheme(newValue.toString());
					colorScheme.setSummary(newValue.toString());
					((BaseActivity)getActivity()).checkInit();
					return true;
				}
			});
			portraitDisplay.setOnPreferenceChangeListener(new ListPreference.OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference pref, Object newValue)
				{
					portraitDisplay.setSummary(newValue.equals(getResources().getStringArray(R.array.portraitDisplayValues)[1])?getResources().getString(R.string.belowPrimary):getResources().getString(R.string.besidePrimary));
					return true;
				}
				
			});
	}
	
}
