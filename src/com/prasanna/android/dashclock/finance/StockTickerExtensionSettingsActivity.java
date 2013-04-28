package com.prasanna.android.dashclock.finance;

import com.prasanna.android.dashclock.finance.SearchCompanyAsyncTask.Company;
import com.prasanna.android.dashclock.finance.SymbolListFragment.OnTickerRemoveListener;
import com.prasanna.android.dashclock.finance.SymbolSearchFragment.OnTickerAddListener;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class StockTickerExtensionSettingsActivity extends FragmentActivity implements OnTickerAddListener, OnTickerRemoveListener
{
    private SymbolListFragment symbolListFragment;
    private SymbolSearchFragment symbolSearchFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);

        symbolListFragment =
                        (SymbolListFragment) getSupportFragmentManager().findFragmentById(R.id.symbol_list_fragment);
        symbolSearchFragment =
                        (SymbolSearchFragment) getSupportFragmentManager()
                                        .findFragmentById(R.id.symbol_search_fragment);
        
        symbolListFragment.setOnTickerRemoveListener(this);
        symbolSearchFragment.setOnTickerAddListener(this);
    }
    

    @Override
    public void onTickerAdd(Company companySearchResult)
    {
        symbolListFragment.refresh();
    }

    @Override
    public void onTickerRemove(String symbol)
    {
        symbolSearchFragment.removeSymbol(symbol);
    }
}
