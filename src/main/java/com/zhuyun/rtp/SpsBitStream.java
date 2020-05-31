package com.zhuyun.rtp;

import java.util.Arrays;

/**
 * SPS帧
 * @author zhouyinfei
 *
 */
public class SpsBitStream {
    byte[] data;   //sps数据
    int size;          //sps数据大小
    int index;         //当前计算位所在的位置标记
    
	@Override
	public String toString() {
		return "SpsBitStream [data=" + Arrays.toString(data) + ", size=" + size
				+ ", index=" + index + "]";
	}
    
}
