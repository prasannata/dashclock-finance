package com.prasanna.android.dashclock.finance;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppUtil
{

    private AppUtil()
    {
    }


    public static String getSavedSymbols(Context context)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(StockTickerExtensionSettingsActivity.PREF_TICKERS, null);
    }
}
