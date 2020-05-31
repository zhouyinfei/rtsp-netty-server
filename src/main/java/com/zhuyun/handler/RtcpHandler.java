package com.zhuyun.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

public class RtcpHandler extends SimpleChannelInboundHandler<DatagramPacket>
{

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception
    {
//    	System.out.println("==========rtcp==========");
    	ByteBuf content = msg.content();
    	byte[] dst = new byte[content.readableBytes()];
    	content.readBytes(dst);
    	
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelActive(ctx);
        System.out.println("rtcp handler active " + ctx.channel().id().asShortText());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelInactive(ctx);
        System.out.println("rtcp handler inactive "  + ctx.channel().id().asShortText());
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
