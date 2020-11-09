package com.xlabs.cardbossapp.util;


import com.cardlan.utils.ByteUtil;

/**
 *
 * Created by cardlan on 18-7-19.
 *
 */

public abstract class AbstractBaseLogClass {

    /**
     *　Print log information of type string
     * @param logMsg
     */
    public void printLog(String logMsg) {
        CardlanLog.debugOnConsole(this.getClass(), "String message :"+logMsg);
    }

    /**
     * Print log information of type int
     * @param intLog
     */
    public void printLog(int intLog) {
        CardlanLog.debugOnConsole(this.getClass(), "int message: "+ String.valueOf(intLog));
    }

    /**
     * Print log information of type byte[]
     * @param logMsgBytes
     */
    public void printLog(byte[] logMsgBytes) {
        CardlanLog.debugOnConsole(this.getClass(), "byte array message:"+ ByteUtil
                .byteArrayToHexString(logMsgBytes));
    }

}
