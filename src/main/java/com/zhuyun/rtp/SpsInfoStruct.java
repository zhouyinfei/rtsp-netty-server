package com.zhuyun.rtp;

public class SpsInfoStruct {
    int profile_idc;
    int level_idc;
    
    int width;
    int height;
    int fps;       //SPS中可能不包含FPS信息
    
	@Override
	public String toString() {
		return "SpsInfoStruct [profile_idc=" + profile_idc + ", level_idc="
				+ level_idc + ", width=" + width + ", height=" + height
				+ ", fps=" + fps + "]";
	}
    
}
