package nl.ps.mywsan;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class TabsAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    public TabsAdapter(FragmentManager fm, int NoofTabs) {
        super(fm);
        this.mNumOfTabs = NoofTabs;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                ConnectionFragment connection = new ConnectionFragment();
                return connection;
            case 1:
                AggregationFragment aggregation = new AggregationFragment();
                return aggregation;
            case 2:
                AnalyticsFragment analytics = new AnalyticsFragment();
                return analytics;
            default:
                return null;
        }
    }
}
