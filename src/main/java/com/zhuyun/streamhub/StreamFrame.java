package com.zhuyun.streamhub;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

//将data存储在ByteBuf里
public class StreamFrame extends DefaultByteBufHolder
{
    public int dwTime;
    public boolean bIsKey;
    public int streamType;

    public StreamFrame(ByteBuf buf)
    {
        super(buf);
    }
}
