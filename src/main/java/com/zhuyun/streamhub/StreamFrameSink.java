package com.zhuyun.streamhub;

public interface StreamFrameSink {
    boolean writeFrame(StreamFrame frame);
    void closeThisClient();
}
