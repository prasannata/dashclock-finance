package com.prasanna.android.dashclock.finance;

import java.util.Collection;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.prasanna.android.dashclock.finance.SearchCompanyAsyncTask.Company;

public class AppUtil
{
    public static final String PREF_TICKERS = "pref_tickers";
    public static final String PREF_NUM_TICKERS = "pref_num_tickers";
    public static final int MAX_NUM_TICKERS = 5;

    private AppUtil()
    {
    }

    public static String getSavedSymbols(Context context)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_TICKERS, null);
    }

    public static boolean canAddTicker(Context context)
    {
        return getNumTickers(context) < MAX_NUM_TICKERS;
    }

    public static int getNumTickers(Context context)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(PREF_NUM_TICKERS, 0);
    }

    public static void addTicker(final Context context, final Company item)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int numTickers = sharedPreferences.getInt(PREF_NUM_TICKERS, 0);
        String existingTickers = sharedPreferences.getString(PREF_TICKERS, null);
        Editor editor = sharedPreferences.edit();
        if (existingTickers == null)
            editor.putString(PREF_TICKERS, item.symbol);
        else
            editor.putString(PREF_TICKERS, existingTickers + "," + item.symbol);

        editor.putInt(PREF_NUM_TICKERS, ++numTickers);
        editor.commit();
    }

    public static void saveTickers(final Context context, final Collection<String> tickers)
    {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(PREF_TICKERS, convertSetToCsvString(tickers));
        editor.putInt(PREF_NUM_TICKERS, tickers.size());
        editor.commit();
    }

    private static String convertSetToCsvString(Collection<String> savedSymbolSet)
    {
        StringBuilder result = new StringBuilder();
        for (String symbol : savedSymbolSet)
        {
            result.append(symbol);
            result.append(",");
        }

        return result.length() > 0 ? result.substring(0, result.length() - 1) : "";
    }
}
