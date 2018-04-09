package com.ubicomp.jeeshn.steptracker;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

// Adapter used by the Main activity to transition between views
public class StepTrackerPagerAdapter extends FragmentPagerAdapter {
    Context mContext;

    public StepTrackerPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return new UserView();
        } else {
            return new DebugView();
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position) {
            case 0:
                return mContext.getString(R.string.usertab_name);
            case 1:
                return mContext.getString(R.string.debugtab_name);
            default:
                return null;
        }
    }
}
