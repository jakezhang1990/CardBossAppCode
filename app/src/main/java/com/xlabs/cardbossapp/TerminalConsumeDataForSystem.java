package com.xlabs.cardbossapp;

import android.app.Activity;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.cardlan.utils.ByteUtil;
import com.cardlan.utils.CalendarUtil;
import com.xlabs.cardbossapp.util.AbstractBaseLogClass;
import com.xlabs.cardbossapp.util.CardReadWriteUtil;
import com.xlabs.cardbossapp.util.CardlanLog;
import com.xlabs.cardbossapp.data.KeyConstant;
import com.xlabs.cardbossapp.data.KeyErrorException;
import com.xlabs.cardbossapp.secret.DESTwo;
import com.xlabs.cardbossapp.secret.ICardDataListener;
import com.xlabs.cardbossapp.thread.ReadCardNonUIThread;
import com.xlabs.cardbossapp.util.CpuCardSecretKeyHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.security.InvalidParameterException;
import java.util.Arrays;

/**
 * Created by cardlan on 18-6-4.
 * Terminal consumption data this class supports
 * reading and writing of unauthorized CARDS and authorization CARDS
 *
 */

public class TerminalConsumeDataForSystem extends AbstractBaseLogClass {

    //Consumption amount, unit cent
    private int mConsumeFee;

    //Card read threads
    private Thread mReadThread;

//    private boolean mNeedStartReadThreadWhenSetConsumeFee = true;

    public int getmConsumeFee() {
        return mConsumeFee;
    }

    public void setmConsumeFee(int mConsumeFee) {
        this.mConsumeFee = mConsumeFee;
    }

    private boolean mConsumeFeeIsLegal() {
        return mConsumeFee > 0;
    }

    private String mReadOrWriteKeyHexStr = null;

    public String getmReadOrWriteKeyHexStr() {
        return mReadOrWriteKeyHexStr;
    }

    /**
     * Total number of sectors read, default 4
     */
    private int mSectorReadNumber = 4;
    /**
     * Sector reads index, default 0
     */
    private int mSectorReadIndex = 1;

    private CardReadWriteUtil mCardUtil = new CardReadWriteUtil();

    //Is it an authorization card
    private boolean isAuthCard;


    /**
     * Card data list
     * key ：  sector-index（ sector + index）
     * value：byte array
     */
//    private HashMap<String, byte[]> mCardBytes = new HashMap<>();

    /**
     * This method will start a thread to read the card information continuously.
     * This method will determine whether the thread exists.
     * If it exists, the thread will not be recreated.
     * So you can stop the thread by {@link #setmConsumeFee};
     *
     * @param iCardDataListener Card information callback interface，
     */
    public void startRead(final ICardDataListener iCardDataListener) {
        mReadThread = null;
        if (mReadThread != null) {
            if (mReadThread.isAlive()) {
                return;
            }
        } else {
            mReadThread = new ReadCardNonUIThread(new Runnable() {
                @Override
                public void run() {
                    while (mConsumeFeeIsLegal()) {
//                        long threadID = Thread.currentThread().getId();
//                        CardlanLog.debugOnConsole(this.getClass(),"thread id :" + threadID);
                        mReadOrWriteKeyHexStr = null;
                        isAuthCard = false;
                        mCardUtil.initDev();
                        byte[] resetBytes = null;
                        if (iCardDataListener != null) {
                            resetBytes = mCardUtil.getCardResetBytes();
                            if (!ByteUtil.notNull(resetBytes)) {
                                CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class,
                                                          "Did not find the card");
                                continue;
                            }
                            try {
                                mReadOrWriteKeyHexStr = ByteUtil.byteArrayToHexString
                                        (calculateNormalCardKey(resetBytes));
                            } catch (KeyErrorException e) {
                                e.printStackTrace();
                            }
                            iCardDataListener.cardResetMsg(resetBytes);
                        }
//                        mCardBytes.clear();
                        for (int i = mSectorReadIndex; i <= mSectorReadNumber; i++) {
                            //Each sector has four modules, but the last one holds the check code,
                            // so no reading is required
                            if (i == 2 || i == 3) {
                                continue;
                            }
                            byte sector = ByteUtil.intToByteTwo(i);
                            byte[] readTemp = null;
                            int j = 0;
                            for (; j < 3; j++) {
                                if (i == 0 && (j == 1 || j == 2)) {
                                    continue;
                                }
                                byte index = ByteUtil.intToByteTwo(j);
                                if (i > 0) {
                                    //Starting from the first sector,
                                    // the subsequent reads and writes need to be written using the computed read key
                                    readTemp = mCardUtil.callReadJNI(ByteUtil.byteToHex(sector),
                                            ByteUtil.byteToHex(index), mReadOrWriteKeyHexStr, null);
                                    if (!ByteUtil.notNull(readTemp)) {
//
                                    }
                                } else {

                                    readTemp = mCardUtil.callReadJNI(ByteUtil.byteToHex(sector),
                                            ByteUtil.byteToHex(index), null, null);
                                }
                                //If the data is not read,
                                // you do not need to continue reading because there are no CARDS.
                                if (i == 0 && j == 0) {
                                    if (!ByteUtil.notNull(readTemp)) {
                                        //Determine if it is an authorization card
                                        isAuthCard(resetBytes, iCardDataListener);
                                        break;
                                    } else {
                                        //Ordinary card, calculate the key of ordinary card
                                        byte[] sn = resetBytes;
                                        try {
                                            mReadOrWriteKeyHexStr = ByteUtil.byteArrayToHexString
                                                    (calculateNormalCardKey(sn));
                                        } catch (KeyErrorException e) {
                                            e.printStackTrace();
                                            CardlanLog.debugOnConsole
                                                    (TerminalConsumeDataForSystem.class, e);
                                        }
                                        if (iCardDataListener != null) {
                                            iCardDataListener.readMsg(i, j, readTemp, false);
                                        }
//                                        continue;
                                    }
                                } else {
                                    if (isAuthCard) {
                                        break;
                                    }
                                    if (iCardDataListener != null) {
                                        if (i == mSectorReadNumber && j == 2) {
                                            try {

                                            } catch (Exception e) {

                                                CardlanLog.debugOnConsole
                                                        (TerminalConsumeDataForSystem.class,
                                                                "Read the card to complete!");
                                                CardlanLog.debugOnConsole
                                                        (TerminalConsumeDataForSystem.class,
                                                                ByteUtil.byteArrayToHex(readTemp));

                                                CardlanLog.debugOnConsole
                                                        (TerminalConsumeDataForSystem.class,
                                                                "===============");
                                            }
                                            mCardUtil.setmHasGetResetBytes(false);
                                            iCardDataListener.readMsg(i, j, readTemp, true);
                                        } else {
                                            iCardDataListener.readMsg(i, j, readTemp, false);
                                        }
                                    }
//                                    continue;
                                }
                            }

                            if (isAuthCard) {
                                break;
                            }
                            if (i == 0 && j == 0) {
                                //If the data is not read,
                                // you do not need to continue reading because there are no CARDS
                                if (!ByteUtil.notNull(readTemp)) {
                                    break;
                                }
                            }

                        }

                    }
                    CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class, "Exit the card reader thread!");
                }
            });
            mReadThread.start();
        }
    }

    /**
     * Whether the write card process was written successfully
     *
     * @param sector    sector index of hex string
     * @param index     block index of hex string
     * @param hexString hex string
     * @param keys      hex string
     * @param keyArea   hex string
     * @return boolean if write success, it return true, else return false
     */
    public boolean writeData(String sector, String index, String hexString, String keys, String
            keyArea) {
        int writeStatus = mCardUtil.callWriteJNI(sector, index, hexString, keys, keyArea);
        CardlanLog.debugOnConsole(this.getClass(), "writeStatus : " + writeStatus);
        return writeStatus == 5;
    }


    /**
     * Sets the total number of sectors to read, The default is 7.
     *
     * @param mSectorReadNumber
     */
    public void setmSectorReadNumber(int mSectorReadNumber) {
        this.mSectorReadNumber = mSectorReadNumber;
    }

    /**
     * Sets the index to start reading sectors, with 0 by default
     *
     * @param mSectorReadIndex
     */
    public void setmSectorReadIndex(int mSectorReadIndex) {
        this.mSectorReadIndex = mSectorReadIndex;
    }

    //Buzzer parameters

    private static final String procpath = "/proc/gpio_set/rp_gpio_set";
    private static final String open_bee_voice = "c_24_1_1";
    private static final String close_bee_voice = "c_24_1_0";

    private String writeProc(String path, byte[] buffer) {
        try {
            File file = new File(path);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "write error!";
        }
        return (buffer.toString());
    }

    /**
     * Corresponding buzzer
     */
    public void callProc() {
        writeProc(procpath, open_bee_voice.getBytes());
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        writeProc(procpath, close_bee_voice.getBytes());
    }


    public void hideSoftKeyboard(EditText mEditxt, Activity mAct) {
        InputMethodManager imm = (InputMethodManager) mAct.getSystemService(Context
                .INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mEditxt.getWindowToken(), 0);
        }
    }

    public void showSoftKeyboard(EditText mEditxt, Activity mAct) {
        InputMethodManager imm = (InputMethodManager) mAct.getSystemService(Context
                .INPUT_METHOD_SERVICE);
        if (imm != null) {
            mEditxt.requestFocus();
            imm.showSoftInput(mEditxt, 0);
        }
    }

    //The key related
    private byte[] assembleBytes = new byte[]{0x26, (byte) 0x91, 0x13, 0x00};
    //The system authorization card is relevant 0x82,0x26,0x00,0x36,0x82,0x42,0x27,0x79
    private byte[] desKeyBytes = new byte[]{(byte) 0x82, 0x26, 0x00, 0x36, (byte) 0x82, 0x42,
            0x27, 0x79};
    private byte[] checkBytes = new byte[]{0x55, (byte) 0xa0, (byte) 0xa1, (byte) 0xa2};


    /**
     * The key calculation method of the system authorization card is inconsistent,
     * so it is not suitable for the system authorization card
     * @param cardSNBytes
     * @return the result of NormalCardReadKey.
     * @throws KeyErrorException
     */
    public byte[] calculateNormalCardKey(byte[] cardSNBytes) throws KeyErrorException {
        if (ByteUtil.notNull(cardSNBytes)) {
            byte[] srcBytes = null;
            if (cardSNBytes.length == 4) {
                srcBytes = cardSNBytes;
            } else if (cardSNBytes.length > 4) {
                srcBytes = ByteUtil.copyBytes(cardSNBytes, 0, 4);
            } else {
                InvalidParameterException ipe = new InvalidParameterException
                        (TerminalConsumeDataForSystem
                                .class.getSimpleName() +
                                "Argument is invalid!");
                CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class, ipe);
                throw ipe;
            }
            srcBytes = ByteUtil.addBytes(srcBytes, assembleBytes);
            byte[] desKey = DESTwo.encrypt(srcBytes, KeyConstant.mAuthCardKey);

            if (ByteUtil.notNull(desKey) && desKey.length >= 6) {

                return ByteUtil.copyBytes(desKey, 0, 6);
            } else {
                throw new KeyErrorException();
            }
        } else {
            InvalidParameterException ipe = new InvalidParameterException
                    (TerminalConsumeDataForSystem
                            .class.getSimpleName() +
                            "Argument is invalid!");
            CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class, ipe);
            throw ipe;
        }
    }

    /**
     * calculate to the key to read the authorization card,
     * which is used to read the contents of the system authorization card
     * @param cardSNBytes sn of card to array;
     * @return the result of AuthCardReadKey.
     * @throws KeyErrorException
     */
    private byte[] calculateAuthCardReadKey(byte[] cardSNBytes) throws KeyErrorException {
        if (ByteUtil.notNull(cardSNBytes)) {
            byte[] srcBytes = null;
            if (cardSNBytes.length == 4) {
                srcBytes = cardSNBytes;
            } else if (cardSNBytes.length > 4) {
                srcBytes = ByteUtil.copyBytes(cardSNBytes, 0, 4);
            } else {
                InvalidParameterException ipe = new InvalidParameterException
                        (TerminalConsumeDataForSystem
                                .class.getSimpleName() +
                                "Argument is invalid!");
                CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class, ipe);
                throw ipe;
            }
            srcBytes = ByteUtil.addBytes(srcBytes, assembleBytes);
//            byte[] desKey = DESTwo.encrypt(desKeyBytes, srcBytes);
            byte[] desKey = DESTwo.encrypt(srcBytes, desKeyBytes);
            if (ByteUtil.notNull(desKey) && desKey.length >= 6) {
                CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class, ByteUtil
                        .byteArrayToHex(desKey));
                return ByteUtil.copyBytes(desKey, 0, 6);
            } else {
                throw new KeyErrorException();
            }
        } else {
            InvalidParameterException ipe = new InvalidParameterException
                    (TerminalConsumeDataForSystem
                            .class.getSimpleName() +
                            "Argument is invalid!");
            CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class, ipe);
            throw ipe;
        }
    }

    /**
     * Read the key to the authorization card,
     * which is used to decrypt the key for ordinary CARDS.
     * @param sectorOneIndexZerobytes The first sector 0 blocks of bytes
     * @return
     */
    private byte[] readAuthCardKey(byte[] sectorOneIndexZerobytes) {
        byte[] returnBytes = null;
        if (ByteUtil.notNull(sectorOneIndexZerobytes)) {
            if (sectorOneIndexZerobytes.length >= (checkBytes.length + 8)) {
                byte[] checkSrc = ByteUtil.copyBytes(sectorOneIndexZerobytes, 0, 4);
                CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class, "checkSrc=" + ByteUtil
                        .byteArrayToHex(checkSrc));
                CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class, "checkBytes=" +
                        ByteUtil
                                .byteArrayToHex(checkBytes));
                if (Arrays.equals(checkSrc, checkBytes)) {
                    returnBytes = ByteUtil.copyBytes(sectorOneIndexZerobytes, 4, 8);
                    KeyConstant.mAuthCardKey = returnBytes;
                    CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class, "KeyConstant" +
                            ".mAuthCardKey=" +
                            ByteUtil.byteArrayToHex(KeyConstant.mAuthCardKey));
                    isAuthCard = true;
                }
            }
        }
        return returnBytes;
    }

    /**
     * Reads the bytes of the first sector, the 0th field
     * @param readKeys
     * @return byte[]
     */
    private byte[] readSectorIndexZeroBytes(byte[] readKeys) {
        char readSector = 1;
        char readindex = 0;
        char bb = 0x0b;
        byte[] returnBytes = mCardUtil.callReadJNI(readSector,
                readindex, readKeys, bb);
        return returnBytes;
    }

    private boolean isAuthCard(byte[] cardSNBytes, final ICardDataListener iCardDataListener) {
        try {
            CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class, "the card sn is :" +
                    ByteUtil.byteArrayToHex(cardSNBytes));
            byte[] readKeys = calculateAuthCardReadKey(cardSNBytes);
            CardlanLog.debugOnConsole(TerminalConsumeDataForSystem.class, "the auth card " +
                    "readkeys is :" + ByteUtil.byteArrayToHex(readKeys));
            byte[] sectorOneIndexZerobytes = readSectorIndexZeroBytes(readKeys);
            readAuthCardKey(sectorOneIndexZerobytes);
            if (isAuthCard) {
                if (iCardDataListener != null) {
                    iCardDataListener.readMsg(1, 0, null, true);
                }
            }
        } catch (KeyErrorException e) {
            e.printStackTrace();
        }
        return isAuthCard;
    }


    public boolean readThreadIsAlive() {
        if (mReadThread != null) {
            return mReadThread.isAlive() && !mReadThread.isInterrupted();
        }
        return false;
    }


    //===================================Cpu卡===========================================

    private byte[] binNameArray = new byte[]{0x3f, 0x01};
    private byte[] selfileDffciArray = new byte[]{0x00, (byte) 0xa4, 0x00, 0x00};
    private String ppse = "2PAY.SYS.DDF01";
    private String getBalance = "GET BALANCE";
    private byte[] receiveBytes = null;
    private int readSize = 128;
    private Thread mReadCpuThread;
    private boolean mReadFlag = true;

    public void startReadCpuCardThread() {
        mReadCpuThread = new ReadCardNonUIThread(new Runnable() {
            @Override
            public void run() {
                while (mReadFlag) {
                    receiveBytes = new byte[readSize];
                    mCardUtil.initDev();
                    byte binlenth = ByteUtil.intToByteTwo(binNameArray.length);
                    byte[] tempBytes = ByteUtil.addBytes(selfileDffciArray, binlenth);
                    byte[] sendCmd = ByteUtil.addBytes(tempBytes, binNameArray);
                    printLog("sendCmd byte array is " + ByteUtil
                            .byteArrayToHexArray(sendCmd));
                    byte[] resetBytes = mCardUtil.getCardResetBytes();
                    if (!ByteUtil.notNull(resetBytes)) {
                        CardlanLog.debugOnConsole(this.getClass(), "while continue");
//                        continue;
                    } else {
                        CardlanLog.debugOnConsole(this.getClass(), "resetBytes : " + ByteUtil
                                .byteArrayToHexString(resetBytes));
                    }

                    int binresult = mCardUtil.callSendCpuCmd(sendCmd, receiveBytes);
                    CardlanLog.debugOnConsole(this.getClass(), "binresult : " + binresult);
                    CardlanLog.debugOnConsole(this.getClass(), ByteUtil.byteArrayToHex
                            (receiveBytes));
                    CardlanLog.debugOnConsole(this.getClass(), "==============================");

                    byte[] receivePpseBytes = new byte[readSize];
                    byte ppselenth = ByteUtil.intToByteTwo(ppse.length());
                    byte[] tempppseBytes = ByteUtil.addBytes(selfileDffciArray, ppselenth);
                    byte[] sendCmdppse = ByteUtil.addBytes(tempppseBytes, ppse.getBytes());
                    CardlanLog.debugOnConsole(this.getClass(), "sendCmdppse byte array is " +
                            ByteUtil
                                    .byteArrayToHexArray(sendCmdppse));
                    CardlanLog.debugOnConsole(this.getClass(), "==============================");
                    int ppseresult = mCardUtil.callSendCpuCmd(sendCmdppse, receivePpseBytes);
                    CardlanLog.debugOnConsole(this.getClass(), "ppseresult : " + ppseresult);
                    CardlanLog.debugOnConsole(this.getClass(), ByteUtil.byteArrayToHex
                            (receivePpseBytes));
                    CardlanLog.debugOnConsole(this.getClass(), "==============================");

                    //Get the balance
                    byte[] receiveBalanceBytes = new byte[readSize];
                    int balanceResult = mCardUtil.callSendCpuCmd(getBalance.getBytes(),
                            receivePpseBytes);
                    CardlanLog.debugOnConsole(this.getClass(), ByteUtil.byteArrayToHex
                            (receiveBalanceBytes));
                    CardlanLog.debugOnConsole(this.getClass(), "================balanceResult" +
                            "(" + balanceResult + ")==============");

                    try {
                        Thread.sleep(5 * 1000l);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                CardlanLog.debugOnConsole(this.getClass(), " exit read cpu card");
            }
        });
//
        mReadCpuThread.start();

    }

    public void stopRead() {
        setmConsumeFee(0);

        mReadFlag = false;

        CardlanLog.debugOnConsole(this.getClass(), " stop read card");
    }

    public byte[] sendCpuCmd(byte[] sendCmd, boolean needCheckStatus) {
        if (!ByteUtil.notNull(sendCmd)) {
            CardlanLog.debugOnConsole(this.getClass(), "cpu cmd is null");
            return null;
        }
        CardlanLog.debugOnConsole(this.getClass(), "sendCmd:"+ ByteUtil.byteArrayToHexString
                (sendCmd));
        byte[] receiveResultBytes = new byte[readSize];
        int result = mCardUtil.callSendCpuCmd(sendCmd, receiveResultBytes);
        CardlanLog.debugOnConsole(this.getClass(), "send cpu cmd result status is " + result);

        byte[] returnBytes = checkCpuTradeSuccess(receiveResultBytes,needCheckStatus);

        if (!ByteUtil.notNull(returnBytes)) {
            CardlanLog.debugOnConsole(this.getClass(), "send cpu cmd result byte array is " +
                    0);
            return null;
        }
        return returnBytes;
    }

    /**
     * Verify that the CPU card transaction is successful,
     * and determine if the data at the end of the data is 9000.
     * @param receiveResultBytes Operate on the CPU card return results
     * @return boolean return real value, if check success
     */
    private byte[] checkCpuTradeSuccess(byte[] receiveResultBytes, boolean needCheckStatus) {
        byte[] returnBytes = null;
        if (!ByteUtil.notNull(receiveResultBytes)) {
            return null;
        }
        returnBytes = ByteUtil.removeNULLByte(receiveResultBytes);
        CardlanLog.debugOnConsole(this.getClass(), "send cpu cmd return bytes is " +
                ByteUtil.byteArrayToHexArray(returnBytes));
        if (!ByteUtil.notNull(returnBytes)) {
            return null;
        }

        byte lastByte = returnBytes[returnBytes.length - 1];
        byte compareByte = (byte) 0x90;
        if (needCheckStatus) {
            if (lastByte != compareByte) {
                return null;
            }
        }
        return returnBytes;
    }


    public byte[] getCardResetBytes() {
        return mCardUtil.getCardResetBytes();
    }

    //File selection
    private String cmd_file_select = "00A40000023F0100";
    //Initialize the consumption header
    private String cmd_init_consume = "805001020B";
    //Initialize the consumer key index
    private String cmd_init_consume_key_index = "01";
    //Initial consumption transaction amount (cent)
    private String cmd_init_consume_fee = "00000001";
    //Initialize the consumer transaction terminal number
    private String cmd_init_consume_terminal_no = "100000000321";
    //Initializes the consumption terminator
    private String cmd_init_consume_end = "0F";
    //debit　The command header
    private String cmd_debit = "805401000F";
    //Command the tail
    private String cmd_consume_end = "08";

    private byte[] fileBytes = null;
    //count of consumption
    private int consumeCount = 8;

    private CpuCardSecretKeyHelper mCpuCardSecretKeyHelper = new CpuCardSecretKeyHelper();

    //Initializes the resulting array of consumption
    private byte[] initCpuConsumeBytes;

    /**
     * Gets the key for the consumption parameter.
     * @return byte[]
     */
    public byte[] getConsumeInitKey() {
        byte[] returnBytes = null;

        return returnBytes;
    }

    /**
     * Select the file
     */
    public byte[] selectCpuFile() {

        fileBytes = sendCpuCmd(ByteUtil.hexStringToByteArray(cmd_file_select), true);
        CardlanLog.debugOnConsole(this.getClass(), "fileBytes :" + ByteUtil.byteArrayToHexArray(fileBytes));
        return fileBytes;
    }

    /**
     * Initialize the consumption command of CPU card.
     *
     * @return
     */
    public byte[] sendInitCpuConsumeCmd(int consumeFee) {
        selectCpuFile();
        byte[] returnBytes = null;
        StringBuilder cmdSb = new StringBuilder();
        cmdSb.append(cmd_init_consume);
        cmdSb.append(cmd_init_consume_key_index);
        if (consumeFee <= 0) {
            cmdSb.append(cmd_init_consume_fee);
        } else {
            cmdSb.append(ByteUtil.intToHexString(consumeFee));
        }
        cmdSb.append(cmd_init_consume_terminal_no);
        cmdSb.append(cmd_init_consume_end);
        printLog("init Command is:" + cmdSb.toString());
        returnBytes = sendCpuCmd(ByteUtil.hexStringToByteArray(cmdSb.toString()), true);
        printLog("init The result of the command is:"+ ByteUtil.byteArrayToHexArray(returnBytes));
        return returnBytes;
    }

    /**
     * The second des calculation,
     * associated with the first result, is based on the initialization consumption.
     *
     * @return byte[] it not null ,it work success
     */
    public byte[] calculateSecondDes(int mConsumeFee) {
        byte[] returnBytes = null;
        byte[] mkeys = calculateFirstDes();
        if (!ByteUtil.notNull(mkeys)) {
            return returnBytes;
        }
        initCpuConsumeBytes = sendInitCpuConsumeCmd(mConsumeFee);
        if (!ByteUtil.notNull(initCpuConsumeBytes)) {
            printLog("init failure:");
            return returnBytes;
        }
        printLog("keys:" + ByteUtil.byteArrayToHexArray(mkeys));
        printLog("initCpuConsumeBytes : "+ ByteUtil.byteArrayToHexArray(initCpuConsumeBytes));
        byte[] sendCmdBytes = null;
        sendCmdBytes = ByteUtil.copyBytes(initCpuConsumeBytes, 11, 4);
        sendCmdBytes = ByteUtil.addBytes(sendCmdBytes, ByteUtil.copyBytes(initCpuConsumeBytes, 4,
                2));
        byte[] consumeConuts = ByteUtil.intToByteArray(consumeCount);
        sendCmdBytes = ByteUtil.addBytes(sendCmdBytes, ByteUtil.copyBytes(consumeConuts, 2, 2));
        printLog(sendCmdBytes);
        if (!ByteUtil.notNull(sendCmdBytes)) {
            return returnBytes;
        }
        printLog("sendCmdBytes:"+ ByteUtil.byteArrayToHexArray(sendCmdBytes));
        returnBytes = mCpuCardSecretKeyHelper.callRunDes(sendCmdBytes, mkeys);
        printLog(returnBytes);
        return returnBytes;
    }

    /**
     * Calculate the MAC
     *
     * @param consumeFee consumeFee
     * @return byte[] if not null, it work success.
     */
    public byte[] calculateMac(int consumeFee) {
        byte[] returnBytes = null;
        byte[] consumeFeeBytes = null;
        if (consumeFee <= 0) {
            consumeFeeBytes = ByteUtil.hexStringToByteArray(cmd_init_consume_fee);
        } else {
            consumeFeeBytes = ByteUtil.intToByteArray(consumeFee);
        }
        printLog("Transaction amount：" + ByteUtil.byteArrayToIntHighToLow(consumeFeeBytes));
        byte[] sendCmdBytes = null;
        sendCmdBytes = consumeFeeBytes;
        sendCmdBytes = ByteUtil.addBytes(sendCmdBytes, (byte) 0x06);
        //Add the terminal number
        sendCmdBytes = ByteUtil.addBytes(sendCmdBytes, ByteUtil.hexStringToByteArray
                (cmd_init_consume_terminal_no));
        //Trading hours
        String hexTradeTime = CalendarUtil.getDefaultYYYYMMddHHmmss();
        printLog("Trading hours" + hexTradeTime);
        sendCmdBytes = ByteUtil.addBytes(sendCmdBytes, ByteUtil.hexStringToByteArray(hexTradeTime));
        //Calculate the MAC
        byte[] keys = calculateSecondDes(consumeFee);
        printLog("Mac sendCmdBytes：" + ByteUtil.byteArrayToHexArray(sendCmdBytes));
        printLog("Mac keys：" + ByteUtil.byteArrayToHexArray(keys));
        returnBytes = mCpuCardSecretKeyHelper.callMacAnyLength(sendCmdBytes, keys);
        return returnBytes;
    }


    /**
     * Calculate the first of　Des.
     *
     * @return byte[] Return des results
     */
    public byte[] calculateFirstDes() {
        byte[] cardSn = getCardResetBytes();
        byte[] returnBytes = null;
        returnBytes = mCpuCardSecretKeyHelper.getCpuCardKey(cardSn);
        return returnBytes;
    }

    /**
     * According to the input amount of CPU card consumption process,and the consumption result is returned
     * @param consumeFee
     * @return byte[] Return consumption results
     */
    public byte[] cpuConsume(int consumeFee) {
        mCardUtil.initDev();
        initCpuConsumeBytes = null;
        byte[] returnBytes = null;
        byte[] sendCmdBytes = null;
        sendCmdBytes = ByteUtil.hexStringToByteArray(cmd_debit);
        //Transaction number
        byte[] consumeConutBytes = ByteUtil.intToByteArray(consumeCount);
        sendCmdBytes = ByteUtil.addBytes(sendCmdBytes, consumeConutBytes);
        //Trading hours
        String hexTradeTime = CalendarUtil.getDefaultYYYYMMddHHmmss();
        sendCmdBytes = ByteUtil.addBytes(sendCmdBytes, ByteUtil.hexStringToByteArray(hexTradeTime));
        //mac
        byte[] macBytes = calculateMac(consumeFee);
        sendCmdBytes = ByteUtil.addBytes(sendCmdBytes, ByteUtil.copyBytes(macBytes, 0, 4));
        sendCmdBytes = ByteUtil.addBytes(sendCmdBytes, (byte) 0x08);
        //Send consumption command
        returnBytes = sendCpuCmd(sendCmdBytes, true);

        if (ByteUtil.notNull(returnBytes)) {
            //Consumption is successful.
            consumeCount ++;
        }
        return returnBytes;
    }

    public byte[] getInitCpuConsumeBytes() {
        return initCpuConsumeBytes;
    }

    public void clear() {
        fileBytes = null;
    }


}
