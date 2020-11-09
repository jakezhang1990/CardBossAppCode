package com.xlabs.cardbossapp.thread;

import android.os.Message;

/**
 * 不需要处理handler 消息的线程.只需要重写 doRun 方法.
 *
 * Created by zhoushenghua on 18-6-23.
 */

public class NotDoHandlerMessageNonUIThread extends DefaultBaseNonUIThread {

    @Override
    public void doHandlerMsg(Object handlerEntity) {

    }

    @Override
    public void doHandlerMessage(Message handlerMessage) {

    }

    @Override
    public void doRun() {

    }
}
