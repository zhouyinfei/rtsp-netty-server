package com.zhuyun.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 
 * @author zhouyinfei
 *
 */
public class ReadFromFile {
	 public static byte[] toByteArray(String filename) throws IOException {
		 
	        File f = new File(filename);
	        if (!f.exists()) {
	            throw new FileNotFoundException(filename);
	        }
	 
	        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
	        BufferedInputStream in = null;
	        try {
	            in = new BufferedInputStream(new FileInputStream(f));
	            int bufSize = 1024;
	            byte[] buffer = new byte[bufSize];
	            int len = 0;
	            while (-1 != (len = in.read(buffer, 0, bufSize))) {
	                bos.write(buffer, 0, len);
	            }
	            return bos.toByteArray();
	        } catch (IOException e) {
	            e.printStackTrace();
	            throw e;
	        } finally {
	            try {
	                in.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	            bos.close();
	        }
	    }
}
