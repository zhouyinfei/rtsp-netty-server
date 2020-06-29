package com.zhuyun.rtcp;

/**
 * 视频发送端报告
 * @author zhouyinfei
 *
 */
public class VideoSRRtcp {
	private int mswNtpTimestamp;			//NTP时间戳
	private int lswNtpTimestamp;			//NTP时间戳
	private int rtpTimestamp;				//RTP时间戳
	private int senderPacketCount;			//从开始传输到此 SR 包产生时该发送者发送的 RTP 数据包总数
	private int senderOctetCount;			//从开始传输到此 SR 包产生时该发送者在 RTP 数据包发送的字节总数
	
	public VideoSRRtcp() {
		super();
	}

	public int getMswNtpTimestamp() {
		return mswNtpTimestamp;
	}

	public void setMswNtpTimestamp(int mswNtpTimestamp) {
		this.mswNtpTimestamp = mswNtpTimestamp;
	}

	public int getLswNtpTimestamp() {
		return lswNtpTimestamp;
	}

	public void setLswNtpTimestamp(int lswNtpTimestamp) {
		this.lswNtpTimestamp = lswNtpTimestamp;
	}

	public int getRtpTimestamp() {
		return rtpTimestamp;
	}

	public void setRtpTimestamp(int rtpTimestamp) {
		this.rtpTimestamp = rtpTimestamp;
	}

	public int getSenderPacketCount() {
		return senderPacketCount;
	}

	public void setSenderPacketCount(int senderPacketCount) {
		this.senderPacketCount = senderPacketCount;
	}

	public int getSenderOctetCount() {
		return senderOctetCount;
	}

	public void setSenderOctetCount(int senderOctetCount) {
		this.senderOctetCount = senderOctetCount;
	}

	@Override
	public String toString() {
		return "VideoSRRtcp [mswNtpTimestamp=" + mswNtpTimestamp
				+ ", lswNtpTimestamp=" + lswNtpTimestamp + ", rtpTimestamp="
				+ rtpTimestamp + ", senderPacketCount=" + senderPacketCount
				+ ", senderOctetCount=" + senderOctetCount + "]";
	}
	
}
