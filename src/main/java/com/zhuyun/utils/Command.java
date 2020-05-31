package com.zhuyun.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
public class Command {
	public static void exeCmd(String commandStr) {
		BufferedReader br = null;
		try {
			Process p = Runtime.getRuntime().exec(commandStr);
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null)
			{
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
  
	public static void main(String[] args) {
		String commandStr = "ping www.baidu.com";
		//String commandStr = "ipconfig";
		Command.exeCmd(commandStr);
	}
}

