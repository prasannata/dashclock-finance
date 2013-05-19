package com.prasanna.android.dashclock.finance;

import java.io.IOException;
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
        final int IDX_DJI_VAL = 0;
        final int IDX_DJI_PRETTY_PRINT = 1;
        String[] dji = getDji();
        Log.d(TAG, dji[IDX_DJI_PRETTY_PRINT]);

        try
        {
            Map<String, Company> quotes = AppUtil.getQuotes(getSymbols());
            String symbols = new String();

            if (quotes != null && !quotes.isEmpty())
            {
                for (Company company : quotes.values())
                    symbols += getPriceDisplayForSymbol(company);

                String webUrl = YAHOO_FINANCE_URL + DJI_TICKER_SYMBOL;
                String savedSymbols = AppUtil.getSavedSymbols(getApplicationContext());
                if (savedSymbols != null)
                    webUrl += "," + savedSymbols;

                Intent clickIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                publishUpdate(new ExtensionData().visible(true).icon(R.drawable.stocks).status(dji[IDX_DJI_VAL])
                                .expandedTitle(dji[IDX_DJI_PRETTY_PRINT]).expandedBody(symbols)
                                .clickIntent(clickIntent));
            }
            else
                publishUpdateOnNoSymbols(dji[IDX_DJI_PRETTY_PRINT]);

        }
        catch (JSONException e)
        {
            publishUpdateOnNoSymbols(dji[IDX_DJI_PRETTY_PRINT]);
        }
    }

    private void publishUpdateOnNoSymbols(String status)
    {
        publishUpdate(new ExtensionData().visible(true).icon(R.drawable.stocks).status(status).expandedBody("--"));
    }

    private String[] getDji()
    {
        String[] dji = new String[2];

        try
        {
            Company company = AppUtil.getDJI();
            if (company != null)
            {
                dji[0] = company.realTimePrice;
                dji[1] = AppUtil.DJI_PRETTY_SYMBOL + " " + company.realTimePrice + " " + company.realTimeChange;
                return dji;
            }
        }
        catch (XmlPullParserException e)
        {
        }
        catch (IOException e)
        {
        }

        dji[0] = "--";
        dji[1] = AppUtil.DJI_PRETTY_SYMBOL + " --";
        return dji;
    }

    private String[] getSymbols()
    {
        return AppUtil.getSavedSymbolsArray(getApplicationContext());
    }

    private String getPriceDisplayForSymbol(Company company) throws JSONException
    {
        return AppUtil.padRight(company.symbol, 6 + (6 - company.symbol.length())) + ".."
                        + AppUtil.padLeft(company.realTimePrice, 10 + (10 - company.realTimePrice.length())) + " "
                        + company.realTimeChange + "\n";
    }
}
