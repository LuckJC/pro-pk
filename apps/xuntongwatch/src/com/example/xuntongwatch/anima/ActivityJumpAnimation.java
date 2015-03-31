package com.example.xuntongwatch.anima;

import com.example.xuntongwatch.R;

import android.app.Activity;

public class ActivityJumpAnimation {

	/**
	 * 左进
	 * @param act
	 */
	public static void LeftInto(Activity act)
	{
		act.overridePendingTransition(R.anim.activity_into_left_to_right, R.anim.activity_back_left_to_right);
	}
	/**
	 * 右进
	 * @param act
	 */
	public static void RightInto(Activity act)
	{
		act.overridePendingTransition( R.anim.activity_into_right_to_left,R.anim.activity_back_right_to_left);
	}
	/**
	 * 上进
	 * @param act
	 */
	public static void UpInto(Activity act)
	{
		act.overridePendingTransition(R.anim.activity_into_up_to_down,R.anim.activity_back_up_to_down);
	}
	/**
	 * 下进
	 * @param act
	 */
	public static void DownInto(Activity act)
	{
		act.overridePendingTransition(R.anim.activity_into_down_to_up,R.anim.activity_back_down_to_up);
	}
	
	/**
	 * 左出
	 * @param act
	 */
	public static void LeftBack(Activity act)
	{
		act.overridePendingTransition(R.anim.activity_into_left_to_right,R.anim.activity_back_left_to_right);
	}
	/**
	 * 右出
	 * @param act
	 */
	public static void RightBack(Activity act)
	{
		act.overridePendingTransition(R.anim.activity_into_right_to_left,R.anim.activity_back_right_to_left);
	}
	/**
	 * 上出
	 * @param act
	 */
	public static void UpBack(Activity act)
	{
		act.overridePendingTransition(R.anim.activity_into_up_to_down,R.anim.activity_back_up_to_down);
	}
	/**
	 * 下出
	 * @param act
	 */
	public static void DownBack(Activity act)
	{
		act.overridePendingTransition(R.anim.activity_into_down_to_up,R.anim.activity_back_down_to_up);
	}
	
	
}
