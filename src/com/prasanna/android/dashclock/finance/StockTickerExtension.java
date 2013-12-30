package com.prasanna.android.dashclock.finance;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;

import android.content.Intent;
import android.net.Uri;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.prasanna.android.dashclock.finance.SearchCompanyAsyncTask.Company;

public class StockTickerExtension extends DashClockExtension {
  private static final String YAHOO_FINANCE_URL = "http://finance.yahoo.com/quotes/";
  private static final String DJI_TICKER_SYMBOL = "%5EDJI";

  @Override
  protected void onUpdateData(int reason) {
    switch (reason) {
      case UPDATE_REASON_INITIAL:
      case UPDATE_REASON_SETTINGS_CHANGED:
      case UPDATE_REASON_PERIODIC:
        getQuotesAndPublishUpdate();
        break;
    }
  }

  private void getQuotesAndPublishUpdate() {
    try {
      Map<String, Company> quotes = AppUtil.getQuotes(AppUtil.getSavedSymbolsArray(getApplicationContext()));

      if (!quotes.isEmpty()) {
        String symbols = new String();
        for (Company company : quotes.values())
          symbols += getPriceDisplayForSymbol(company);

        String webUrl = YAHOO_FINANCE_URL + DJI_TICKER_SYMBOL;
        String savedSymbols = AppUtil.getSavedSymbols(getApplicationContext());
        if (savedSymbols != null)
          webUrl += "," + savedSymbols;

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());

        Intent clickIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
        publishUpdate(new ExtensionData().visible(true).icon(R.drawable.stocks).expandedTitle(currentDateandTime)
            .expandedBody(symbols).clickIntent(clickIntent));
      }
    } catch (JSONException e) {
    }
  }

  private String getPriceDisplayForSymbol(Company company) throws JSONException {
    return AppUtil.padRight(company.symbol, 6 + (6 - company.symbol.length())) + ".."
        + AppUtil.padLeft(company.realTimePrice, 10 + (10 - company.realTimePrice.length())) + " "
        + company.realTimeChange + "\n";
  }
}
