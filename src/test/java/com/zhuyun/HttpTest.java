package com.zhuyun;

import com.zhuyun.utils.HttpConnection;

public class HttpTest {

	public static void main(String[] args) {
		for (int i = 0; i < 100; i++) {
			String string = HttpConnection.get("http://localhost:8080/springmvc/eco?username=a&password=b");
			System.out.println(string);
		}
		
	}
}
