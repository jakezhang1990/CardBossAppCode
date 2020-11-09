package com.xlabs.cardbossapp.thread;

import android.os.Looper;
import android.os.Message;

/**
 * loop 线程.run方法只做loop .
 *
 * Created by zhoushenghua on 18-6-20.
 */

public abstract class LoopNonUIThread<T> extends DefaultBaseNonUIThread<T> {

    @Override
    public void doRun() {
        Looper.loop();
    }

    @Override
    public void doHandlerMessage(Message handlerMessage) {

    }
}

