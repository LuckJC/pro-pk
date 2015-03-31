package com.example.xuntongwatch.util;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.example.xuntongwatch.entity.TelephoneAdress;

public class XMLSax {

	public ArrayList<TelephoneAdress> getSAXPersons(InputStream inStream)
			throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();// 创建解析器
		TelephoneAdressHandler handler = new TelephoneAdressHandler();
		parser.parse(inStream, handler);
		inStream.close();
		return handler.getPersons();
	}

	private class TelephoneAdressHandler extends DefaultHandler {
		// <list>
		// <id>119054</id>
		// <num>1557278</num>
		// <code>0717</code>
		// <city>湖北省宜昌市</city>
		// <cardtype>湖北联通GSM卡</cardtype>
		// </list>

		private ArrayList<TelephoneAdress> entitys = null;
		private TelephoneAdress entity;
		private String tagName = null;// 当前解析的元素标签
		private int i = 0;

		public ArrayList<TelephoneAdress> getPersons() {
			return entitys;
		}

		@Override
		public void startDocument() throws SAXException {
			entitys = new ArrayList<TelephoneAdress>();
		}

		@Override
		public void startElement(String namespaceURI, String localName,
				String qName, Attributes atts) throws SAXException {

			if (localName.equals("list")) {
				entity = new TelephoneAdress();
			}
			this.tagName = localName;
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {

			if (tagName != null) {
				String data = new String(ch, start, length);
				if (tagName.equals("num")) {
					entity.setTelephone(data);
				} else if (tagName.equals("code")) {
					entity.setAreaCode(data);
				} else if (tagName.equals("city")) {
					entity.setCity(data);
				} else if (tagName.equals("cardtype")) {
					entity.setCardType(data);
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String name)
				throws SAXException {

			if (localName.equals("list")) {
				entitys.add(entity);
				// Log.e("", "~~~~~~~~~" + i);
				i++;
				entity = null;
			}

			this.tagName = null;
		}
	}

}
