package com.prasanna.android.dashclock.finance;

import java.util.Locale;
import java.util.Map;

import org.json.JSONException;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.prasanna.android.dashclock.finance.SearchCompanyAsyncTask.Company;

public class StockTickerExtension extends DashClockExtension
{
    private static final String TAG = StockTickerExtension.class.getSimpleName();
    private static final String YAHOO_FINANCE_URL = "http://finance.yahoo.com/quotes/";
    private static final String QUOTE_FORMAT = "%-6s %8s (%s)\n";

    @Override
    protected void onUpdateData(int reason)
    {
        switch (reason)
        {
            case UPDATE_REASON_INITIAL:
            case UPDATE_REASON_SETTINGS_CHANGED:
            case UPDATE_REASON_PERIODIC:
            case UPDATE_REASON_SCREEN_ON:
                getQuotesAndPublishUpdate();
                break;
        }
    }

    private void getQuotesAndPublishUpdate()
    {
        try
        {
            Map<String, Company> quotes = AppUtil.getQuotes(getSymbols());
            String symbols = new String();

            for (Company company : quotes.values())
                symbols += getPriceDisplayForSymbol(company);

            Log.d(TAG, symbols);
            Intent clickIntent =
                            new Intent(Intent.ACTION_VIEW, Uri.parse(YAHOO_FINANCE_URL
                                            + AppUtil.getSavedSymbols(getApplicationContext())));
            publishUpdate(new ExtensionData().visible(true).icon(R.drawable.stocks).status("Stock Prices")
                            .expandedBody(symbols).clickIntent(clickIntent));

        }
        catch (JSONException e)
        {
            e.printStackTrace();
            publishUpdate(new ExtensionData().visible(true).icon(R.drawable.stocks).status("Stock Prices")
                            .expandedBody("--"));
        }
    }

    private String[] getSymbols()
    {
        String desiredSymbols = AppUtil.getSavedSymbols(getApplicationContext());
        if (desiredSymbols != null)
            return desiredSymbols.split(",");

        return null;
    }

    private String getPriceDisplayForSymbol(Company company) throws JSONException
    {
        return String.format(Locale.getDefault(), QUOTE_FORMAT, company.symbol, company.realTimePrice,
                        company.realTimeChange).toString();
    }
}
