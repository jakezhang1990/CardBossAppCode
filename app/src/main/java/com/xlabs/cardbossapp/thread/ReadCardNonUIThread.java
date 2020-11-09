package com.xlabs.cardbossapp.thread;

/**
 * 读卡线程
 *
 * Created by zhoushenghua on 18-6-23.
 */

public class ReadCardNonUIThread extends NotDoHandlerMessageNonUIThread {

    private Runnable mTarget;

    public ReadCardNonUIThread(Runnable target) {
        this.mTarget = target;
    }

    @Override
    public void doRun() {
        this.mTarget.run();
    }

}
