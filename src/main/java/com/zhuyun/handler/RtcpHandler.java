package com.zhuyun.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

import net.sf.fmj.media.rtp.RTCPPacket;

import org.jitsi.impl.neomedia.rtcp.RTCPFBPacket;
import org.jitsi.service.neomedia.ByteArrayBuffer;
import org.jitsi.service.neomedia.RawPacket;
import org.jitsi.util.RTPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtcpHandler extends SimpleChannelInboundHandler<DatagramPacket>
{
	public static final Logger log = LoggerFactory.getLogger(RtcpHandler.class); 

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg)
    {
    	ByteBuf content = msg.content();
    	byte[] dst = new byte[content.readableBytes()];
    	content.readBytes(dst);

    	RawPacket rawPacket = new RawPacket(dst, 0, dst.length);
    	
    	try {
			if (!rawPacket.isInvalid()) {
				String rtcpSsrc = null;
				switch (rawPacket.getRTCPPacketType()) {
				case RTCPPacket.SR:						//发送端报告200
					rtcpSsrc = String.valueOf(rawPacket.getRTCPSSRC());
					break;
				case RTCPPacket.RR:						//接收端报告201
					rtcpSsrc = String.valueOf(RTCPFBPacket.getSourceSSRC(rawPacket));
					break;
				case RTCPFBPacket.RTPFB:				//RTPFB 205, FMT是1,NACK; FMT是15, transport-cc
					rtcpSsrc = String.valueOf(RTCPFBPacket.getSourceSSRC(rawPacket));
					int fmt = (rawPacket.getBuffer()[0] & 0x1F);
					if (fmt == 1) {			//NACK
						
					} else if(fmt == 15){				//transport-cc

					}
					break;
				case RTCPFBPacket.PSFB:					//PSFB 206, FMT是1, PLI; FMT是4, FIR; FMT是15, REMB
					rtcpSsrc = String.valueOf(RTCPFBPacket.getSourceSSRC(rawPacket));
					int fmt2 = (rawPacket.getBuffer()[0] & 0x1F);
					if (fmt2 == 1) {			//PLI
						
					}
					break;
				default:
					break;
				}
				RtspHandler rtspHandler = RtpHandler.rtspHandlerMap.get(rtcpSsrc);
				if (rtspHandler != null) {
					log.debug("put rtcpQueue length= {}" , rtspHandler.rtcpQueue.size());
					rtspHandler.rtcpQueue.offer(rawPacket);
				}
			} else {							//不是rtp包
				String destIp = msg.sender().getAddress().getHostAddress();
				int destPort = msg.sender().getPort();
				
				byte sign = dst[0];
				int ssrc = ((dst[1]&0xFF)<<24) + ((dst[2]&0xFF)<<16) + ((dst[3]&0xFF)<<8) + (dst[4]&0xFF);
				InetSocketAddress dstAddr = new InetSocketAddress(destIp, destPort);
				RtspHandler rtspHandler2 = RtpHandler.rtspHandlerMap.get(String.valueOf(ssrc));
				if (rtspHandler2 != null && destIp.equals(rtspHandler2.strremoteip)) {
					if (sign == 0x2) {		//视频RTCP探测
						rtspHandler2.dstVideoRtcpAddr = dstAddr;
						rtspHandler2.isVideoRtcpDetected = true;
					} if (sign == 0x3) {		//音频RTCP探测
						rtspHandler2.dstAudioRtcpAddr = dstAddr;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelActive(ctx);
        log.info("rtcp handler active {}", ctx.channel().id().asShortText());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelInactive(ctx);
        log.info("rtcp handler inactive {}",  ctx.channel().id().asShortText());
    }
    
    private static final char Hex_Char_Arr[] = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

    public static String byteArrToHex(byte[] btArr) {
        char strArr[] = new char[btArr.length * 2];
        int i = 0;
        for (byte bt : btArr) {
            strArr[i++] = Hex_Char_Arr[bt>>>4 & 0xf];
            strArr[i++] = Hex_Char_Arr[bt & 0xf];
        }
        return new String(strArr);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        log.error("{}", cause.getMessage());
        ctx.channel().close();
    }
}
