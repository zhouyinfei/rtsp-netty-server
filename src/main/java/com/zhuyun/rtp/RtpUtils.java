package com.zhuyun.rtp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jitsi.service.neomedia.RawPacket;


public class RtpUtils {
	private int intervel = 40;						//默认发送间隔40ms，帧率1000/40=25 
	private int seqNum = 0;							//h264 rtp的序列号，play的时候使用
	
	//rtp拆包成nalu
	public static byte[] rtpToNaluPack(RawPacket rtpPacket){
		//h264码流处理
		if (rtpPacket.getPayloadType() == 96) {												//以下处理仅针对H264码流
		    ByteBuffer bb = null;														//存放RTP解析后的NALU的数据
		    
		    byte[] rtpPayload = rtpPacket.getPayload();
		    byte fu_indicator = rtpPayload[0];
		    byte fu_header = rtpPayload[1];
		    byte nalu_type = (byte) (fu_indicator & 0x1f);		
		      
//		    System.out.println("=======nalu_type========" + nalu_type);
		    if (nalu_type == NaluType.FU_A.getType()) {  //FU-A										//分片封包模式
		          byte start_flag = (byte) (fu_header & 0x80);
		          byte end_flag = (byte) (fu_header & 0x40);
		          byte nalu_header = (byte) ((fu_indicator & 0xe0) | (fu_header & 0x1f));	//根据fu_indicator和fu_header来重构出nalu_header
		          if (start_flag != 0) {											//第一个分片
		        	  bb = ByteBuffer.allocate(rtpPayload.length + 3);
		        	  bb.put(new byte[]{0x0, 0x0, 0x0, 0x1});
		        	  bb.put(nalu_header);
		        	  byte[] dest = new byte[rtpPayload.length-2];
		        	  System.arraycopy(rtpPayload, 2, dest, 0, rtpPayload.length-2);
		        	  bb.put(dest);
		          } else if (end_flag != 0) {										//最后一个分片
		        	  bb = ByteBuffer.allocate(rtpPayload.length-2);
		              byte[] dest = new byte[rtpPayload.length-2];
		        	  System.arraycopy(rtpPayload, 2, dest, 0, rtpPayload.length-2);
		        	  bb.put(dest);
		          } else {															//中间分片
		        	  bb = ByteBuffer.allocate(rtpPayload.length-2);
		        	  byte[] dest = new byte[rtpPayload.length-2];
		        	  System.arraycopy(rtpPayload, 2, dest, 0, rtpPayload.length-2);
		        	  bb.put(dest);
		          }
		     } else if (nalu_type == NaluType.STAP_A.getType()) {  //STAP-A								//组合封包模式
		        int srcOffset = 1;										//第一个字节是STAP-A头，跳过
		        int bufferLen = 0;
		        //先计算需要的ByteBuffer长度，再将内容放进去
		        while ((rtpPayload.length - srcOffset) > 2)				//循环解析RTP，将组合后的NALU取出来，再加上起始码
		        {	
		            int size = 0;										//NALU的长度，2个字节
		            size |= rtpPayload[srcOffset] << 8;						
		            size |= rtpPayload[srcOffset + 1];
		
		            srcOffset += 2;										//将NALU header和NALU payload一起放进去，然后进入下一个循环
		        	bufferLen += (4+size);
		            srcOffset += size;
		        }
		        
		        srcOffset = 1;
		        bb = ByteBuffer.allocate(bufferLen);
		        while ((rtpPayload.length - srcOffset) > 2)				//循环解析RTP，将组合后的NALU取出来，再加上起始码
		        {	
		            int size = 0;										//NALU的长度，2个字节
		            size |= rtpPayload[srcOffset] << 8;						
		            size |= rtpPayload[srcOffset + 1];
		
		            srcOffset += 2;										//将NALU header和NALU payload一起放进去，然后进入下一个循环
		            byte[] dest = new byte[size];
		        	System.arraycopy(rtpPayload, srcOffset, dest, 0, size);
		        	
		        	bb.put(new byte[]{0x0, 0x0, 0x0, 0x1});				//NALU的起始码
		        	bb.put(dest);
		        	
		            srcOffset += size;
		        }
		        
		     } else if (nalu_type == NaluType.SINGLE.getType()) {											//单一NAL 单元模式
		          bb = ByteBuffer.allocate(rtpPayload.length + 4);					//将整个rtpPayload一起放进去
		    	  bb.put(new byte[]{0x0, 0x0, 0x0, 0x1});
		    	  bb.put(rtpPayload);
		     } else {
		    	 System.out.println("Unsupport nalu type!");
		     }
		    
		    if (bb != null) {
		    	return bb.array();
			}
		}
		return null;
	}
	
	//rtp拆包成ADTS列表
	public static List<byte[]> rtpToAdtsPack(RawPacket rtpPacket){
		//aac码流处理
		if (rtpPacket.getPayloadType() == 96) {												//以下处理仅针对H264码流
		    byte[] rtpPayload = rtpPacket.getPayload();
		    int aUHeadersLength = ((rtpPayload[0]&0xFF)<<16) + (rtpPayload[1]&0xFF);
		    int auCount = aUHeadersLength/16;					//AAC帧的数量
		    int index = 2 + auCount*2;							//上一次解析到的位置
		    List<byte[]> adtsList = new ArrayList<byte[]>();		//ADTS帧列表
		    
		    for (int i = 2; i < 2+2*auCount; i+=2) {			//遍历所有auHeader
		    	int aacDataLen = ((rtpPayload[i]&0xFF)<<5) + ((rtpPayload[i+1]&0xF8)>>3); 		//AAC的数据长度(不包括header)
		    	byte[] aacData = new byte[aacDataLen];											//AAC的数据
		    	System.arraycopy(rtpPayload, index, aacData, 0, aacDataLen);
		    	index += aacDataLen;
		    	byte[] adts = addAdtsHeader(aacData);
		    	adtsList.add(adts);
			}
		    
		    return adtsList;
		}
		return null;
	}
	
	//nalu封装成rtp
	public List<byte[]> naluToRtpPack(byte[] nalu, int ssrc, int fps){
		
		byte[] pcData = nalu;						//两个起始码(00 00 00 01)之间的NALU数据
		int mtu = 1400;								//最大传输单元
		int iLen = pcData.length;					//NALU总长度
		ByteBuffer bb = null;					
		List<byte[]> rtpList = new ArrayList<byte[]>();				//封装后的rtp包集合
		
		if (iLen > mtu) { //超过MTU						分片封包模式
	        byte start_flag = (byte) 0x80;
	        byte mid_flag = 0x00;
	        byte end_flag = 0x40;
	        
	        //获取帧头数据，1byte
	        byte nalu_type = (byte) (pcData[0] & 0x1f); //获取NALU的5bit 帧类型
	        byte nal_ref_idc = (byte) (pcData[0] & 0x60); //获取NALU的2bit 帧重要程度 00 可以丢 11不能丢
	        
	        //组装FU-A帧头数据 2byte
	        byte fu_identifier = (byte) (nal_ref_idc + 28);			//F为0 1bit,nri上面获取到2bit,28为FU-A分片类型5bit
	        byte fu_header = nalu_type;								//低5位和naluHeader相同
	        boolean bFirst = true;									//是否是第一个分片
	        boolean mark = false;									//是否是最后一个分片
	        int nOffset = 1;										//偏移量，跳过第一个字节naluHeader
	        while (!mark) {
	        	bb = ByteBuffer.allocate(mtu + 2);
	            if (iLen < nOffset + mtu) {           //是否拆分结束， 最后一个分片
	            	mtu = iLen - nOffset;
	            	
	                mark = true;
	                fu_header = (byte) (end_flag + nalu_type);		
	            } else {				
	                if (bFirst == true) {					//第一个分片
	                	fu_header = (byte) (start_flag + nalu_type);
	                    bFirst = false;
	                } else {								//或者中间分片
	                	fu_header = (byte) (mid_flag + nalu_type);
	                }
	            }
	            bb.put(fu_identifier);
	            bb.put(fu_header);
	            byte[] dest = new byte[mtu];
	            System.arraycopy(pcData, nOffset, dest, 0, mtu);
	            bb.put(dest);
	            nOffset += mtu;
	            byte[] rtpPackage = makeH264Rtp(bb.array(), mark, seqNum, (int)System.currentTimeMillis(), ssrc);
	            rtpList.add(rtpPackage);
	            seqNum ++;
	        }
	    } else {				//单一NAL 单元模式， 不使用组合模式。mark始终为false
	    	//从SPS中获取帧率
//	    	byte nalu_type = (byte) (pcData[0] & 0x1f); //获取NALU的5bit 帧类型
//	    	if (nalu_type == 7) {				//如果是sps
//				SpsInfoStruct info = new SpsInfoStruct();
//				SpsUtils.h264ParseSps(nalu, nalu.length, info);
//				if (info.fps != 0) {			//如果sps中存在帧率信息，则使用。如果没有，则使用默认帧率
//					intervel = 1000/info.fps;		
//				}
//			}
	    	
	    	//根据rtsp传过来的fps参数
	    	if (fps != 0) {
	    		intervel = 1000/fps;	
			}
	    	
	    	byte[] rtpPackage = makeH264Rtp(pcData, false, seqNum, (int)System.currentTimeMillis(), ssrc);
	    	rtpList.add(rtpPackage);
	    	seqNum ++;
	    }
		
		try {
			Thread.sleep(intervel);					//根据帧率延时发送
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return rtpList;
	}
	
	//aac data封装成rtp
	public byte[] aacToRtpPack(byte[] aacData, int seqNum, int ssrc){
		int aacLen = aacData.length;
		ByteBuffer bb = ByteBuffer.allocate(aacData.length + 4);
		bb.put((byte) 0x00);
		bb.put((byte) 0x10);
		bb.put((byte) ((aacLen>>5)&0xFF));			//取长度的高8位
		bb.put((byte) ((aacLen&0x1F)<<3));							//取长度的低5位
		bb.put(aacData);
		
		return makeAacRtp(bb.array(), true, seqNum, (int)System.currentTimeMillis(), ssrc);
	}
	
	public byte[] makeH264Rtp(byte[] pcData, boolean mark, int seqNum, int timestamp, int ssrc){
		byte b;
		if (mark) {
			b = (byte) 0xE0;	
		} else {
			b = (byte) 0x60;
		}
		
		ByteBuffer bb = ByteBuffer.allocate(pcData.length + 12);
		bb.put((byte) 0x80);				//V、P、X、CC， 1000 0000
		bb.put(b);							//mark 、payloadType(96)
		bb.putShort((short) seqNum);
		bb.putInt(timestamp);
		bb.putInt(ssrc);				
		bb.put(pcData);
		return bb.array();
	}
	
	public byte[] makeAacRtp(byte[] pcData, boolean mark, int seqNum, int timestamp, int ssrc){
		byte b;
		if (mark) {
			b = (byte) 0xE0;	
		} else {
			b = (byte) 0x60;
		}
		
		ByteBuffer bb = ByteBuffer.allocate(pcData.length + 12);
		bb.put((byte) 0x80);				//V、P、X、CC， 1000 0000
		bb.put(b);							//mark 、payloadType(96)
		bb.putShort((short) seqNum);
		bb.putInt(timestamp);
		bb.putInt(ssrc);				
		bb.put(pcData);
		return bb.array();
	}
	
	//构造ADTS帧，添加ADTS头
	public static byte[] addAdtsHeader(byte[] aacData){
		ByteBuffer bb = ByteBuffer.allocate(aacData.length + 7);
		bb.put((byte) 0xFF);		
		bb.put((byte) 0xF1);
		bb.put((byte) 0x60);
		
		short aacFrameLength = (short) ((aacData.length + 7)&0x1FFF);			//取低13位
		bb.put((byte) (((aacFrameLength>>11)&0x0F) + 0x40));
		bb.put((byte) ((aacFrameLength>>3)&0xFF));								//取低3位往后数8位
		bb.put((byte) (((aacFrameLength&0x07)<<5) + 0x02));																//取低3位作为结果的高3位，后5位固定：0 0010
		
		bb.put((byte) 0x40);
		bb.put(aacData);
		return bb.array();
	}
	
	//获取采样率
	public static int getSampling(byte samp){
		int sampling = 16000;
		switch (samp) {
		case 0x0:
			sampling = 96000;
			break;
		case 0x1:
			sampling = 88200;
			break;
		case 0x2:
			sampling = 64000;
			break;
		case 0x3:
			sampling = 48000;
			break;
		case 0x4:
			sampling = 44100;
			break;
		case 0x5:
			sampling = 32000;
			break;
		case 0x6:
			sampling = 24000;
			break;
		case 0x7:
			sampling = 22050;
			break;
		case 0x8:
			sampling = 16000;
			break;
		case 0x9:
			sampling = 12000;
			break;
		case 0xa:
			sampling = 11025;
			break;
		case 0xb:
			sampling = 8000;
			break;
		case 0xc:
			sampling = 7350;
			break;
		default:
			break;
		}
		return sampling;
	}

}
