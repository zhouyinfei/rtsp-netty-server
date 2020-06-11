package com.zhuyun.rtp;

/**
 * H264的NALU type
 * @author zhouyinfei
 *
 */
public enum NaluType {
	FU_A((byte) 28),				//分片封包模式
	FU_B((byte) 29),
	STAP_A((byte) 24),				//组合封包模式
	STAP_B((byte) 25),			
	MTAP_16((byte) 26),			
	MTAP_24((byte) 27),		
	SINGLE((byte) 1);						//单一NAL 单元模式
	
	private byte type;
	
	private NaluType(byte type) {
		this.type = type;
	}

	public byte getType() {
		return type;
	}
}
