package com.zhuyun.media;

public class MediaSdpInfo {
	private String media;					//媒体类型，可选范围：audio、video
	private String codec;					//编解码格式，例如：H264、H265、MPEG4-GENERIC
	private int rtpPayloadType;				//rtp包的payload类型，例如：96、97
	private int clockRate;					//采样率
	private String control;
	
	public MediaSdpInfo() {
		super();
	}

	public MediaSdpInfo(String media, String codec, int rtpPayloadType,
			int clockRate, String control) {
		super();
		this.media = media;
		this.codec = codec;
		this.rtpPayloadType = rtpPayloadType;
		this.clockRate = clockRate;
		this.control = control;
	}

	public String getMedia() {
		return media;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	public String getCodec() {
		return codec;
	}

	public void setCodec(String codec) {
		this.codec = codec;
	}

	public int getRtpPayloadType() {
		return rtpPayloadType;
	}

	public void setRtpPayloadType(int rtpPayloadType) {
		this.rtpPayloadType = rtpPayloadType;
	}

	public int getClockRate() {
		return clockRate;
	}

	public void setClockRate(int clockRate) {
		this.clockRate = clockRate;
	}

	public String getControl() {
		return control;
	}

	public void setControl(String control) {
		this.control = control;
	}

	@Override
	public String toString() {
		return "MediaSdpInfo [media=" + media + ", codec=" + codec
				+ ", rtpPayloadType=" + rtpPayloadType + ", clockRate="
				+ clockRate + ", control=" + control + "]";
	}		
	
}
