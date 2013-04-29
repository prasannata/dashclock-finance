package com.prasanna.android.dashclock.finance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.prasanna.android.dashclock.finance.AppUtil.GetQuotesAsyncTask;
import com.prasanna.android.dashclock.finance.AppUtil.GetQuotesAsyncTask.GetQuotesAsyncTaskCompletionNotifier;
import com.prasanna.android.dashclock.finance.SearchCompanyAsyncTask.AsyncTaskCompletionNotifier;
import com.prasanna.android.dashclock.finance.SearchCompanyAsyncTask.Company;

public class SymbolSearchFragment extends ListFragment
{
    private TextView searchTermTv;
    private ImageView searchActionIv;
    private SearchResultAdapter searchResultAdapter;
    private HashSet<String> savedSymbolSet = new HashSet<String>();
    private ArrayList<Company> companySearchResults;
    private Context appContext;
    private View rootView;
    private ProgressBar progressBar;
    private OnTickerAddListener onTickerAddListener;

    public interface OnTickerAddListener
    {
        void onTickerAdd(Company companySearchResult);
    }

    private class SearchCompleteNotifier implements AsyncTaskCompletionNotifier<List<Company>>
    {
        public void onAsyncTaskComplete(List<Company> result)
        {
            progressBar.setVisibility(View.GONE);
            companySearchResults.clear();
            searchResultAdapter.notifyDataSetInvalidated();
            companySearchResults.addAll(result);
            searchResultAdapter.notifyDataSetChanged();
        }
    }

    static class SearchViewHolder
    {
        TextView companyNameTv;
        ImageView addTickerIv;
        ImageView tickerPresentIv;
    }

    private class SearchResultAdapter extends ArrayAdapter<Company>
    {

        public SearchResultAdapter(Context context, int resource, int textViewResourceId, List<Company> objects)
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
                searchViewHolder.addTickerIv = (ImageView) convertView.findViewById(R.id.addTicker);
                searchViewHolder.tickerPresentIv = (ImageView) convertView.findViewById(R.id.tickerPresent);
                convertView.setTag(searchViewHolder);
            }
            else
                searchViewHolder = (SearchViewHolder) convertView.getTag();

            prepareView(getItem(position), searchViewHolder);
            return convertView;
        }

        private void prepareView(final Company company, SearchViewHolder searchViewHolder)
        {
            searchViewHolder.companyNameTv.setText(company.name + " (" + company.symbol + ")");
            searchViewHolder.addTickerIv.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View v)
                {
                    if (!savedSymbolSet.contains(company.symbol))
                    {
                        if (AppUtil.canAddTicker(appContext))
                        {
                            AppUtil.addTicker(appContext, company);
                            onTickerAddListener.onTickerAdd(company);
                            savedSymbolSet.add(company.symbol);
                            refresh();
                        }
                        else
                            Toast.makeText(getActivity(), "You can have only a maximum of 5 symbols", Toast.LENGTH_LONG)
                                            .show();
                    }
                }
            });

            if (savedSymbolSet.contains(company.symbol))
            {
                searchViewHolder.addTickerIv.setVisibility(View.GONE);
                searchViewHolder.tickerPresentIv.setVisibility(View.VISIBLE);
            }
            else
            {
                searchViewHolder.addTickerIv.setVisibility(View.VISIBLE);
                searchViewHolder.tickerPresentIv.setVisibility(View.GONE);
            }
        }

    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        if (!(activity instanceof OnTickerAddListener))
            throw new IllegalArgumentException("Activity must implement OnTickerAddListener");

        appContext = activity.getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.symbol_search, null);

        String savedSymbols = AppUtil.getSavedSymbols(appContext);

        if (savedSymbols != null)
            savedSymbolSet.addAll(Arrays.asList(savedSymbols.split(",")));

        searchTermTv = (TextView) rootView.findViewById(R.id.searchTerm);
        searchActionIv = (ImageView) rootView.findViewById(R.id.searchAction);
        companySearchResults = new ArrayList<Company>();
        searchResultAdapter =
                        new SearchResultAdapter(appContext, R.layout.company_search_result_item, R.id.companyName,
                                        companySearchResults);
        progressBar = (ProgressBar) LayoutInflater.from(appContext).inflate(R.layout.progress_bar, null);
        progressBar.setVisibility(View.GONE);

        searchActionIv.setOnClickListener(new View.OnClickListener()
        {

            public void onClick(View v)
            {
                progressBar.setVisibility(View.VISIBLE);
                CharSequence searchTerm = searchTermTv.getText();

                if (searchTerm != null && searchTerm.length() > 0)
                    new SearchCompanyAsyncTask(new SearchCompleteNotifier()).execute(searchTerm.toString());
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        getListView().addFooterView(progressBar);
        getListView().setOnItemLongClickListener(new OnItemLongClickListener()
        {

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
            {
                Company company = searchResultAdapter.getItem(position);
                PopupWindow popupWindow = new PopupWindow(getActivity());
                popupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
                popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
                popupWindow.setTouchable(true);
                popupWindow.setFocusable(true);
                popupWindow.setOutsideTouchable(true);
                popupWindow.setBackgroundDrawable(new BitmapDrawable(getActivity().getResources()));
                popupWindow.setContentView(getSymbolPopupQuoteView(company));
                popupWindow.showAsDropDown(view);
                return true;
            }
        });
        setListAdapter(searchResultAdapter);
    }

    private View getSymbolPopupQuoteView(final Company company)
    {
        final View view = LayoutInflater.from(appContext).inflate(R.layout.popup_quote, null);
        ((TextView) view.findViewById(R.id.companySymbol)).setText(company.symbol);
        ((TextView) view.findViewById(R.id.companyName)).setText(company.name);

        new GetQuotesAsyncTask(new GetQuotesAsyncTaskCompletionNotifier()
        {

            @Override
            public void onGetQuotes(Map<String, Company> result)
            {
                Company companyWithPrice = result.get(company.symbol);
                ((TextView) view.findViewById(R.id.stockPrice)).setText(companyWithPrice.realTimePrice);
                TextView stockPriceChangeTv = (TextView) view.findViewById(R.id.stockPriceChange);

                float priceChange = Float.parseFloat(companyWithPrice.realTimeChange);
                if (priceChange >= 0)
                    stockPriceChangeTv.setTextColor(getActivity().getResources().getColor(R.color.darkGreen));
                else
                    stockPriceChangeTv.setTextColor(Color.RED);
                stockPriceChangeTv.setText(AppUtil.formatFloatValue(companyWithPrice.realTimeChange));
            }
        }).execute(company.symbol);
        return view;
    }

    private void refresh()
    {
        searchResultAdapter.notifyDataSetChanged();
    }

    public void setOnTickerAddListener(OnTickerAddListener onTickerAddListener)
    {
        this.onTickerAddListener = onTickerAddListener;
    }

    public void removeSymbol(String symbol)
    {
        if (symbol != null)
        {
            Log.d(SymbolSearchFragment.class.getSimpleName(), "Removing " + symbol);
            savedSymbolSet.remove(symbol);
            refresh();
        }
    }

}
