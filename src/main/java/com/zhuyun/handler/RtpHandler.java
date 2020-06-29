package com.zhuyun.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.jitsi.service.neomedia.RawPacket;

import com.zhuyun.RtspNettyServer;

public class RtpHandler extends SimpleChannelInboundHandler<DatagramPacket>
{
	//key是ssrc。同一个channel内，audio和video的ssrc不同，但是Queue是同一个
	public static Map<String, RtspHandler> rtspHandlerMap = 
						new ConcurrentHashMap<String, RtspHandler>(10000);
//		public static ArrayBlockingQueue<byte[]> arrayBlockingQueue = new ArrayBlockingQueue<byte[]>(500000);
	
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception
    {
    	ByteBuf content = msg.content();
    	byte[] dst = new byte[content.readableBytes()];
    	content.readBytes(dst);
    	
//    	byte[] b = new byte[11];
//    	System.arraycopy(dst, 0, b, 0, 11);
    	//如果udp的前面11个字节内容是"zhuyun-stun"，则返回port, 2个字节
//    	System.out.println("b=" + new String(b));
//    	if ("zhuyun-stun".equals(new String(b))) {
//    		String destIp = msg.sender().getAddress().getHostAddress();
//    		int destPort = msg.sender().getPort();
//    		System.out.println("destPort=" + destPort + ",destIp=" + destIp);
//    		
//    		InetSocketAddress dstAddr = new InetSocketAddress(destIp, destPort);;
//    		ByteBuf byteBuf = Unpooled.buffer(2);
//			byteBuf.writeShort(destPort);
//			RtspNettyServer.rtpChannel.writeAndFlush(new DatagramPacket(byteBuf, dstAddr));
//		} else {
//			arrayBlockingQueue.offer(dst);
			
			//ssrc校验
	    	RawPacket rawPacket = new RawPacket(dst, 0, dst.length);
	    	
	    	if (!rawPacket.isInvalid()) {
	    		RtspHandler rtspHandler = rtspHandlerMap.get(String.valueOf(rawPacket.getSSRC()));
	    		if (rtspHandler != null) {
		    		rtspHandler.rtpQueue.offer(dst);
				}
			} else {					//不是rtp包
	    		String destIp = msg.sender().getAddress().getHostAddress();
	    		int destPort = msg.sender().getPort();
	    		
	    		byte sign = dst[0];
	    		int ssrc = ((dst[1]&0xFF)<<24) + ((dst[2]&0xFF)<<16) + ((dst[3]&0xFF)<<8) + (dst[4]&0xFF);
	    		InetSocketAddress dstAddr = new InetSocketAddress(destIp, destPort);
	    		RtspHandler rtspHandler2 = rtspHandlerMap.get(String.valueOf(ssrc));
	    		if (rtspHandler2 != null && destIp.equals(rtspHandler2.strremoteip)) {
					if (sign == 0x0) {				//视频RTP探测
						rtspHandler2.dstVideoRtpAddr = dstAddr;
						rtspHandler2.isVideoRtpDetected = true;
					} else if (sign == 0x1) {		//音频RTP探测
						rtspHandler2.dstAudioRtpAddr = dstAddr;
					} else if (sign == 0x2) {		//视频RTCP探测
						rtspHandler2.dstVideoRtcpAddr = dstAddr;
						rtspHandler2.isVideoRtcpDetected = true;
					}
				}
	    		
			}
//		}
    	

    	
    	
    	

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelActive(ctx);
        System.out.println("rtp handler active " + ctx.channel().id().asShortText());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelInactive(ctx);
        System.out.println("rtp handler inactive "  + ctx.channel().id().asShortText());
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
        System.out.println("exception caught");
        cause.printStackTrace();
        ctx.channel().close();
    }
}
