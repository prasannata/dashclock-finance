package com.prasanna.android.dashclock.finance;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

public class StockTickerExtension extends DashClockExtension
{
    private static final String TAG = StockTickerExtension.class.getSimpleName();
    private static final String YAHOO_API_HOST = "http://query.yahooapis.com";
    private static final String YQL_PATH = "/v1/public/yql";
    private static final String YQL_FINANCE_QUOTES_HEAD = "SELECT * FROM yahoo.finance.quotes WHERE symbol=\"";
    private static final String YQL_FINANCE_QUOTES_TAIL = "\"";
    private static final String JSON = "json";
    private static final String ENV_VAL = "store://datatables.org/alltableswithkeys";
    private static final String YAHOO_FINANCE_URL = "http://finance.yahoo.com/quotes/";
    private static final String QUOTE_FORMAT = "%-6s %8s (%s)\n";

    private static class QueryParams
    {
        private static final String Q = "q";
        private static final String FORMAT = "format";
        private static final String ENV = "env";
    }

    private static class JsonFields
    {
        private static final String QUERY = "query";
        private static final String RESULTS = "results";
        private static final String QUOTE = "quote";

        private static class Quote
        {
            private static final String SYMBOL = "symbol";
            private static final String LAST_TRADE_REAL_TIME_WITH_TIME = "LastTradeRealtimeWithTime";
            private static final String CHANGE_REAL_TIME = "ChangeRealtime";
        }
    }

    @Override
    protected void onUpdateData(int reason)
    {
        try
        {
            JSONObject jsonObject = executeHttpRequest();
            String symbols = "No symbols added";

            if (jsonObject != null)
            {
                JSONObject query = jsonObject.getJSONObject(JsonFields.QUERY);
                JSONObject results = query.getJSONObject(JsonFields.RESULTS);
                JSONArray jsonArray = results.getJSONArray(JsonFields.QUOTE);

                symbols = getPriceDisplayForSymbol(jsonArray.getJSONObject(0));
                for (int i = 1; i < jsonArray.length(); i++)
                    symbols += getPriceDisplayForSymbol(jsonArray.getJSONObject(i));
            }

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
        }
    }

    private JSONObject executeHttpRequest()
    {
        String yql = getYql();

        if (yql == null)
            return null;

        HttpHelper httpHelper = new HttpHelper();
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put(QueryParams.Q, yql);
        queryParams.put(QueryParams.FORMAT, JSON);
        queryParams.put(QueryParams.ENV, ENV_VAL);
        return httpHelper.executeHttpGet(YAHOO_API_HOST, YQL_PATH, queryParams);
    }

    private String getYql()
    {
        String desiredSymbols = AppUtil.getSavedSymbols(getApplicationContext());
        if (desiredSymbols != null)
            return YQL_FINANCE_QUOTES_HEAD + desiredSymbols.replaceAll(",", " ") + YQL_FINANCE_QUOTES_TAIL;
        return null;
    }

    private String getPriceDisplayForSymbol(JSONObject symbolObj) throws JSONException
    {
        return String.format(
                        Locale.getDefault(),
                        QUOTE_FORMAT,
                        symbolObj.getString(JsonFields.Quote.SYMBOL).trim(),
                        splitTimeAndPriceAndGetPrice(symbolObj
                                        .getString(JsonFields.Quote.LAST_TRADE_REAL_TIME_WITH_TIME)),
                        formatFloatValue(symbolObj.getString(JsonFields.Quote.CHANGE_REAL_TIME).trim())).toString();
    }

    private String splitTimeAndPriceAndGetPrice(String symbolObj) throws JSONException
    {
        String[] timeAndPrice = symbolObj.split("-");
        if (timeAndPrice != null && timeAndPrice.length == 2)
        {
            Pattern p = Pattern.compile("<b>(.*)</b>");
            Matcher m = p.matcher(timeAndPrice[1].trim());
            m.find();
            return String.format(Locale.getDefault(), "%5.2f", Float.parseFloat(m.group(1).trim()));
        }
        return symbolObj;
    }

    private String formatFloatValue(String value) throws JSONException
    {
        float floatVal = Float.parseFloat(value);
        DecimalFormat fmt = new DecimalFormat("+#,##0.00;-#");
        return fmt.format(floatVal);
    }
}
