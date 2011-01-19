package com.misgod.pdbreader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.misgod.pdbreader.util.Constatnts;

public class SettingActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setPreferenceScreen(createPreferenceHierarchy());
    }

    private PreferenceScreen createPreferenceHierarchy() {
    	PreferenceManager prefManager = getPreferenceManager();
    	prefManager.setSharedPreferencesName(Constatnts.PREF_TAG);
    	prefManager.setSharedPreferencesMode(Context.MODE_PRIVATE);
    	
    	
        // Root
        PreferenceScreen root = prefManager.createPreferenceScreen(this);
        
        // Inline preferences 
        PreferenceCategory displayPrefCat = new PreferenceCategory(this);
        displayPrefCat.setTitle(R.string.setting_display);
        root.addPreference(displayPrefCat);
        
        // fullscreenPref preference
        CheckBoxPreference fullscreenPref = new CheckBoxPreference(this);
        fullscreenPref.setKey("pref_fullscreen");
        fullscreenPref.setTitle(R.string.setting_fullscreen);
        fullscreenPref.setSummary(R.string.setting_fullscreen_desc);
        displayPrefCat.addPreference(fullscreenPref);
        
        
        // screenOnPref preference
        CheckBoxPreference screenOnPref = new CheckBoxPreference(this);
        screenOnPref.setKey("pref_screenon");
        screenOnPref.setTitle(R.string.setting_screenon);
        screenOnPref.setSummary(R.string.setting_screenon_desc);
        screenOnPref.setDefaultValue(true);
        displayPrefCat.addPreference(screenOnPref);
        

        // Color List preference
        PreferenceScreen colorPref = getPreferenceManager().createPreferenceScreen(this);
        colorPref.setIntent(new Intent(this,ColorListActivity.class));
        colorPref.setTitle(R.string.setting_pickcolor);
        colorPref.setSummary(R.string.setting_pickcolor_desc);
        displayPrefCat.addPreference(colorPref);

        colorPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				finish();
				return false;
			}
		});
        
        
        // Inline preferences 
        PreferenceCategory scrollPrefCat = new PreferenceCategory(this);
        scrollPrefCat.setTitle(R.string.setting_scroll);
        root.addPreference(scrollPrefCat);
        
        // tapScrollPref preference
        CheckBoxPreference tapScrollPref = new CheckBoxPreference(this);
        tapScrollPref.setKey("pref_tapscroll");
        tapScrollPref.setTitle(R.string.setting_tapscroll);
        tapScrollPref.setSummary(R.string.setting_tapscroll_desc);
        tapScrollPref.setDefaultValue(true);
        scrollPrefCat.addPreference(tapScrollPref);
        
        
        // volumeScrollPref preference
        CheckBoxPreference volumeScrollPref = new CheckBoxPreference(this);
        volumeScrollPref.setKey("pref_volumescroll");
        volumeScrollPref.setTitle(R.string.setting_volume);
        volumeScrollPref.setSummary(R.string.setting_volume_desc);
        scrollPrefCat.addPreference(volumeScrollPref);

        return root;
    }
}
