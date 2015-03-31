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

package com.android.mms.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.android.mms.R;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.mms.ext.IMmsAdvanceSearch;
/**
 * M: AdvancedSearchActivity ; ADD FOR OP09;
 *
 */
public class AdvancedSearchActivity extends Activity {
    private static final String TAG = "Mms/AdvancedSearchActivity";

    private static final int DATA_CERTAIN_ONE_DAY = 0;
    private static final int DATA_CERTAIN_SEVEN_DAY = 1;
    private static final int DATA_CERTAIN_THIRTY_DAY = 2;

    private static final int DATA_PICKER_FROM = 0;
    private static final int DATA_PICKER_TO = 1;

    private static final int BASE_YEAR = 1900;
    private static final long ONE_DAY_MILLI_SECOND = 24 * 60 * 60 * 1000L;

    private EditText mCertainTimeText;
    private AlertDialog mCertainTimePickerDialog;
    private Button mCertainTimeSearchButton;
    private EditText mFromText;
    private EditText mToText;
    private Button mSearchButton;
    private DatePickerDialog mFromPickerDialog;
    private DatePickerDialog mToPickerDialog;

    private boolean mIsClearDate = false;
    private boolean mIsCancelDatePick = false;
    private int mCurrentCertainDate = DATA_CERTAIN_SEVEN_DAY;
    private int mCurrentDatePicker = 0;
    private long mFromDate = 0;
    private long mToDate = 0;

    private java.text.DateFormat mDateFormater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MmsLog.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.advanced_search_activity);
        initResource();
        initContent();
        setTitle(R.string.search_by_time_period);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFromPickerDialog != null && mFromPickerDialog.isShowing()) {
            mFromPickerDialog.dismiss();
        }
        if (mToPickerDialog != null && mToPickerDialog.isShowing()) {
            mToPickerDialog.dismiss();
        }
    }

    private void initContent() {
        mDateFormater = DateFormat.getDateFormat(this);

        Date date = new Date(System.currentTimeMillis());
        mCurrentDatePicker = DATA_PICKER_FROM;
        setDate(date.getYear() + BASE_YEAR, date.getMonth(), date.getDate());
        mCurrentDatePicker = DATA_PICKER_TO;
        setDate(date.getYear() + BASE_YEAR, date.getMonth(), date.getDate());
    }

    private void initResource() {
        mCertainTimeText = (EditText) findViewById(R.id.certain_time_text);
        /// M: deal with the touch event;
        mCertainTimeText.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                MmsLog.e(TAG, "mCertainTimeText.onTouch()");
                if (MotionEvent.ACTION_UP == arg1.getActionMasked()) {
                    showCertainTimePickerDialog();
                    return true;
                }
                return false;
            }
        });
        mCertainTimeText.setText(R.string.search_within_seven_days);
        mCertainTimeSearchButton = (Button) findViewById(R.id.certain_time_search_button);
        /// M: set click listener for certainTimeSearchBtn;
        mCertainTimeSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long toDate = getTodayDate();
                long fromDate = getFromDate(toDate);
                MmsLog.d(TAG, "mCertainTimeText.onFocusChange(): fromDate = " + fromDate + ", toDate = " + toDate);
                Intent intent = new Intent();
                intent.setClass(AdvancedSearchActivity.this, SearchActivity.class);
                intent.putExtra(IMmsAdvanceSearch.ADVANCED_SEARCH_QUERY, true);
                intent.putExtra(IMmsAdvanceSearch.ADVANCED_SEARCH_BEGIN_DATE, (fromDate > 0L) ? fromDate : 0L);
                intent.putExtra(IMmsAdvanceSearch.ADVANCED_SEARCH_END_DATE, toDate);
                startActivity(intent);
            }
        });

        mFromText = (EditText) findViewById(R.id.from_date_text);
        /// M: set on touch listener for : if the text's focused has not got. then user click it ,it will not show the
        /// date picker dialog.
        mFromText.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if ( MotionEvent.ACTION_UP == arg1.getActionMasked()) {
                    processEditTouch(DATA_PICKER_FROM);
                    return true;
                }
                return false;
            }
        });

        mToText = (EditText) findViewById(R.id.to_date_text);
        mToText.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if (MotionEvent.ACTION_UP == arg1.getActionMasked()) {
                    processEditTouch(DATA_PICKER_TO);
                    return true;
                }
                return false;
            }
        });

        mSearchButton = (Button) findViewById(R.id.search_button);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFromDate > 0 && mToDate > 0 && mFromDate >= mToDate) {
                    Toast.makeText(AdvancedSearchActivity.this, R.string.search_error_wrong_date, Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                Intent intent = new Intent();
                intent.setClass(AdvancedSearchActivity.this, SearchActivity.class);
                intent.putExtra(IMmsAdvanceSearch.ADVANCED_SEARCH_QUERY, true);
                intent.putExtra(IMmsAdvanceSearch.ADVANCED_SEARCH_BEGIN_DATE, mFromDate);
                intent.putExtra(IMmsAdvanceSearch.ADVANCED_SEARCH_END_DATE, mToDate);
                startActivity(intent);
            }
        });
    }

    /**
     *
     * @param pickerType
     */
    private void processEditTouch(int pickerType) {
        mCurrentDatePicker = pickerType;
        if (mFromPickerDialog == null || mToPickerDialog == null) {
            initDatePickerDialog();
        }
        if (mFromPickerDialog != null && pickerType == DATA_PICKER_FROM) {
            showDatePickerDialog(mFromPickerDialog);
        } else if (mToPickerDialog != null && pickerType == DATA_PICKER_TO) {
            showDatePickerDialog(mToPickerDialog);
        } else {
            MmsLog.e(TAG, "onClick(): init date picker failed; type:" + pickerType);
        }
    }

    private void initCertainTimePickerDialog() {
        if (mCertainTimePickerDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.search_certain_time_dialog_title);
            builder.setSingleChoiceItems(getResources().getTextArray(R.array.certain_search_string_array),
                mCurrentCertainDate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MmsLog.d(TAG, "mCertainTimePickerDialog.onClick(): which = " + which);
                        mCurrentCertainDate = which;
                        switch (mCurrentCertainDate) {
                            case DATA_CERTAIN_ONE_DAY:
                                mCertainTimeText.setText(R.string.search_within_one_day);
                                break;
                            case DATA_CERTAIN_SEVEN_DAY:
                                mCertainTimeText.setText(R.string.search_within_seven_days);
                                break;
                            case DATA_CERTAIN_THIRTY_DAY:
                                mCertainTimeText.setText(R.string.search_within_thirty_days);
                                break;
                            default:
                                break;
                        }
                        dialog.dismiss();
                    }
                });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            mCertainTimePickerDialog = builder.create();
        }
    }

    private void showCertainTimePickerDialog() {
        if (mCertainTimePickerDialog == null) {
            initCertainTimePickerDialog();
        }
        if (mCertainTimePickerDialog.isShowing()) {
            mCertainTimePickerDialog.dismiss();
        }
        mCertainTimePickerDialog.show();
    }

    private long getTodayDate() {
        int year, month, day;
        Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        day = c.get(Calendar.DAY_OF_MONTH);
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        return c.getTimeInMillis() + ONE_DAY_MILLI_SECOND;
    }

    private long getFromDate(long toDate) {
        MmsLog.d(TAG, "getFromDate(): mCurrentCertainDate = " + mCurrentCertainDate + ", toDate = " + toDate);
        switch (mCurrentCertainDate) {
            case DATA_CERTAIN_ONE_DAY:
                return toDate - ONE_DAY_MILLI_SECOND;
            case DATA_CERTAIN_SEVEN_DAY:
                return toDate - (ONE_DAY_MILLI_SECOND * 7);
            case DATA_CERTAIN_THIRTY_DAY:
                return toDate - (ONE_DAY_MILLI_SECOND * 30);
            default:
                MmsLog.e(TAG, "getFromDate(): error case!", new Exception());
        }
        return 0;
    }

    private void initDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        mFromPickerDialog = new DatePickerDialog(this, new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                MmsLog.d(TAG, "onDateSet(): set begin date to text.");
                setDate(year, monthOfYear, dayOfMonth);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        mFromPickerDialog.setButton3(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsCancelDatePick = true;
            }
        });
        setDateRange(mFromPickerDialog);

        mToPickerDialog = new DatePickerDialog(this, new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                MmsLog.d(TAG, "onDateSet(): set end date to text.");
                setDate(year, monthOfYear, dayOfMonth);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        mToPickerDialog.setButton3(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsCancelDatePick = true;
            }
        });
        setDateRange(mToPickerDialog);
    }

    private void showDatePickerDialog(DatePickerDialog datePickerDialog) {
        if (datePickerDialog.isShowing()) {
            datePickerDialog.dismiss();
        }
        datePickerDialog.show();
    }

    /**
     * set date range.
     * @param dialog
     */
    private void setDateRange(DatePickerDialog dialog) {
        if (dialog != null) {
            Time minTime = new Time();
            Time maxTime = new Time();
            /// M: 1970/1/1
            minTime.set(0, 0, 0, 1, 0, 1970);
            /// M: 2037/12/31
            maxTime.set(59, 59, 23, 31, 11, 2037);
            long maxDate = maxTime.toMillis(false);
            /// M: in millsec
            maxDate = maxDate + 999;
            long minDate = minTime.toMillis(false);
            /// M: set min date
            dialog.getDatePicker().setMinDate(minDate);
            /// M: set max date;
            dialog.getDatePicker().setMaxDate(maxDate);
        }
    }

    private void setDate(int year, int month, int day) {
        MmsLog.d(TAG, "setDate(): year = " + year + ", month = " + month + ", day = " + day + ", mCurrentDatePicker = "
            + mCurrentDatePicker + ", mIsClearDate = " + mIsClearDate + ", mIsCancelDatePick = " + mIsCancelDatePick);
        if (mIsCancelDatePick) {
            mIsCancelDatePick = false;
            MmsLog.d(TAG, "setDate(): cancel.");
            return;
        }
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = mIsClearDate ? 0 : c.getTimeInMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        switch (mCurrentDatePicker) {
            case DATA_PICKER_FROM:
                mFromDate = when;
                mFromText.setText(mIsClearDate ? "" : mDateFormater.format(new Date(when)));
                MmsLog.d(TAG, "setDate(): mFromDate = " + mFromDate + ", " + sdf.format(new Date(mFromDate)));
                if (mFromDate >= mToDate) {
                    mToDate = when + (when == 0 ? 0 : ONE_DAY_MILLI_SECOND);
                    mToText.setText(mIsClearDate ? "" : mDateFormater.format(new Date(when)));
                    MmsLog.d(TAG, "setDate(): mToDate = " + mToDate + ", " + sdf.format(new Date(mToDate)));
                    if (mToPickerDialog != null) {
                        mToPickerDialog.updateDate(year, month, day);
                    }
                }
                break;
            case DATA_PICKER_TO:
                /// M: make the end date from 00:00:00 today to 00:00:00 tomorrow
                mToDate = when + (when == 0 ? 0 : ONE_DAY_MILLI_SECOND);
                mToText.setText(mIsClearDate ? "" : mDateFormater.format(new Date(when)));
                MmsLog.d(TAG, "setDate(): mToDate = " + mToDate + ", " + sdf.format(new Date(mToDate)));
                if (mFromDate >= mToDate) {
                    mFromDate = when;
                    mFromText.setText(mIsClearDate ? "" : mDateFormater.format(new Date(when)));
                    MmsLog.d(TAG, "setDate(): mFromDate = " + mFromDate + ", " + sdf.format(new Date(mFromDate)));
                    if (mFromPickerDialog != null) {
                        mFromPickerDialog.updateDate(year, month, day);
                    }
                }
                break;
            default:
                break;
        }
        mIsClearDate = false;
    }
}
