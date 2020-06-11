package com.zhuyun.utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * @author CC
 * 2018年6月19日 下午6:18:58
 */
public class HttpConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpConnectionManager.class);


    public static String post(String path) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        HttpPost httpPost = new HttpPost(path);
        httpPost.setHeader("Content-Type", "application/json;charset=utf-8");
        httpPost.setConfig(RequestConfig.custom().setConnectTimeout(10000).setConnectionRequestTimeout(10000).setSocketTimeout(10000).build());
        try {
            response = httpClient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            return EntityUtils.toString(httpEntity);
        } catch (IOException e) {
            LOGGER.error("系统错误", e);
        }finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOGGER.error("系统错误", e);
                }
            }
        }
        return "";
    }

    public static String post(String path, String params) {
        String result="";
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        HttpPost httpPost = new HttpPost(path);
        httpPost.setHeader("Content-Type", "application/json;charset=utf-8");
        httpPost.setConfig(RequestConfig.custom().setConnectTimeout(10000).setConnectionRequestTimeout(10000).setSocketTimeout(10000).build());
        StringEntity entity = new StringEntity(params, Charset.forName("UTF-8"));
        httpPost.setEntity(entity);
        try {
            response = httpClient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            result = EntityUtils.toString(httpEntity, "utf-8");
            //System.out.println(result);
        } catch (IOException e) {
            LOGGER.error("系统错误", e);
        }finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOGGER.error("系统错误", e);
                }
            }
        }
        return result;
    }
    public static String get(String path) {
        String result = "";
        CloseableHttpClient httpClient = HttpConnectionManager.getHttpClient();
        HttpGet httpget = new HttpGet(path);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000).setConnectionRequestTimeout(10000)
                .setSocketTimeout(10000).build();
        httpget.setConfig(requestConfig);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpget);
            InputStream in = response.getEntity().getContent();

            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            // 存放数据
            StringBuffer sbf = new StringBuffer();
            String temp = null;
            while ((temp = br.readLine()) != null) {
                sbf.append(temp);
            }
            result = sbf.toString();

            in.close();
        } catch (UnsupportedOperationException e) {
            LOGGER.error("系统错误", e);
        } catch (IOException e) {
            LOGGER.error("系统错误", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOGGER.error("系统错误", e);
                }
            }
        }
        return result;
    }

}
