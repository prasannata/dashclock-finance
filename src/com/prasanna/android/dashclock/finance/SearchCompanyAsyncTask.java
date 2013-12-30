package com.prasanna.android.dashclock.finance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.prasanna.android.dashclock.finance.SearchCompanyAsyncTask.Company;

public class SearchCompanyAsyncTask extends AsyncTask<String, Void, List<Company>> {
  private static final String HOST = "http://d.yimg.com/autoc.finance.yahoo.com/autoc";
  private static final String QUERY = "query";
  private static final String CALLBACK = "callback";
  private static final String CALLBACK_VAL = "YAHOO.Finance.SymbolSuggest.ssCallback";

  public static class Company {
    public final String symbol;
    public final String name;
    public String realTimePrice;
    public String realTimeChange;

    public Company(String symbol, String name) {
      this.symbol = symbol.trim();
      this.name = name.trim();
    }

    @Override
    public String toString() {
      return "Company [symbol=" + symbol + ", name=" + name + ", realTimePrice=" + realTimePrice + ", realTimeChange="
          + realTimeChange + "]";
    }
  }

  public interface AsyncTaskCompletionNotifier<T> {
    void onAsyncTaskComplete(T result);
  }

  private AsyncTaskCompletionNotifier<List<Company>> asyncTaskCompletionNotifier;

  public SearchCompanyAsyncTask(AsyncTaskCompletionNotifier<List<Company>> asyncTaskCompletionNotifier) {
    this.asyncTaskCompletionNotifier = asyncTaskCompletionNotifier;
  }

  @Override
  protected List<Company> doInBackground(String... params) {
    List<Company> searchResults = new ArrayList<Company>();
    String responseBody = executeHttpRequestAndGetResponse(params);
    if (responseBody != null) {
      try {
        JSONObject jsonObject = new JSONObject(getJsonText(responseBody));
        JSONObject resultSet = jsonObject.getJSONObject("ResultSet");
        JSONArray jsonArray = resultSet.getJSONArray("Result");

        for (int i = 0; i < jsonArray.length(); i++) {
          JSONObject resultItem = jsonArray.getJSONObject(i);
          searchResults.add(new Company(resultItem.getString("symbol"), resultItem.getString("name")));
        }

      } catch (JSONException e) {
      }
    }

    return searchResults;
  }

  private String executeHttpRequestAndGetResponse(String... params) {
    HttpHelper httpHelper = new HttpHelper();
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put(QUERY, params[0]);
    queryParams.put(CALLBACK, CALLBACK_VAL);
    return httpHelper.executeHttpGetAndGetResponseBody(HOST, null, queryParams);
  }

  private String getJsonText(String responseBody) throws JSONException {
    Pattern p = Pattern.compile(CALLBACK_VAL + "\\((.*)\\)");
    Matcher m = p.matcher(responseBody);
    m.find();
    return m.group(1);
  }

  @Override
  protected void onPostExecute(List<Company> result) {
    asyncTaskCompletionNotifier.onAsyncTaskComplete(result);
  }

}
