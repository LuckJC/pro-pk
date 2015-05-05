package com.shizhongkeji.speech.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Json结果解析类
 */
public class JsonParser {
	
	public static String parseIatResult(String json) {
		StringBuffer ret = new StringBuffer();
		try {
			JSONTokener tokener = new JSONTokener(json);
			JSONObject joResult = new JSONObject(tokener);

			JSONArray words = joResult.getJSONArray("ws");
			for (int i = 0; i < words.length(); i++) {
				// 转写结果词，默认使用第一个结果
				JSONArray items = words.getJSONObject(i).getJSONArray("cw");
				JSONObject obj = items.getJSONObject(0);
				ret.append(obj.getString("w"));
//				如果需要多候选结果，解析数组其他字段
//				for(int j = 0; j < items.length(); j++)
//				{
//					JSONObject obj = items.getJSONObject(j);
//					ret.append(obj.getString("w"));
//				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return ret.toString();
	}
	
	/**
	 * <br>
	 * 功能简述:解析识别的命令结果 <br>
	 * 功能详细描述:识别到命令的词后返回相应的结果给目标 <br>
	 * 注意:
	 * 
	 * @param json
	 *            命令传进来的json数据
	 * @param engType
	 *            解析方式是云端解析还是离线包解析
	 * @return
	 */
	public static Map<String, List<String>> parseGrammarResultIntent(String json, String engType) {

		StringBuffer ret = new StringBuffer();
		try {
			JSONTokener tokener = new JSONTokener(json);
			JSONObject joResult = new JSONObject(tokener);

			JSONArray words = joResult.getJSONArray("ws");
			// 云端和本地结果分情况解析
			if ("cloud".equals(engType)) {
				for (int i = 0; i < words.length(); i++) {
					JSONArray items = words.getJSONObject(i).getJSONArray("cw");
					for (int j = 0; j < items.length(); j++) {
						JSONObject obj = items.getJSONObject(j);
						if (obj.getString("w").contains("nomatch")) {
							Integer.toHexString(i);
							ret.append("没有匹配结果.");
							// return ret.toString();
						}
						ret.append("【结果】" + obj.getString("w"));
						ret.append("【置信度】" + obj.getInt("sc"));
						ret.append("\n");
					}
				}
			} else if ("local".equals(engType)) {
				// ret.append("【结果】");
				Map<String, List<String>> map = new HashMap<String, List<String>>();

				List<String> list = new ArrayList<String>();

				for (int i = 0; i < words.length(); i++) {
					JSONObject wsItem = words.getJSONObject(i);
					JSONArray items = wsItem.getJSONArray("cw");
					if ("<callPhone>".equals(wsItem.getString("slot"))) {
						for (int j = 0; j < words.length(); j++) {
							JSONObject wsItem2 = words.getJSONObject(j);
							JSONArray items2 = wsItem2.getJSONArray("cw");
							if ("<contact>".equals(wsItem2.getString("slot"))) {
								// 可能会有多个联系人供选择，用中括号括起来，这些候选项具有相同的置信度
								// ret.append("【");
								for (int k = 0; k < items2.length(); k++) {
									JSONObject obj = items2.getJSONObject(k);
									list.add(obj.getString("w"));

								}
								map.put("callPhone", list);
								return map;
							}
						}

					} else if ("<sendmsgPhone>".equals(wsItem.getString("slot"))) {
						for (int j = 0; j < words.length(); j++) {
							JSONObject wsItem2 = words.getJSONObject(j);
							JSONArray items2 = wsItem2.getJSONArray("cw");
							if ("<contact>".equals(wsItem2.getString("slot"))) {
								// 可能会有多个联系人供选择，用中括号括起来，这些候选项具有相同的置信度
								// ret.append("【");

								for (int k = 0; k < items2.length(); k++) {
									JSONObject obj = items2.getJSONObject(k);
									list.add(obj.getString("w"));

								}
								map.put("sendmsgPhone", list);
								return map;
							}

						}
					} else if ("<openApp>".equals(wsItem.getString("slot"))) {
						for (int j = 0; j < words.length(); j++) {
							JSONObject wsItem2 = words.getJSONObject(j);
							JSONArray items2 = wsItem2.getJSONArray("cw");
							if ("<appName>".equals(wsItem2.getString("slot"))) {
								// 可能会有多个联系人供选择，用中括号括起来，这些候选项具有相同的置信度
								// ret.append("【");

								for (int k = 0; k < items2.length(); k++) {
									JSONObject obj = items2.getJSONObject(k);
									list.add(obj.getString("w"));

								}
								map.put("openApp", list);
								return map;
							}
						}
					}
					/*else if("<contact>".equals(wsItem.getString("slot")))
					{
						for (int j = 0; j < words.length(); j++) {
							JSONObject wsItem2 = words.getJSONObject(j);
							JSONArray items2 = wsItem2.getJSONArray("cw");
							
								for (int k = 0; k < items2.length(); k++) {
									JSONObject obj = items2.getJSONObject(k);
									list.add(obj.getString("w"));

								}
								map.put("nothing", list);
								return map;
							
						}
					}*/
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			ret.append("没有匹配结果.");
		}
		return null;
	}
}
