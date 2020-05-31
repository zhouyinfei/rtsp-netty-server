package com.zhuyun.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.RandomStringUtils;

public class SessionFactory {
	public static Map<String, String> sessionStore = new ConcurrentHashMap<String, String>();
		
    
	private static final int HEADER_COUNT = 8;
	private static final int FOOTER_COUNT = 10;
	private static final String DELIMITER = "-";
	
	public static synchronized String createSession() {
		String header = RandomStringUtils.randomNumeric(HEADER_COUNT);
		String footer = RandomStringUtils.randomNumeric(FOOTER_COUNT);
		return header + DELIMITER + footer;
	}
	
	public static String getSession(String session){
		return sessionStore.get(session);
	}
	
	public static void putSession(String session, String keyhash){
		sessionStore.put(session, keyhash);
	}
	
	public static void removeSession(String session){
		sessionStore.remove(session);
	}
	  
}
