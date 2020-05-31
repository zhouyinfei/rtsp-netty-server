package com.zhuyun.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspHeaderValues;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspResponseStatuses;
import io.netty.handler.codec.rtsp.RtspVersions;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.jitsi.service.neomedia.RawPacket;

import com.zhuyun.RtspNettyServer;
import com.zhuyun.rtp.RtpUtils;
import com.zhuyun.session.SessionFactory;
import com.zhuyun.streamhub.StreamFrame;
import com.zhuyun.streamhub.StreamHub;
import com.zhuyun.utils.Command;
import com.zhuyun.utils.ReadFromFile;

public class RtspHandler extends HttpFlvHandler
{
    private int remoteVideoRtpPort = 0;
    private int remoteVideoRtcpPort = 0;
    private int remoteAudioRtpPort = 0;
    private int remoteAudioRtcpPort = 0;
    private String strremoteip;
    private String session;							
    private InetSocketAddress dstaddr = null;		//目的客户端地址
    private int isRtspAlive = 1;				//rtsp连接是否存在，如果不存在，则停止发送udp
    private RtpUtils rtpUtils;
    private int fps = 25;						//默认帧率
    private String media = "h264";						//默认媒体类型

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelInactive(ctx);
        System.out.println("close rtp rtcp channel "  + ctx.channel().id().asShortText());
        
//        if (null != rtpchn && rtpchn.isActive())
//        {
//            rtpchn.close();
//            rtcpchn.close();
//            System.out.println("close rtp rtcp channel "  + ctx.channel().id().asShortText());
//        }
        
        if (session != null) {
        	SessionFactory.removeSession(session);
		}
        
        isRtspAlive = 0;
    }
    
    private boolean checkUrl(FullHttpRequest r)
    {
        if (!StringUtil.isNullOrEmpty(keyhash))
        {
            return true;
        }

        QueryStringDecoder uri = new QueryStringDecoder(r.uri());

        if (!uri.path().endsWith("/live") || !uri.parameters().containsKey("keyhash"))
        {
            return false;
        }
        
        keyhash = uri.parameters().get("keyhash").get(0);
        //设备keyhash，必须
        if (StringUtil.isNullOrEmpty(keyhash))
        {
            return false;
        }
        
        //媒体类型，必须，值目前可选范围：264、aac
        if (uri.parameters().get("media") != null) {
        	media = uri.parameters().get("media").get(0);
		}
        if (StringUtil.isNullOrEmpty(media))
        {
            return false;
        }
        if (!"h264".equals(media) && !"aac".equals(media)) {
        	return false;
		}
        
        //帧率，非必须
        String fpsString = null;
        if (uri.parameters().get("fps") != null) {
        	fpsString = uri.parameters().get("fps").get(0);
		}
        if (fpsString != null && StringUtils.isNumeric(fpsString)) {
        	fps = Integer.parseInt(fpsString);
		}
        
        return true;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest r) throws Exception
    {
        if (!r.decoderResult().isSuccess())
        {
            System.out.println("decode error");
            closeThisClient();
            return;
        }
        if (false == checkUrl(r))
        {
            System.out.println("check url error");
            closeThisClient();
            return;
        }

        FullHttpResponse o = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
        if (r.method() == RtspMethods.OPTIONS)
        {
        	System.out.println("options");
            o.headers().add(RtspHeaderValues.PUBLIC, "DESCRIBE, SETUP, PLAY, TEARDOWN, ANNOUNCE, RECORD");
        } else if (r.method() == RtspMethods.DESCRIBE)
        {
            InetSocketAddress addr = (InetSocketAddress) ctx.channel().localAddress();
            
            String sdp = String.format("c=IN IP4 %s \nm=video 0 RTP/AVP 96\na=rtpmap:96 H264/90000\n", addr.getHostString());
            if ("h264".equals(media)) {
            	sdp = String.format("c=IN IP4 %s \nm=video 0 RTP/AVP 96\na=rtpmap:96 H264/90000\n", addr.getHostString());
//    			+ "a=ssrc:123456 cname:AyMDWB+q6ApWdpfU\n"
//    			+ "a=ssrc:123456 msid:stream_id video_label\n"
//    			+ "a=ssrc:123456 mslabel:stream_id\n"
//    			+ "a=ssrc:123456 label:video_label\n"
            } else if ("aac".equals(media)) {
            	sdp = String.format("c=IN IP4 %s \nm=audio 0 RTP/AVP 96\na=rtpmap:96 mpeg4-generic/16000\n"
            			+ "a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; config=140856e500; sizeLength=13; indexLength=3; indexDeltaLength=3; Profile=1;\n", addr.getHostString());
//    			+ "a=ssrc:123456 cname:AyMDWB+q6ApWdpfU\n"
//    			+ "a=ssrc:123456 msid:stream_id audio_label\n"
//    			+ "a=ssrc:123456 mslabel:stream_id\n"
//    			+ "a=ssrc:123456 label:audio_label\n"
			}
            
            o.headers().add(RtspHeaderNames.CONTENT_TYPE, "application/sdp");
            o.content().writeCharSequence(sdp, CharsetUtil.UTF_8);
            o.headers().add(RtspHeaderNames.CONTENT_LENGTH, o.content().writerIndex());

        } else if (r.method() == RtspMethods.SETUP)
        {
            System.out.println(r.headers().get(RtspHeaderNames.TRANSPORT));
            String transport = r.headers().get(RtspHeaderNames.TRANSPORT);
            transport = transport.toLowerCase();

            String[] strlist = transport.split(";");
            if (strlist.length > 0 && strlist[0].contains("rtp/avp"))
            {
                for(String i : strlist)
                {
                    if (i.startsWith("client_port"))
                    {
                        String[] strclientport = i.split("=|-");

                        remoteAudioRtpPort = Integer.parseInt(strclientport[1]);
                        remoteAudioRtcpPort = Integer.parseInt(strclientport[2]);
                        strremoteip = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();

                        if (null == dstaddr)
                        {
                            dstaddr = new InetSocketAddress(strremoteip, remoteAudioRtpPort);
                        }
                        break;
                    }
                }

                o.headers().add(RtspHeaderNames.TRANSPORT,
                        r.headers().get(RtspHeaderNames.TRANSPORT)+";ssrc=aaaaaaaa"+String.format(";server_port=%d-%d", RtspNettyServer.rtpPort, RtspNettyServer.rtpPort+1));
            } else{
                System.out.println("error transport exit");
                closeThisClient();
                return;
            }

            session = r.headers().get(RtspHeaderNames.SESSION);
            if (session == null)			//只有请求中没有session时才创建
            {
            	session = SessionFactory.createSession();
                while (SessionFactory.getSession(session) != null) {	//防止重复
                	session = SessionFactory.createSession();
    			}
                SessionFactory.putSession(session, keyhash);
                o.headers().add(RtspHeaderNames.SESSION, session);
            }
            
            System.out.println("setup over");

        } else if (r.method() == RtspMethods.PLAY)
        {
            // send rtp and rtcp to client
            System.out.println("play");
            
            //校验session是否存在
            session = r.headers().get(RtspHeaderNames.SESSION);
            keyhash = SessionFactory.getSession(session);
            if (keyhash == null) {						
            	System.out.println("rtspSession is null.");
            	o = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.BAD_REQUEST);
			} else {
				StreamHub.EnterStream(keyhash, this);
				
				//直播，边上传，边播放
//				while (true) {
//					byte[] take = UdpHandler.arrayBlockingQueue.take();
////					System.out.println("=============queue.length: " + UdpHandler.arrayBlockingQueue.size());
//					ByteBuf byteBuf = Unpooled.directBuffer();
//					byteBuf.writeBytes(take);
//					RtspNettyServer.rtpChannel.writeAndFlush(new DatagramPacket(byteBuf, this.dstaddr));
//				}
				
				if ("h264".equals(media)) {
					//播放h264视频文件
					String filename = RtspNettyServer.outputPath + keyhash + ".h264";
					File f = new File(filename);
			        if (!f.exists()) {
			        	o = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.BAD_REQUEST);
			        	sendAnswer(ctx, r, o);
			            throw new FileNotFoundException(filename);
			        }
			 
			        sendAnswer(ctx, r, o);
			        BufferedInputStream in = null;
			        try {
			            in = new BufferedInputStream(new FileInputStream(f));
			            int buf_size = 64*1024;
			            byte[] buffer = new byte[buf_size];		//从文件读的字节存入的地方
			            byte[] nalu;							//临时存储一个nalu单元内容
			            byte[] firstHalfNalu = null;			//nalu前半段
			            byte[] secondHalfNalu = null;			//nalu后半段
			            int len = 0;							//每次从文件读的字节数
			            int state = 0;							//状态机，值范围：0、1、2、3、4
			            int first = 1;							//是否是第一个起始码
			            int cross = 0;							//某个nalu是否跨buffer
			            int ssrc = RandomUtils.nextInt();		//rtp的ssrc
			            rtpUtils = new RtpUtils();
			            
			            while (-1 != (len = in.read(buffer, 0, buf_size))) {
			            	if (isRtspAlive == 0) {			//如果rtsp连接中断，则停止发送udp
								break;
							}
			            	
			            	int start = 0;							//第一个nalu的起始位置
			            	int offset = 0;							//当前循环中的偏移量
		            		while (offset <= len-4) {
		            			if (state == 0) {							//没有遗留状态
	            					if (buffer[offset] == 0x00 &&
	            							buffer[offset + 1] == 0x00 &&
	            							buffer[offset + 2] == 0x00 &&
	            							buffer[offset + 3] == 0x01) {
	            						if (cross == 1) {				//跨buffer
	            							if (first == 0) {			//不是第一个起始码
	            								secondHalfNalu = new byte[offset];		//拿到后半段内容
	            								System.arraycopy(buffer, start, secondHalfNalu, 0, secondHalfNalu.length);
	            								
	            								//拼接前半段与后半段内容, 拷贝到新的数组中
	            								int naluSize = firstHalfNalu.length + secondHalfNalu.length;
	            								nalu = new byte[naluSize];
	            								System.arraycopy(firstHalfNalu, 0, nalu, 0, firstHalfNalu.length);
	            								System.arraycopy(secondHalfNalu, 0, nalu, firstHalfNalu.length, secondHalfNalu.length);
	            								
	            								List<byte[]> rtpList = rtpUtils.naluToRtpPack(nalu, ssrc, fps);
	            								for (byte[] rptPackage : rtpList) {
	            									ByteBuf byteBuf = Unpooled.directBuffer();
	            									byteBuf.writeBytes(rptPackage);
	            									RtspNettyServer.rtpChannel.writeAndFlush(new DatagramPacket(byteBuf, this.dstaddr));
	            								}
	            							}
	            							
	            							offset += 4;
	            							state = 0;
	            							first = 0;	
	            							start = offset;					//当前位置变成新的起始位置
	            							cross = 0;						//跨buffer标志位重置成0
										} else {							//没有跨buffer
											if (first == 0) {			//不是第一个起始码
	            								int naluSize = offset - start;
	            								nalu = new byte[naluSize];
	            								System.arraycopy(buffer, start, nalu, 0, naluSize);
	            								
	            								List<byte[]> rtpList = rtpUtils.naluToRtpPack(nalu, ssrc, fps);
	            								for (byte[] rptPackage : rtpList) {
	            									ByteBuf byteBuf = Unpooled.directBuffer();
	            									byteBuf.writeBytes(rptPackage);
	            									RtspNettyServer.rtpChannel.writeAndFlush(new DatagramPacket(byteBuf, this.dstaddr));
	            								}
	            							}
	            							
	            							offset += 4;
	            							state = 0;
	            							first = 0;	
	            							start = offset;					//当前位置变成新的起始位置
										}
	            						
	            						
	            					} else {
	            						state = 0;
	            						offset ++;
	            					}
								} else if (state == 1) {
									if (buffer[offset] == 0x00 &&
	    								buffer[offset + 1] == 0x00 &&
	    								buffer[offset + 2] == 0x01) {
										
										//拿到两个起始码之间的一个nalu的数据
	    				            	int naluSize = firstHalfNalu.length - 1;
	    				            	nalu = new byte[naluSize];
	    				            	System.arraycopy(firstHalfNalu, 0, nalu, 0, naluSize);
	    								
	    								List<byte[]> rtpList = rtpUtils.naluToRtpPack(nalu, ssrc, fps);
	    								for (byte[] rptPackage : rtpList) {
	    									ByteBuf byteBuf = Unpooled.directBuffer();
	    									byteBuf.writeBytes(rptPackage);
	    									RtspNettyServer.rtpChannel.writeAndFlush(new DatagramPacket(byteBuf, this.dstaddr));
	    								}
										
										offset += 3;
	    								state = 0;
	    								start = offset;					//当前位置变成新的起始位置
	    							} else {
	    								state = 0;
	    								offset ++;
									}
								} else if (state == 2) {
									if (buffer[offset] == 0x00 &&
	    								buffer[offset + 1] == 0x01) {
	    								
										//拿到两个起始码之间的一个nalu的数据
	    				            	int naluSize = firstHalfNalu.length - 2;
	    				            	nalu = new byte[naluSize];
	    				            	System.arraycopy(firstHalfNalu, 0, nalu, 0, naluSize);
	    								
	    								List<byte[]> rtpList = rtpUtils.naluToRtpPack(nalu, ssrc, fps);
	    								for (byte[] rptPackage : rtpList) {
	    									ByteBuf byteBuf = Unpooled.directBuffer();
	    									byteBuf.writeBytes(rptPackage);
	    									RtspNettyServer.rtpChannel.writeAndFlush(new DatagramPacket(byteBuf, this.dstaddr));
	    								}
										
										offset += 2;
	    								state = 0;
	    								start = offset;					//当前位置变成新的起始位置
	    							} else {
	    								state = 0;
	    								offset ++;
									}
								} else if (state == 3) {
									if (buffer[offset] == 0x01) {
										//拿到两个起始码之间的一个nalu的数据
	    				            	int naluSize = firstHalfNalu.length - 3;
	    				            	nalu = new byte[naluSize];
	    				            	System.arraycopy(firstHalfNalu, 0, nalu, 0, naluSize);
	    								
	    								List<byte[]> rtpList = rtpUtils.naluToRtpPack(nalu, ssrc, fps);
	    								for (byte[] rptPackage : rtpList) {
	    									ByteBuf byteBuf = Unpooled.directBuffer();
	    									byteBuf.writeBytes(rptPackage);
	    									RtspNettyServer.rtpChannel.writeAndFlush(new DatagramPacket(byteBuf, this.dstaddr));
	    								}
	    								
										offset += 1;
	    								state = 0;
	    								start = offset;					//当前位置变成新的起始位置
	    							} else {
	    								state = 0;
	    								offset ++;
									}
								}
							}
			            	
		            		
			            	//指针指向最后3位时
			            	if (offset == len-3) {
			            		if (buffer[offset] == 0x00 && 
			            			buffer[offset + 1] == 0x00	&&
			            			buffer[offset + 2] == 0x00) {
									state = 3;
								} else if (buffer[offset + 1] == 0x00 &&
			            			buffer[offset + 2] == 0x00) {
									state = 2;
								} else if (buffer[offset + 2] == 0x00) {
									state = 1;
								}
			            		cross = 1;				//一定会跨buffer
			            		firstHalfNalu = new byte[offset + 3 - start];	//初始化前半段nalu数组，将前半段内容放进去
			            		System.arraycopy(buffer, start, firstHalfNalu, 0, firstHalfNalu.length);
							}
			            	
						}
			            
			        } catch (IOException e) {
			            e.printStackTrace();
			            throw e;
			        } finally {
			            try {
			                in.close();
			            } catch (IOException e) {
			                e.printStackTrace();
			            }
			        }
			        return;
				} else if ("aac".equals(media)) {
					//播放aac音频文件
					String filename = RtspNettyServer.outputPath + keyhash + ".aac";
					File f = new File(filename);
			        if (!f.exists()) {
			        	o = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.BAD_REQUEST);
			        	sendAnswer(ctx, r, o);
			            throw new FileNotFoundException(filename);
			        }
			        sendAnswer(ctx, r, o);
			        BufferedInputStream in = null;
					
			        
			        in = new BufferedInputStream(new FileInputStream(f));
			        int seqNum = 1;							//rtp的seqnum
		            int ssrc = RandomUtils.nextInt();		//rtp的ssrc
			        int len = 0;							//每次从文件读的实际字节数
			        int aacDataLen = 0;						//aac data长度
			        int sampling = 16000;					//采样率，默认值16000
					byte[] adtsHeaderBuffer = new byte[7];	//临时存储adts头
					byte[] aacDataBuffer;	//临时存储aac data
					rtpUtils = new RtpUtils();
					
					int isHeader = 1;						//标志位，表示当前读取的是adts头还是aac data
					len = in.read(adtsHeaderBuffer, 0, 7);				//刚开始读取adts头
					while (len != -1) {
						if (isRtspAlive == 0) {			//如果rtsp连接中断，则停止发送udp
							break;
						}
						
						if (isHeader == 1) {				//adts头，下一个就是aac data
							aacDataLen = ((adtsHeaderBuffer[3]&0x03)<<11)		//获取aac data长度
										+ (adtsHeaderBuffer[4]<<3)
										+ ((adtsHeaderBuffer[5]&0xE0)>>5) - 7;
							byte samp = (byte) (adtsHeaderBuffer[2]&0x3C);		//获取采样率
							sampling = RtpUtils.getSampling(samp);
							isHeader = 0;
						} else {							//aac data，下一个就是adts头
							aacDataBuffer = new byte[aacDataLen];				
							len = in.read(aacDataBuffer, 0, aacDataLen);		//读取aac data
							byte[] rptPackage = rtpUtils.aacToRtpPack(aacDataBuffer, seqNum, ssrc);
							ByteBuf byteBuf = Unpooled.directBuffer();
							byteBuf.writeBytes(rptPackage);
							RtspNettyServer.rtpChannel.writeAndFlush(new DatagramPacket(byteBuf, this.dstaddr));
							seqNum ++;

							Thread.sleep(1024*1000/sampling);		//延时发送帧
							len = in.read(adtsHeaderBuffer, 0, 7);
							isHeader = 1;
						}
			        }
					
					in.close();
					return;
				}
				
			}

        } else if (r.method() == RtspMethods.TEARDOWN)
        {
            System.out.println("teardown");
            
            //校验session是否存在
            session = r.headers().get(RtspHeaderNames.SESSION);
            keyhash = SessionFactory.getSession(session);
            if (keyhash == null) {						
            	System.out.println("rtspSession is null.");
            	o = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.BAD_REQUEST);
			} else {
	            o.headers().add("filename", keyhash + ".264");
				sendAnswer(ctx, r, o);
				closeThisClient();
				return;
			}
        }  else if (r.method() == RtspMethods.ANNOUNCE)
        {
            System.out.println("announce");
        } else if (r.method() == RtspMethods.RECORD)
        {
            System.out.println("record");
            
            //校验session是否存在
            session = r.headers().get(RtspHeaderNames.SESSION);
            keyhash = SessionFactory.getSession(session);
            if (keyhash == null) {						
            	System.out.println("rtspSession is null.");
            	o = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.BAD_REQUEST);
			} else {
				o.headers().add("Server", "RtspServer");
				
				sendAnswer(ctx, r, o);
				Executors.newSingleThreadExecutor().execute(new Runnable() {
					@Override
					public void run() {
						OutputStream outputStream = null;
						try {
							if ("h264".equals(media)) {
								outputStream=new BufferedOutputStream(new FileOutputStream("C:/Users/zhouyinfei/Desktop/" + keyhash + ".h264", true));
								RtpHandler.arrayBlockingQueue.clear();
								while (true) {
									byte[] take = RtpHandler.arrayBlockingQueue.take();
									RawPacket rawPacket = new RawPacket(take, 0, take.length);
									
									//h264码流
									byte[] b = RtpUtils.rtpToNaluPack(rawPacket);
									if (b != null && b.length != 0) {
										outputStream.write(b);
									}
								}
							} else if("aac".equals(media)) {
								outputStream=new BufferedOutputStream(new FileOutputStream("C:/Users/zhouyinfei/Desktop/" + keyhash + ".aac", true));
								RtpHandler.arrayBlockingQueue.clear();
								while (true) {
									byte[] take = RtpHandler.arrayBlockingQueue.take();
									RawPacket rawPacket = new RawPacket(take, 0, take.length);
									
									//AAC码流
									List<byte[]> adtsList = RtpUtils.rtpToAdtsPack(rawPacket);
									if (adtsList != null && adtsList.size() != 0) {
										for (byte[] b : adtsList) {
											outputStream.write(b);
										}
									}
									
								}
							}
						} catch (InterruptedException | IOException e) {
							e.printStackTrace();
						} finally {
							try {
								if (outputStream != null) {
									outputStream.close();
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				});;
				return;
			}
        } 
        else
        {
            System.out.println("unknown message");
            o.setStatus(RtspResponseStatuses.NOT_FOUND);
        }
        sendAnswer(ctx, r, o);
    }
    
    private int timestamp = 0;
    private int sequence = 0;
    private int rtpssrc = 0x13;

    private void writeRtpHeader(ByteBuf header, byte bmarker)
    {
        header.writeByte(0x80);
        header.writeByte(96 | (bmarker << 7) );
        header.writeShort(this.sequence++);
        header.writeInt(this.timestamp);
        header.writeInt(this.rtpssrc);
    }

    @Override
    public boolean writeFrame(StreamFrame frame)
    {
        final int MAX_RTP_PKT_LENGTH = 1400;					//最大RTP包长度
        int nretains = 0;
        if (this.chn.isActive() && null != this.dstaddr && RtspNettyServer.rtpChannel.isActive())
        {
            ByteBuf h24frame = frame.content();

            ByteBuf buf = h24frame.slice();
            int dwtotallen = buf.readableBytes();
            int dwindex = 0;
            byte bmarker = 0;

            while (buf.isReadable())
            {
                final int nalsize = buf.readInt();
                dwindex += 4;
                if (dwindex + nalsize == dwtotallen) { bmarker = 1; }

                if ((nalsize-1) <= MAX_RTP_PKT_LENGTH)
                {
                    // new datagrampacket header 12
                    // body
                    ByteBuf header = chn.alloc().directBuffer(12);
                    writeRtpHeader(header, bmarker);

                    ByteBuf data = h24frame.retainedSlice(dwindex, nalsize);
                    CompositeByteBuf combuf = chn.alloc().compositeBuffer(2)
                            .addComponent(true, header)
                            .addComponent(true, data);
//                    System.out.println(combuf.readableBytes());

                    RtspNettyServer.rtpChannel.write(new DatagramPacket(combuf, this.dstaddr));
                    nretains += 1;

                }
                else {
                    final byte bynaltype = buf.getByte(dwindex);
                    byte blastfu = 0;
                    int npacket = (nalsize - 1 + MAX_RTP_PKT_LENGTH - 1) / MAX_RTP_PKT_LENGTH;

                    for(int inxpack = 0; inxpack < npacket; inxpack++)
                    {
                        int npacketsize = MAX_RTP_PKT_LENGTH;
                        int packetinx = dwindex + 1 + inxpack * MAX_RTP_PKT_LENGTH;
                        if (inxpack == npacket - 1)
                        {
                            npacketsize = (nalsize - 1) - inxpack * MAX_RTP_PKT_LENGTH; // 2801 nallen
                            blastfu = 1;
                        }
                        ByteBuf header = chn.alloc().directBuffer(14);
                        writeRtpHeader(header, (byte)(bmarker & blastfu));

                        header.writeByte((bynaltype & 0xe0) | 28);
                        if (inxpack == 0)
                        {
                            header.writeByte((bynaltype & 0x1f) | 0x80);//S
                        }
                        // last pack
                        else if (inxpack == npacket - 1)
                        {
                            header.writeByte((bynaltype & 0x1f) | 0x40);//E
                        } else
                        {
                            header.writeByte(bynaltype & 0x1f);
                        }
                        ByteBuf data = h24frame.retainedSlice(packetinx, npacketsize);
                        CompositeByteBuf combuf = chn.alloc().compositeBuffer(2)
                                .addComponent(true, header)
                                .addComponent(true, data);

                        RtspNettyServer.rtpChannel.write(new DatagramPacket(combuf, this.dstaddr));

                        nretains += 1;
                    }
                }
                dwindex += nalsize;
                //except the type byte
                buf.skipBytes(nalsize);
            }
            RtspNettyServer.rtpChannel.flush();
            this.timestamp += 30*90;
        }
        return true;
    }

    private void sendAnswer(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse rep)
    {
        final String cseq = req.headers().get(RtspHeaderNames.CSEQ);
        if (cseq != null)
        {
            rep.headers().add(RtspHeaderNames.CSEQ, cseq);
        }
        final String session = req.headers().get(RtspHeaderNames.SESSION);
        if (session != null)
        {
            rep.headers().add(RtspHeaderNames.SESSION, session);
        }
        if (!HttpUtil.isKeepAlive(req))
        {
            ctx.writeAndFlush(rep).addListener(ChannelFutureListener.CLOSE);
        } else
        {
            rep.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(rep);
        }
    }
 
}