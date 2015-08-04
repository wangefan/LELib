package com.UI.LEDevice;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

public class CommandsList extends PreferenceFragment
{
	private String mTitle = "";
		
	public String getTitle()
	{
		return mTitle;
	}
	
	public void setTitle(final String title)
	{
		ListPreference p = (ListPreference ) getPreferenceManager().findPreference("lstCommands");
		if(p != null)
		{
			p.setTitle(title);
		}
		mTitle = title;
	}
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
 
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
 
        /** Defining PreferenceChangeListener */
        OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener() {
 
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                OnPreferenceChangeListener listener = ( OnPreferenceChangeListener) getActivity();
                listener.onPreferenceChange(preference, newValue);
                return true;
            }
       };
 
        /** Getting the ListPreference from the Preference Resource */
        ListPreference p = (ListPreference ) getPreferenceManager().findPreference("lstCommands");
        /** Setting Preference change listener for the ListPreference */
        p.setOnPreferenceChangeListener(onPreferenceChangeListener);
    }
}
