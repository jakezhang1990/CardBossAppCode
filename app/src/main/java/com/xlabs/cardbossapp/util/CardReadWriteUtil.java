package com.xlabs.cardbossapp.util;


import com.cardlan.twoshowinonescreen.CardLanStandardBus;
import com.cardlan.utils.ByteUtil;

/**
 * card operation util
 *
 * Created by cardlan on 18-6-5.
 */

public class CardReadWriteUtil {

    private CardLanStandardBus mCardLanDevCtrl;

    private boolean mHasInitDev;

    private static final int S_Reset_buffer_size = 32;

    private boolean mHasGetResetBytes = false;

    private int mInitStatus = -1;

    /**
     * get the card sn
     * @return return the card sn byte array, if not search card, it return null
     */
    public byte[] getCardResetBytes() {
        initDev();
        byte[] resetByte = new byte[S_Reset_buffer_size];
        int cardResult = mCardLanDevCtrl.callCardReset(resetByte);
        if (cardResult == 1) {
            mHasGetResetBytes = false;
            return null;
        }
        mHasGetResetBytes = true;
        return resetByte;
    }

    public CardReadWriteUtil() {
        mCardLanDevCtrl = new CardLanStandardBus();
    }

    /**
     * Reads data from a sector or block
     *
     * @param readSectorStr     Sector index, default 0
     * @param readIndexStr      Domain index, default 0
     * @param readkeyHexStr     The check Key for the read operation, default"0xFFFFFFFFFFFF"
     * @param readKeyAreaHexStr read block of Key,default:0x0b,It is only can operation to "0x0a" or "0x0b",if not，default:"0x0b"
     * @return byte[]
     */
    public byte[] callReadJNI(String readSectorStr, String readIndexStr, String readkeyHexStr,
                              String readKeyAreaHexStr) {
        char readSector = stringToChar(readSectorStr);
        char readIndex = stringToChar(readIndexStr);
        byte[] readkeyHex = hexToByteArray(readkeyHexStr);
        if (!ByteUtil.notNull(readKeyAreaHexStr)) {
            readKeyAreaHexStr = "0b";
        }

        if (!readKeyAreaHexStr.equals("0a") && !readKeyAreaHexStr.equals("0b")) {
            readKeyAreaHexStr = "0b";
        }
        char readKeyArea = ByteUtil.hexStringToChar(readKeyAreaHexStr);
        return callReadJNI(readSector, readIndex, readkeyHex, readKeyArea);
    }

    /**
     * Call the{@link CardLanDevCtrl#callReadOneSectorDataFromCard(char SectorNo, char BlockNo, char VerifyFlag, byte[] key_array, char mode)},
     * Read data from a sector or block
     *
     * @param readSector Sector index
     * @param readindex  block index
     * @param readkey The check Key for the read operation
     * @param readKeyArea  read block of Key,default:0x0b,It is only can operation to "0x0a" or "0x0b"
     * @return byte[]
     */
    public byte[] callReadJNI(char readSector, char readindex, byte[] readkey, char readKeyArea) {

        //Initialization machine
        initDev();
        char one = 1;

        if (!mHasGetResetBytes) {
            byte[] resetByte = new byte[S_Reset_buffer_size];
            int cardResult = mCardLanDevCtrl.callCardReset(resetByte);
        }

        byte[] readMsg = mCardLanDevCtrl.callReadOneSectorDataFromCard(readSector,
                readindex, one,
                readkey, readKeyArea);
        if (ByteUtil.notNull(readMsg)) {
            String realStr = ByteUtil.byteArrayToHexString(readMsg);
            CardlanLog.debugOnConsole(CardReadWriteUtil.class, "The information read is：" + realStr);
            return readMsg;
        }
        return null;
    }

    /**
     * Writes data to the card
     *
     * @param writeSectorStr  Sector index
     * @param writeindexStr  block index
     * @param writeHexStr  Write hexadecimal
     * @param hexWriteKey     default:"0xFFFFFFFFFFFF"
     * @param writeKeyAreaStr default:"0b"
     * @return  Returns the status of the write operation was successful
     */
    public int callWriteJNI(String writeSectorStr, String writeindexStr, String writeHexStr, String
            hexWriteKey, String writeKeyAreaStr) {
        char writeSector = stringToChar(writeSectorStr);
        char writeindex = stringToChar(writeindexStr);
        byte[] writeKey = hexToByteArray(hexWriteKey);
        if (!ByteUtil.notNull(writeKeyAreaStr)) {
            writeKeyAreaStr = "0b";
        }
        if (!writeKeyAreaStr.equals("0a") && !writeKeyAreaStr.equals("0b")) {
            writeKeyAreaStr = "0b";
        }
        char writeKeyArea = ByteUtil.hexStringToChar(writeKeyAreaStr);

        return callWriteJNI(writeSector, writeindex, writeHexStr, writeKey, writeKeyArea);
    }

    /**
     * Call the{@link CardLanDevCtrl#callWriteOneSertorDataToCard(byte[] SectorArray, char SectorNo, char BlockNo, char VerifyFlag, byte[] key_array, char mode)},
     * Writes data to the card
     *
     * @param writeSector Sector index
     * @param writeindex  block index
     * @param writeHexStr Write hexadecimal
     * @param writeKey the check Key for the read operation
     * @param readKeyArea The area to read the key
     * @return int
     */
    public int callWriteJNI(char writeSector, char writeindex, String writeHexStr, byte[]
            writeKey, char readKeyArea) {

        initDev();
        //reset card
        if (!mHasGetResetBytes) {
            byte[] resetByte = new byte[S_Reset_buffer_size];
            int cardResult = mCardLanDevCtrl.callCardReset(resetByte);
        }

        byte[] writeBytes = hexToByteArray(writeHexStr);
        char one = 1;
        int writeResult = mCardLanDevCtrl.callWriteOneSertorDataToCard(writeBytes,
                writeSector,
                writeindex, one,
                writeKey, readKeyArea);
        return writeResult;
    }

    /**
     * Call the{@link CardLanDevCtrl#callInitDev()},
     * Initialize the device. If it has been initialized, it will not be initialized again
     */
    public void initDev() {
        if (!ismHasInitDev()) {
            mInitStatus = mCardLanDevCtrl.callInitDev();
            CardlanLog.debugOnConsole(this.getClass(), "initStatus:" + mInitStatus);
            mHasInitDev = (mInitStatus == 0 || mInitStatus == -3 || mInitStatus == -4);
        }

    }

    private char stringToChar(String string) {
        if (!ByteUtil.notNull(string)) {
            string = "0";
        }

        return ByteUtil.intStringToChar(string);
    }

    private byte[] hexToByteArray(String hex) {
        if (!ByteUtil.notNull(hex)) {
            hex = "FFFFFFFFFFFF";
        }
        return ByteUtil.hexStringToByteArray(hex);
    }

    public void setmHasInitDev(boolean mHasInitDev) {
        this.mHasInitDev = mHasInitDev;

    }

    public boolean ismHasInitDev() {
        return mHasInitDev;
    }

    public void setmHasGetResetBytes(boolean mHasGetResetBytes) {
        this.mHasGetResetBytes = mHasGetResetBytes;
    }

    /**
     * Call the{@link CardLanDevCtrl#callCpuSendCmd(byte[] cmdArray, byte[] receiveArray)},
     * CPU card sends CMD command to communicate with hardware;
     * @param cmdArray cmd array
     * @param receiveArray receive array
     * @return
     */
    public int callSendCpuCmd(byte[] cmdArray, byte[] receiveArray) {
        return mCardLanDevCtrl.callCpuSendCmd(cmdArray, receiveArray);
    }


}
