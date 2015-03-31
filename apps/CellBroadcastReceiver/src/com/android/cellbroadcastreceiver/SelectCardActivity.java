/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.android.cellbroadcastreceiver;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mediatek.xlog.Xlog;

public class SelectCardActivity extends ListActivity {
    private static final String TAG = "[ETWS]CB/SelectCardActivity";
    private ListView mListView;
    private SimpleAdapter mAdapter;
    public static final String KEY_SIM_ICON    = "icon";
    public static final String KEY_SIM_TITLE   = "title";
    private int mSlot0SimId = -1;
    private int mSlot1SimId = -1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initList();
    }

    private void initList() {
        mListView = getListView();
        mAdapter = new SimpleAdapter(this, getSimInfoList(), R.layout.select_card_list_item,
                                    new String[]{KEY_SIM_ICON, KEY_SIM_TITLE},
                                    new int[]{R.id.icon, R.id.title});
        setListAdapter(mAdapter);
        
    }

    private List<Map<String, Object>> getSimInfoList() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        
        SimInfoRecord sim1Info = SimInfoManager.getSimInfoBySlot(this, 0);
        if (sim1Info != null) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(KEY_SIM_ICON, (Integer)(sim1Info.mSimBackgroundRes));
            map.put(KEY_SIM_TITLE, sim1Info.mDisplayName);
            list.add(map);
            mSlot0SimId = (int)sim1Info.mSimInfoId;
            Xlog.d(TAG, "add first card");
        }
        
        SimInfoRecord sim2Info = SimInfoManager.getSimInfoBySlot(this, 1);
        if (sim2Info != null) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(KEY_SIM_ICON, (Integer)(sim2Info.mSimBackgroundRes));
            map.put(KEY_SIM_TITLE, sim2Info.mDisplayName);
            list.add(map);
            mSlot1SimId = (int)sim2Info.mSimInfoId;
            Xlog.d(TAG, "add second card");
        }
        return list;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent it = new Intent(this, CellBroadcastSettings.class);
        if (isTwoCard()) {
            if (position == 0) {
                it.putExtra("sim_id", mSlot0SimId);
            } else if (position == 1){
                it.putExtra("sim_id", mSlot1SimId);
            } else {
                Xlog.e(TAG, "invalid position:" + position);
            }
        } else {
            //only one card
            it.putExtra("sim_id", mSlot0SimId == -1?mSlot1SimId:mSlot0SimId);
        }
        finish();
        startActivity(it);
    }

    private boolean isTwoCard() {
        return ((mSlot0SimId != -1) && (mSlot1SimId != -1));
    }
}
