package com.zhuyun.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.zhuyun.media.MediaSdpInfo;

/**
 * sdp解析
 * @author zhouyinfei
 *
 */
public class SdpParser {

	//从sdp中提取video、audio的内容，包括
	public static Map<String, MediaSdpInfo> parse(String sdp){
//		v=0
//		o=- 0 0 IN IP4 127.0.0.1
//		s=No Name
//		c=IN IP4 127.0.0.1
//		t=0 0
//		a=tool:libavformat 58.35.101
//		m=video 0 RTP/AVP 96
//		b=AS:1893
//		a=rtpmap:96 H264/90000
//		a=fmtp:96 packetization-mode=1; sprop-parameter-sets=Z0IAH5Y1QKALdNwEBAQI,aM4xsg==; profile-level-id=42001F
//		a=control:streamid=0
//		m=audio 0 RTP/AVP 97
//		b=AS:22
//		a=rtpmap:97 MPEG4-GENERIC/16000/1
//		a=fmtp:97 profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3; config=1408
//		a=control:streamid=1
		
		Map<String, MediaSdpInfo> mediaSdpInfoMap = new HashMap<String, MediaSdpInfo>();
		String[] lines = StringUtils.split(sdp, "\n");
		String lastMedia = null;								//最近一次解析到的媒体类型
		MediaSdpInfo videoSdpInfo = null;
		MediaSdpInfo audioSdpInfo = null;
		for (String line : lines) {
			if ("m".equals(StringUtils.split(line, "=")[0])) {		//=前面的标签如果是m
				String mediaInfo = StringUtils.split(line.trim(), "=")[1];
				if (mediaInfo.startsWith("video")) {	//视频参数
					videoSdpInfo = new MediaSdpInfo();
					String[] videoInfo = StringUtils.split(mediaInfo, " ");				//一个空格分隔
					videoSdpInfo.setMedia("video");
					videoSdpInfo.setRtpPayloadType(Integer.parseInt(videoInfo[3]));     //最后一个字段是payload类型
					lastMedia = "video";				
				}else if (mediaInfo.startsWith("audio")) {	//音频参数
					audioSdpInfo = new MediaSdpInfo();
					audioSdpInfo.setCodec("MPEG4-GENERIC"); 				//设置音频的默认编码
					String[] audioInfo = StringUtils.split(mediaInfo, " ");				//一个空格分隔
					audioSdpInfo.setMedia("audio");
					audioSdpInfo.setRtpPayloadType(Integer.parseInt(audioInfo[3]));     //最后一个字段是payload类型
					lastMedia = "audio";	
				}
			}
			
			if ("a".equals(StringUtils.split(line, "=")[0])) {		//=前面的标签如果是a,后面是rtpmap
				String mediaInfo = line.trim().substring(2);
				if (mediaInfo.startsWith("rtpmap")) {	//视频参数
					String[] mediaInfoList = StringUtils.split(mediaInfo, " ");				//一个空格分隔
					if ("video".equals(lastMedia)) {
						videoSdpInfo.setCodec(mediaInfoList[1].split("/")[0]);			//设置video的编解码格式
						videoSdpInfo.setClockRate(Integer.parseInt(mediaInfoList[1].split("/")[1]));	//设置采样率
					} else if ("audio".equals(lastMedia)) {
						audioSdpInfo.setCodec(mediaInfoList[1].split("/")[0]);			//设置audio的编解码格式
						audioSdpInfo.setClockRate(Integer.parseInt(mediaInfoList[1].split("/")[1]));	//设置采样率
					}
					
				} else if (mediaInfo.startsWith("control")) {					//stream流
					String[] mediaInfoList = StringUtils.split(mediaInfo, ":");				//;分隔
					if ("video".equals(lastMedia)) {
						videoSdpInfo.setControl(mediaInfoList[1]);		
					} else if ("audio".equals(lastMedia)) {
						audioSdpInfo.setControl(mediaInfoList[1]);
					}
				}
			}
		}
		
		if(videoSdpInfo != null){
			mediaSdpInfoMap.put("video", videoSdpInfo);
		}

		if (audioSdpInfo != null) {
			mediaSdpInfoMap.put("audio", audioSdpInfo);
		}
		return mediaSdpInfoMap;
		
	}
	
	public static void main(String[] args) {
		String sdp = "v=0\n" + 
				"o=- 0 0 IN IP4 127.0.0.1\n" +
				"s=No Name\n" +
				"c=IN IP4 127.0.0.1\n" +
				"t=0 0\n" +
				"a=tool:libavformat 58.35.101\n" +
				"m=video 0 RTP/AVP 96\n" +
				"b=AS:1893\n" +
				"a=rtpmap:96 H264/90000\n" +
				"a=fmtp:96 packetization-mode=1; sprop-parameter-sets=Z0IAH5Y1QKALdNwEBAQI,aM4xsg==; profile-level-id=42001F\n" +
				"a=control:streamid=0\n" +
				"m=audio 0 RTP/AVP 97\n" +
				"b=AS:22\n" +
				"a=rtpmap:97 MPEG4-GENERIC/16000/1\n" +
				"a=fmtp:97 profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3; config=1408\n" +
				"a=control:streamid=1";
		Map<String, MediaSdpInfo> parser = parse(sdp);
		System.out.println(parser);
	}
	
	
}
