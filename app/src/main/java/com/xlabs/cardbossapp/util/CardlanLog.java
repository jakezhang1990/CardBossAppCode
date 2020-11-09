package com.xlabs.cardbossapp.util;

/*
 *  @Project：  CardDemoForForeign
 *  @packege：  com.cardlan.twoshowinonescreen.util
 *  @class:     CardlanLog
 *  @author:    cardlan
 *  @Creation-time:  2018/8/10 0010 pm 4:09
 *  @description：  To print the log
 */
public class CardlanLog {
    private static boolean printOnConsole = true;


    public CardlanLog() {
    }

    public static void debugOnConsole(Class<?> clazz, String msg) {
        if (printOnConsole) {
            System.out.println("[" + clazz.getSimpleName() + "] INFO : " + msg);
        }

    }

    public static void debugOnConsole(Class<?> clazz, Exception e) {
        if (printOnConsole) {
            e.printStackTrace();
        }

    }

}
