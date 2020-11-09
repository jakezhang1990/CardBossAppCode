package com.xlabs.cardbossapp.thread;

import android.os.Message;

/**
 * 默认的抽象线程,增加了默认消息{@link #SF_ADD_MSG}
 * Created by zhoushenghua on 18-6-20.
 */

public abstract class DefaultBaseNonUIThread<T> extends BaseNonUIThread<T> {

    private static final int SF_ADD_MSG = 101;

    /**
     * 发送默认消息,默认的消息是{@link #SF_ADD_MSG}
     *
     * @param consumeEntity
     * @param baseThread
     */
    public void addMessage(T consumeEntity, DefaultBaseNonUIThread<T> baseThread) {
        addMessage(consumeEntity, baseThread, SF_ADD_MSG);
    }

    /**
     * 发送消息
     * 不支持在多线程中调用,因为发送这个信息的是调用这个方法的线程,而不是本线程.
     *
     * @param consumeEntity
     * @param msgWhat
     */
    public void addMessage(T consumeEntity, int msgWhat) {
        addMessage(consumeEntity, this, msgWhat);
    }

    @Override
    public void doHandlerMsg(Message msg) {
        switch (msg.what) {
            case SF_ADD_MSG:
                //
                T handlerEntity = (T) msg.obj;
                if (handlerEntity != null) {
                    doHandlerMsg(handlerEntity);
                }
                break;
            default:
                doHandlerMessage(msg);
                break;
        }
    }

    /**
     * 处理 handler 中发来的消息,消息为泛型类型,消息不为null.
     * @param handlerEntity
     */
    public abstract void doHandlerMsg(T handlerEntity);

    /**
     * 处理 handler 中发来的消息,消息为泛型类型,消息不为null.
     * @param handlerMessage
     */
    public abstract void doHandlerMessage(Message handlerMessage);

}

