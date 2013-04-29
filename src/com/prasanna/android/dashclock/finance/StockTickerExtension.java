package com.prasanna.android.dashclock.finance;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

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
    private static final String DJI_TICKER_SYMBOL = "%5EDJI";
    private static final String QUOTE_FORMAT = "%-6s %8s (%s)\n";

    @Override
    protected void onUpdateData(int reason)
    {
        switch (reason)
        {
            case UPDATE_REASON_INITIAL:
            case UPDATE_REASON_SETTINGS_CHANGED:
            case UPDATE_REASON_PERIODIC:
                getQuotesAndPublishUpdate();
                break;
        }
    }

    private void getQuotesAndPublishUpdate()
    {
        String dji = getDji();
        Log.d(TAG, dji);

        try
        {
            Map<String, Company> quotes = AppUtil.getQuotes(getSymbols());
            String symbols = new String();

            if (quotes != null && !quotes.isEmpty())
            {
                for (Company company : quotes.values())
                    symbols += getPriceDisplayForSymbol(company);

                Log.d(TAG, symbols);
                String webUrl = YAHOO_FINANCE_URL + DJI_TICKER_SYMBOL;
                String savedSymbols = AppUtil.getSavedSymbols(getApplicationContext());
                if (savedSymbols != null)
                    webUrl += "," + savedSymbols;

                Log.d(TAG, "Intent URL: " + webUrl);
                Intent clickIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                publishUpdate(new ExtensionData().visible(true).icon(R.drawable.stocks).status(dji)
                                .expandedBody(symbols).clickIntent(clickIntent));
            }
            else
                publishUpdateOnNoSymbols(dji);

        }
        catch (JSONException e)
        {
            publishUpdateOnNoSymbols(dji);
        }
    }

    private void publishUpdateOnNoSymbols(String status)
    {
        publishUpdate(new ExtensionData().visible(true).icon(R.drawable.stocks).status(status).expandedBody("--"));
    }

    private String getDji()
    {
        try
        {
            Company company = AppUtil.getDJI();
            if (company != null)
                return AppUtil.DJI_PRETTY_SYMBOL + " " + company.realTimePrice + " (" + company.realTimeChange + ")";
        }
        catch (XmlPullParserException e)
        {
        }
        catch (IOException e)
        {
        }

        return AppUtil.DJI_PRETTY_SYMBOL + " --";
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
