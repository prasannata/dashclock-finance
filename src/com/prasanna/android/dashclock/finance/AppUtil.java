package com.prasanna.android.dashclock.finance;

import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.prasanna.android.dashclock.finance.HttpHelper.ResponseParser;
import com.prasanna.android.dashclock.finance.SearchCompanyAsyncTask.Company;

public class AppUtil
{
    private static final String YAHOO_API_HOST = "http://query.yahooapis.com";
    private static final String YQL_PATH = "/v1/public/yql";
    private static final String GOOGLE_HOST = "http://www.google.com/ig/api";
    private static final String JSON = "json";
    private static final String ENV_VAL = "store://datatables.org/alltableswithkeys";
    private static final String YQL_FINANCE_QUOTES_HEAD = "SELECT * FROM yahoo.finance.quotes WHERE symbol=\"";
    private static final String YQL_FINANCE_QUOTES_TAIL = "\"";
    private static final String DJI = ".DJI";

    public static final String DJI_PRETTY_SYMBOL = "DJI";
    public static final String DJI_COMPANY_NAME = "Dow Jones Industrial Average";
    public static final String PREF_TICKERS = "pref_tickers";
    public static final String PREF_NUM_TICKERS = "pref_num_tickers";
    public static final int MAX_NUM_TICKERS = 5;

    public static class QueryParams
    {
        public static final String Q = "q";
        public static final String FORMAT = "format";
        public static final String ENV = "env";
        public static final String STOCK = "stock";
    }

    public static class JsonFields
    {
        public static final String QUERY = "query";
        public static final String RESULTS = "results";
        public static final String QUOTE = "quote";

        public static class Quote
        {
            public static final String NAME = "Name";
            public static final String SYMBOL = "symbol";
            public static final String LAST_TRADE_REAL_TIME_WITH_TIME = "LastTradeRealtimeWithTime";
            public static final String CHANGE_REAL_TIME = "ChangeRealtime";
        }
    }

    public static class GetQuotesAsyncTask extends AsyncTask<String, Void, Map<String, Company>>
    {
        public final static int ACTION_GET_SYMBOL_QUOTE = 0x01;
        public final static int ACTION_GET_DJI = 0x02;

        private GetQuotesAsyncTaskCompletionNotifier asyncTaskCompletionNotifier;
        private int action;

        public interface GetQuotesAsyncTaskCompletionNotifier
        {
            void onGetQuotes(Map<String, Company> result);
        }

        public GetQuotesAsyncTask(int action, GetQuotesAsyncTaskCompletionNotifier asyncTaskCompletionNotifier)
        {
            this.action = action;
            this.asyncTaskCompletionNotifier = asyncTaskCompletionNotifier;
        }

        @Override
        protected Map<String, Company> doInBackground(String... params)
        {
            try
            {
                switch (action)
                {
                    case ACTION_GET_SYMBOL_QUOTE:
                        return AppUtil.getQuotes(params);
                }
            }
            catch (JSONException e)
            {
            }

            return null;
        }

        @Override
        protected void onPostExecute(Map<String, Company> result)
        {
            super.onPostExecute(result);

            asyncTaskCompletionNotifier.onGetQuotes(result);
        }

    }

    private AppUtil()
    {
    }

    public static Map<String, Company> getQuotes(String... symbols) throws JSONException
    {
        Map<String, Company> quotes = new HashMap<String, SearchCompanyAsyncTask.Company>();

        if (symbols == null || symbols.length == 0)
            return null;

        JSONObject jsonObject = executeYahooHttpRequest(getYql(symbols));

        if (jsonObject != null)
        {
            JSONObject query = jsonObject.getJSONObject(JsonFields.QUERY);
            JSONObject results = query.getJSONObject(JsonFields.RESULTS);
            if (symbols.length > 1)
            {
                JSONArray jsonArray = results.getJSONArray(JsonFields.QUOTE);
                if (jsonArray != null)
                {
                    for (int i = 0; i < jsonArray.length(); i++)
                    {
                        Company company = getCompany(jsonArray.getJSONObject(i));
                        quotes.put(company.symbol, company);
                    }
                }
            }
            else
            {
                Company company = getCompany(results.getJSONObject(JsonFields.QUOTE));
                quotes.put(company.symbol, company);
            }
        }

        return quotes;
    }

    public static Company getDJI() throws XmlPullParserException, IOException
    {
        XmlPullParser xmlPullParser = executeGoogleHttpRequestForDJI();
        return parseXmlAndGetCompany(xmlPullParser);
    }

    private static Company parseXmlAndGetCompany(XmlPullParser xmlPullParser) throws XmlPullParserException,
                    IOException
    {
        final String SYMBOL = "symbol";
        final String LAST = "last";
        final String CHANGE = "change";
        final String DATA = "data";

        Company company = null;
        String price = null;
        String change = null;

        if (xmlPullParser == null)
            return null;

        while (xmlPullParser.next() != XmlPullParser.END_TAG)
        {
            if (xmlPullParser.getEventType() != XmlPullParser.START_TAG)
                continue;

            String name = xmlPullParser.getName();
            if (name.equals(SYMBOL))
            {
                String symbol = xmlPullParser.getAttributeValue(null, DATA);
                if (symbol == null || !DJI.equals(symbol.trim()))
                    return null;

                company = new Company(DJI_PRETTY_SYMBOL, DJI_COMPANY_NAME);
            }
            else if (name.equals(LAST))
                price = xmlPullParser.getAttributeValue(null, DATA);
            else if (name.equals(CHANGE))
                change = xmlPullParser.getAttributeValue(null, DATA);

            xmlPullParser.nextTag();
        }

        if (company != null)
        {
            company.realTimePrice = price;
            company.realTimeChange = change;
        }

        return company;
    }

    private static Company getCompany(JSONObject symbolObj) throws JSONException
    {
        String symbol = symbolObj.getString(JsonFields.Quote.SYMBOL).trim();
        String name = symbolObj.getString(JsonFields.Quote.NAME).trim();

        Company company = new Company(symbol, name);
        company.realTimePrice = splitTimeAndPriceAndGetPrice(symbolObj
                        .getString(JsonFields.Quote.LAST_TRADE_REAL_TIME_WITH_TIME));
        company.realTimeChange = formatFloatValue(symbolObj.getString(JsonFields.Quote.CHANGE_REAL_TIME));
        return company;
    }

    private static String splitTimeAndPriceAndGetPrice(String symbolObj) throws JSONException
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

    private static String getYql(String... symbols)
    {
        StringBuilder symbolBuilder = new StringBuilder();

        for (String symbol : symbols)
            symbolBuilder.append(symbol).append(" ");

        symbolBuilder.deleteCharAt(symbolBuilder.length() - 1);
        return YQL_FINANCE_QUOTES_HEAD + symbolBuilder.toString() + YQL_FINANCE_QUOTES_TAIL;
    }

    private static XmlPullParser executeGoogleHttpRequestForDJI()
    {
        HttpHelper httpHelper = new HttpHelper();
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put(QueryParams.STOCK, DJI);
        return httpHelper.executeHttpGet(GOOGLE_HOST, null, queryParams, new ResponseParser<XmlPullParser>()
        {
            @Override
            public XmlPullParser parse(String responseBody)
            {
                try
                {
                    XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser xmlPullParser = xmlPullParserFactory.newPullParser();
                    xmlPullParser.setInput(new StringReader(responseBody));
                    return xmlPullParser;
                }
                catch (XmlPullParserException e)
                {
                }

                return null;
            }
        });
    }

    private static JSONObject executeYahooHttpRequest(String yql)
    {
        HttpHelper httpHelper = new HttpHelper();
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put(QueryParams.Q, yql);
        queryParams.put(QueryParams.FORMAT, JSON);
        queryParams.put(QueryParams.ENV, ENV_VAL);
        return httpHelper.executeHttpGet(YAHOO_API_HOST, YQL_PATH, queryParams, new ResponseParser<JSONObject>()
        {

            @Override
            public JSONObject parse(String responseBody)
            {
                try
                {
                    return new JSONObject(responseBody);
                }
                catch (JSONException e)
                {
                }

                return null;
            }
        });
    }

    public static String formatFloatValue(String value)
    {
        float floatVal = Float.parseFloat(value);
        DecimalFormat fmt = new DecimalFormat("+#,##0.00;-#");
        return fmt.format(floatVal);
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
