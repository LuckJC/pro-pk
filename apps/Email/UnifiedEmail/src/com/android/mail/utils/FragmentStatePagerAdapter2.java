/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.utils;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentCompat;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Forked from support lib's {@link FragmentStatePagerAdapter}, with some minor
 * changes that couldn't be accomplished through subclassing:
 * <ul>
 * <li>optionally disable stateful behavior when paging (controlled by {@link #mEnableSavedStates}),
 * for situations where state save/restore when paging is unnecessary</li>
 * <li>override-able {@link #setItemVisible(Fragment, boolean)} method for subclasses to
 * add supplemental handling of visibility hints manually on pre-v15 devices</li>
 * <li>add support to handle data set changes that cause item positions to change</li>
 * <li>allow read access to existing Fragments by index ({@link #getFragmentAt(int)})</li>
 * </ul>
 */
public abstract class FragmentStatePagerAdapter2 extends PagerAdapter {
    private static final String TAG = "FragmentStatePagerAdapter";
    private static final boolean DEBUG = false;

    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;

    private ArrayList<Fragment.SavedState> mSavedState = new ArrayList<Fragment.SavedState>();
    private SparseArrayCompat<Fragment> mFragments = new SparseArrayCompat<Fragment>();
    private Fragment mCurrentPrimaryItem = null;

    private boolean mEnableSavedStates;

    public FragmentStatePagerAdapter2(FragmentManager fm) {
        this(fm, true);
    }

    public FragmentStatePagerAdapter2(FragmentManager fm, boolean enableSavedStates) {
        mFragmentManager = fm;
        mEnableSavedStates = enableSavedStates;
    }

    /**
     * Return the Fragment associated with a specified position.
     */
    public abstract Fragment getItem(int position);

    @Override
    public void startUpdate(ViewGroup container) {
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        // If we already have this item instantiated, there is nothing
        // to do.  This can happen when we are restoring the entire pager
        // from its saved state, where the fragment manager has already
        // taken care of restoring the fragments we previously had instantiated.
        if (DEBUG) LogUtils.v(TAG, "instantiateItem at [" + position + "] fragments: " + mFragments);
        final Fragment existing = mFragments.get(position);
        if (existing != null) {
            return existing;
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        Fragment fragment = getItem(position);
        if (DEBUG) LogUtils.v(TAG, "Adding item #" + position + ": f=" + fragment);
        if (mEnableSavedStates && mSavedState.size() > position) {
            Fragment.SavedState fss = mSavedState.get(position);
            if (fss != null) {
                fragment.setInitialSavedState(fss);
            }
        }
        if (fragment != mCurrentPrimaryItem) {
            setItemVisible(fragment, false);
        }
        mFragments.put(position, fragment);
        mCurTransaction.add(container.getId(), fragment);

        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment)object;

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        if (DEBUG) LogUtils.v(TAG, "Removing item #" + position + ": f=" + object
                + " v=" + ((Fragment)object).getView());
        if (mEnableSavedStates) {
            while (mSavedState.size() <= position) {
                mSavedState.add(null);
            }
            mSavedState.set(position, mFragmentManager.saveFragmentInstanceState(fragment));
        }
        ///M: Do not delete anyone else but itself from mFragments which maybe leaked as situation 3 @{
        if (fragment.equals(mFragments.get(position))) {
            mFragments.delete(position);
        } else {
            if (DEBUG) LogUtils.v(TAG, "Skip to Remove item #" + position + ": f=" + mFragments.get(position));
        }
        ///M: @}

        mCurTransaction.remove(fragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment)object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                setItemVisible(mCurrentPrimaryItem, false);
            }
            if (fragment != null) {
                setItemVisible(fragment, true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment)object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        Bundle state = null;
        if (mEnableSavedStates && mSavedState.size() > 0) {
            state = new Bundle();
            Fragment.SavedState[] fss = new Fragment.SavedState[mSavedState.size()];
            mSavedState.toArray(fss);
            state.putParcelableArray("states", fss);
        }
        for (int i=0; i<mFragments.size(); i++) {
            final int pos = mFragments.keyAt(i);
            final Fragment f = mFragments.valueAt(i);
            if (state == null) {
                state = new Bundle();
            }
            String key = "f" + pos;
            mFragmentManager.putFragment(state, key, f);
        }
        if (DEBUG) LogUtils.v(TAG, "saveState mFragments: " + mFragments);
        return state;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        if (state != null) {
            Bundle bundle = (Bundle)state;
            bundle.setClassLoader(loader);
            mFragments.clear();
            if (mEnableSavedStates) {
                Parcelable[] fss = bundle.getParcelableArray("states");
                mSavedState.clear();
                if (fss != null) {
                    for (int i=0; i<fss.length; i++) {
                        mSavedState.add((Fragment.SavedState)fss[i]);
                    }
                }
            }
            Iterable<String> keys = bundle.keySet();
            for (String key: keys) {
                if (key.startsWith("f")) {
                    int index = Integer.parseInt(key.substring(1));
                    Fragment f = mFragmentManager.getFragment(bundle, key);
                    if (f != null) {
                        setItemVisible(f, false);
                        mFragments.put(index, f);
                    } else {
                        LogUtils.w(TAG, "Bad fragment at key " + key);
                    }
                }
            }
            if (DEBUG) LogUtils.v(TAG, "restoreState mFragments: " + mFragments);
        }
    }

    public void setItemVisible(Fragment item, boolean visible) {
        FragmentCompat.setMenuVisibility(item, visible);
        FragmentCompat.setUserVisibleHint(item, visible);
    }

    @Override
    public void notifyDataSetChanged() {
        if (DEBUG) LogUtils.v(TAG, "notifyDataSetChanged <<<<<<<<<<<<<<<<");
        // update positions in mFragments
        SparseArrayCompat<Fragment> newFragments =
                new SparseArrayCompat<Fragment>(mFragments.size());
        /**
         * M: There are 4 kinds of situations lead Fragment leaked:
         * 1. Fragments in mFragments without available newpos
         * 2. Fragment with newpos 0 equals to an old fragment with other position
         * 3. Fragment with newpos 0 does not equals the fragment at position 0
         * 4. Very special, SaveState of FragmentManager before fragments changes are committed
         * Root cause: The cursor(ConversationListCursor) of PagerAdpater after fragment restored are NULL
         * TODO: We would better find a way to make the cursor available before the first instantiate happens
         * @{
         */
        if (DEBUG) LogUtils.v(TAG, "Current fragments: " + mFragments);
        boolean commitInstantly = false;
        for (int i=0; i<mFragments.size(); i++) {
            final int oldPos = mFragments.keyAt(i);
            final Fragment f = mFragments.valueAt(i);
            final int newPos = getItemPosition(f);

            if (DEBUG) LogUtils.v(TAG, "Changed item "+ f + " from #" + oldPos + " to #" + newPos);
            if (newPos != POSITION_NONE) {
                final int pos = (newPos >= 0) ? newPos : oldPos;
                if (newFragments.get(pos) != null) {
                    if (DEBUG) LogUtils.v(TAG, "Already have one fragment, just delete the old one");
                    removeFragment(f);
                    commitInstantly = true;
                    continue;
                }
                newFragments.put(pos, f);
            } else {
                removeFragment(f);
                commitInstantly = true;
            }
        }
        if (commitInstantly) {
            commitFragmentChanges();
        }
        mFragments = newFragments;
        if (DEBUG) LogUtils.v(TAG, "New items "+ mFragments);
        /** M: @} */

        super.notifyDataSetChanged();
        if (DEBUG) LogUtils.v(TAG, "notifyDataSetChanged >>>>>>>>>>>>>>>>");
    }

    /**
     * M: Remove fragment which is unavailable
     * @param f fragment need to be removed from FragmentManager
     */
    private void removeFragment(Fragment f) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        if (f != null) {
            mCurTransaction.remove(f);
            if (DEBUG) LogUtils.v(TAG, "Removed item "+ f);
        }
    }


    /**
     * M: Committed fragments changes right away
     */
    private void commitFragmentChanges() {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
            if (DEBUG) LogUtils.v(TAG, "commitFragmentChanges instantly");
        }
    }

    public Fragment getFragmentAt(int position) {
        return mFragments.get(position);
    }
}
