package com.prasanna.android.dashclock.finance;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.prasanna.android.dashclock.finance.SwipeDismissListViewTouchListener.DismissCallbacks;

public class SymbolListFragment extends ListFragment
{
    private Context appContext;
    private ArrayList<String> savedSymbolList = new ArrayList<String>();
    private ArrayAdapter<String> symbolArrayAdapter;
    private View emptySymbolsView;
    private OnTickerRemoveListener onTickerRemoveListener;

    public interface OnTickerRemoveListener
    {
        void onTickerRemove(String symbol);
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        if (!(activity instanceof OnTickerRemoveListener))
            throw new IllegalArgumentException("Activity must implement OnTickerRemoveListener");
        
        appContext = activity.getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        emptySymbolsView = LayoutInflater.from(appContext).inflate(R.layout.empty_symbols, null);
        emptySymbolsView.setVisibility(View.GONE);
        symbolArrayAdapter = new ArrayAdapter<String>(appContext, R.layout.symbol, R.id.symbol, savedSymbolList);
        return inflater.inflate(R.layout.symbol_list, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        getListView().addFooterView(emptySymbolsView);

        SwipeDismissListViewTouchListener touchListener =
                        new SwipeDismissListViewTouchListener(getListView(), new DismissCallbacks()
                        {
                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions)
                            {
                                for (int position : reverseSortedPositions)
                                {
                                    String symbol = symbolArrayAdapter.getItem(position);
                                    savedSymbolList.remove(symbol);
                                    onTickerRemoveListener.onTickerRemove(symbol);
                                }

                                AppUtil.saveTickers(appContext, savedSymbolList);
                                symbolArrayAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public boolean canDismiss(int position)
                            {
                                return true;
                            }
                        });

        getListView().setOnTouchListener(touchListener);
        getListView().setOnScrollListener(touchListener.makeScrollListener());
        setListAdapter(symbolArrayAdapter);

        showSavedSymbols();
    }

    private void showSavedSymbols()
    {
        String savedSymbols = AppUtil.getSavedSymbols(appContext);

        if (savedSymbols != null)
        {
            savedSymbolList.addAll(Arrays.asList(savedSymbols.split(",")));
            symbolArrayAdapter.notifyDataSetChanged();
        }
    }

    public void refresh()
    {
        savedSymbolList.clear();
        symbolArrayAdapter.notifyDataSetChanged();

        showSavedSymbols();
    }

    public void setOnTickerRemoveListener(OnTickerRemoveListener onTickerRemoveListener)
    {
        this.onTickerRemoveListener = onTickerRemoveListener;
    }
}
