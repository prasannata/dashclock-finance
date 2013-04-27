package com.prasanna.android.dashclock.finance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.prasanna.android.dashclock.finance.SearchCompanyAsyncTask.AsyncTaskCompletionNotifier;
import com.prasanna.android.dashclock.finance.SearchCompanyAsyncTask.CompanySearchResult;

public class StockTickerExtensionSettingsActivity extends Activity
{
    private static final String TAG = StockTickerExtensionSettingsActivity.class.getSimpleName();
    public static final String PREF_TICKERS = "pref_tickers";

    private TextView searchTermTv;
    private ImageView searchActionIv;
    private ListView searchResultList;
    private SearchResultAdapter searchResultAdapter;
    private HashSet<String> savedSymbolSet = new HashSet<String>();
    private ArrayList<CompanySearchResult> companySearchResults;

    private class SearchCompleteNotifier implements AsyncTaskCompletionNotifier<List<CompanySearchResult>>
    {
        public void onAsyncTaskComplete(List<CompanySearchResult> result)
        {
            companySearchResults.clear();
            searchResultAdapter.notifyDataSetInvalidated();
            companySearchResults.addAll(result);
            searchResultAdapter.notifyDataSetChanged();
        }
    }

    static class SearchViewHolder
    {
        TextView companyNameTv;
        ToggleButton addTickerToggle;
    }

    private class SearchResultAdapter extends ArrayAdapter<CompanySearchResult>
    {

        public SearchResultAdapter(Context context, int resource, int textViewResourceId,
                        List<CompanySearchResult> objects)
        {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            SearchViewHolder searchViewHolder = null;

            if (convertView == null)
            {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.company_search_result_item, null);
                searchViewHolder = new SearchViewHolder();
                searchViewHolder.companyNameTv = (TextView) convertView.findViewById(R.id.companyName);
                searchViewHolder.addTickerToggle = (ToggleButton) convertView.findViewById(R.id.addTickerToggle);
                convertView.setTag(searchViewHolder);
            }
            else
                searchViewHolder = (SearchViewHolder) convertView.getTag();

            final CompanySearchResult item = getItem(position);
            searchViewHolder.companyNameTv.setText(item.name + " (" + item.symbol + ")");
            searchViewHolder.addTickerToggle.setOnCheckedChangeListener(new OnCheckedChangeListener()
            {

                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    if (isChecked && !savedSymbolSet.contains(item.symbol))
                        addTicker(item);
                    else if (!isChecked && savedSymbolSet.contains(item.symbol))
                        removeTicker(item);
                }

            });
            if (savedSymbolSet.contains(item.symbol))
                searchViewHolder.addTickerToggle.setChecked(true);
            else
                searchViewHolder.addTickerToggle.setChecked(false);
            return convertView;
        }

        private void addTicker(final CompanySearchResult item)
        {
            Log.d(TAG, "Adding ticker: " + item.symbol);
            SharedPreferences sharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String existingTickers = sharedPreferences.getString(PREF_TICKERS, null);
            Editor editor = sharedPreferences.edit();
            if (existingTickers == null)
                editor.putString(PREF_TICKERS, item.symbol);
            else
                editor.putString(PREF_TICKERS, existingTickers + "," + item.symbol);
            editor.commit();
        }

        private void removeTicker(CompanySearchResult item)
        {
            savedSymbolSet.remove(item.symbol);

            Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
            editor.putString(PREF_TICKERS, convertSetToCsvString());
            editor.commit();
        }

        private String convertSetToCsvString()
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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);

        String savedSymbols = AppUtil.getSavedSymbols(getApplicationContext());

        if (savedSymbols != null)
            savedSymbolSet.addAll(Arrays.asList(savedSymbols.split(",")));

        searchTermTv = (TextView) findViewById(R.id.searchTerm);
        searchActionIv = (ImageView) findViewById(R.id.searchAction);
        searchResultList = (ListView) findViewById(R.id.searchResultList);
        companySearchResults = new ArrayList<CompanySearchResult>();
        searchResultAdapter =
                        new SearchResultAdapter(this, R.layout.company_search_result_item, R.id.companyName,
                                        companySearchResults);
        searchResultList.setAdapter(searchResultAdapter);

        searchActionIv.setOnClickListener(new View.OnClickListener()
        {

            public void onClick(View v)
            {
                CharSequence searchTerm = searchTermTv.getText();

                if (searchTerm != null && searchTerm.length() > 0)
                    new SearchCompanyAsyncTask(new SearchCompleteNotifier()).execute(searchTerm.toString());
            }
        });
    }
}
