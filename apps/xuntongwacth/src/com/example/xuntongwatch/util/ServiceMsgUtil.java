package com.example.xuntongwatch.util;

import android.text.TextUtils;


public class ServiceMsgUtil {

	public static String getString(String address)
	{
		String str = "";
		for (int i = 0; i < str1s.length; i++) {
			String str2 = str1s[i];
			String str3 = str2s[i];
			str = getStr(address.trim(),str2.trim(),str3.trim());
			if(!TextUtils.isEmpty(str))
				break;
		}
		return str;
	}
	
	private static String getStr(String str1,String str2,String str3)
	{
		if(str1.equals(str2))
		{
			return str3;
		}
		return "";
	}
	
	private static final String[] str1s = new String[]
	{
		"12306",
		"10000",
		"10010",
		"10011",
		"10050",
		"10060",
		"10086",
		"95500",
		"95501",
		"95511",
		"95512",
		"95518",
		"95519",
		"95522",
		"95533",
		"95555",
		"95558",
		"95559",
		"95561",
		"95566",
		"95567",
		"95568",
		"95577",
		"95588",
		"95595",
		"95599"
	};
	private static final String[] str2s = new String[]
	{
		"火车票预订",
		"中国电信 ",
		"中国联通",
		"中国联通话费查询 ",
		"中国铁通",
		"中国网通",
		"中国移动",
		"中国太平洋保险",
		"华夏银行咨询",
		"中国平安人寿保险",
		"中国平安财产保险  ",
		"中国人民保险",
		"中国人寿保险",
		"太康人寿保险保险",
		"中国建设银行",
		"招商银行电话银行",
		"中信实业银行客服咨询",
		"交通银行",
		"兴业银行中心 ",
		"中国银行  ",
		"新华人寿保险", 
		"中国民生银行客服咨询",
		"华夏银行咨询",
		"中国工商银行",
		"中国光大银行",
		"中国农业银行"
	};
}
