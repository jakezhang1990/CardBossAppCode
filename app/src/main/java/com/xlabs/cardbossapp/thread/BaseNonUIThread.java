package com.xlabs.cardbossapp.thread;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.xlabs.cardbossapp.util.CardlanLog;


/**
 * 非UI线程,不阻塞主线程.
 * 这里有一个bug,如果没有初始化 handler,那么其它基于这个类的子线程如果调用发送信息将会收不到.原因是因为一个looper,只能有一个handler.
 * 如果引用了别的线程调用的话,拿到的looper 并非引用线程的looper 而是当前线程的looper.
 * <p>
 * Created by zhoushenghua on 18-6-20.
 */

public abstract class BaseNonUIThread<T> extends Thread {

    private Looper  baseLooper;
    private Handler mBaseHandler;

    private boolean needInitHandler = true;

    @Override
    public void run() {
        Looper.prepare();
        synchronized (this) {
            baseLooper = Looper.myLooper();
            notifyAll();
        }
        if (needInitHandler) {
            getThreadHandler(this);
        }
        doRun();
        CardlanLog.debugOnConsole(this.getClass(), this.getName() + " 线程退出");
    }

    /**
     * 获取当前线程的 looper.
     *
     *
     * @return Looper
     */
    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }

        // If the thread has been started, wait until the looper has been created.
        synchronized (this) {
            while (isAlive() && baseLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return baseLooper;
    }

    /**
     * 获取当前线程的handler
     * @param baseNonUIThread
     * @return Handler
     */
    public Handler getThreadHandler(BaseNonUIThread<T> baseNonUIThread) {
        if (mBaseHandler == null) {
            mBaseHandler = new BaseHandler(baseNonUIThread.getLooper());
        }
        return mBaseHandler;
    }

    /**
     * 发送消息
     *
     * @param consumeEntity
     * @param baseNonUIThread
     * @param msgWhat
     */
    public void addMessage(T consumeEntity, BaseNonUIThread<T> baseNonUIThread, int msgWhat) {
        Message message = Message.obtain();
        message.what = msgWhat;
        message.obj = consumeEntity;
        baseNonUIThread.getThreadHandler(baseNonUIThread).sendMessage(message);
    }


    private class BaseHandler extends Handler {

        public BaseHandler(Looper looper) {

        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            doHandlerMsg(msg);
        }
    }

    /**
     * 退出
     * @return boolean
     */
    public boolean quit() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
            return true;
        }
        return false;
    }

    /***
     * 是否需要初始化handler
     * @param needInitHandler
     */
    public void setNeedInitHandler(boolean needInitHandler) {
        this.needInitHandler = needInitHandler;
    }

    /**
     * run 方法 要做的事情.子线程并不需要考虑looper 的事情,直接写入需要做的事情即可.
     */
    public abstract void doRun();

    /***
     * 处理 handler 的消息
     * @param msg
     */
    public abstract void doHandlerMsg(Message msg);


}

