package com.xlabs.cardbossapp.util;


import com.cardlan.twoshowinonescreen.CardLanStandardBus;
import com.cardlan.utils.ByteUtil;

/**
 * cpu card of Key helper class
 * Created by cardlan on 18-7-16.
 */

public class CpuCardSecretKeyHelper {

    //The key related
    private byte[] assembleBytes = new byte[]{0x26, (byte) 0x91, 0x13, 0x00};

    private String hexSrcKey = "1122334455667788";

    private String mErrorMsg = null;

    private CardLanStandardBus mCardLanDes = new CardLanStandardBus();

    /**
     * Access the encrypted key of the CPU card through sn.
     * @param cardSn
     * @return
     */
    public byte[] getCpuCardKey(byte[] cardSn) {
        CardlanLog.debugOnConsole(this.getClass(), ByteUtil.byteArrayToHexString(cardSn));

        mErrorMsg = null;
        byte[] returnBytes = null;
        if (!ByteUtil.notNull(cardSn)) {
            mErrorMsg = "the card sn byte array is null";
            return returnBytes;
        }
        byte[] tempOne = null;
        byte[] srcKeyBytes = ByteUtil.hexStringToByteArray(hexSrcKey);
        if (!ByteUtil.notNull(srcKeyBytes)) {
            mErrorMsg = "the src Key Bytes is null";
            return returnBytes;
        }

        if (cardSn.length < 4) {
            mErrorMsg = "the card sn length is small than 4";
            return returnBytes;
        }

        byte[] valibCardSN = null;
        if (cardSn.length == 4) {
            valibCardSN = cardSn;
        } else {
            valibCardSN = ByteUtil.copyBytes(cardSn, 0, 4);
        }

        tempOne = ByteUtil.addBytes(srcKeyBytes, ByteUtil.naBytes(srcKeyBytes));
        CardlanLog.debugOnConsole(this.getClass(), "the temp one array is " + ByteUtil
                .byteArrayToHexArray(tempOne));
        byte[] desSrc = null;
        desSrc = ByteUtil.addBytes(valibCardSN, assembleBytes);
        byte[] tempTwo = ByteUtil.addBytes(desSrc, ByteUtil.naBytes(desSrc));
        CardlanLog.debugOnConsole(this.getClass(), "the temp two array is " + ByteUtil
                .byteArrayToHexArray(tempTwo));

        byte[] headOutBytes = new byte[8];

        byte[] headTempBytes = ByteUtil.copyBytes(tempTwo, 0, 8);
        CardlanLog.debugOnConsole(this.getClass(), "the headTempBytes is " + ByteUtil
                .byteArrayToHexArray(headTempBytes));
        CardlanLog.debugOnConsole(this.getClass(), " headTempBytes length" + headTempBytes
                .length + ", tempOne length " + tempOne.length);
        char resultHead = mCardLanDes.callRunDes((char) 0, (char) 0, headTempBytes,
                headOutBytes, tempOne);


        CardlanLog.debugOnConsole(this.getClass(), "the RunDes result is " + ByteUtil.byteToHex
                ((byte) resultHead));
        CardlanLog.debugOnConsole(this.getClass(), "the headOutBytes is " + ByteUtil
                .byteArrayToHexArray(headOutBytes));

        byte[] tailOutBytes = new byte[8];
        byte[] tailTempBytes = ByteUtil.copyBytes(tempTwo, tempTwo.length - 8, 8);
        CardlanLog.debugOnConsole(this.getClass(), "the tailTempBytes is " + ByteUtil
                .byteArrayToHexArray(tailTempBytes));
        CardlanLog.debugOnConsole(this.getClass(), " tailTempBytes length" + tailTempBytes
                .length + ", tempOne length " + tempOne.length);
        char resultTail = mCardLanDes.callRunDes((char) 0, (char) 0, tailTempBytes,
                tailOutBytes, tempOne);
        CardlanLog.debugOnConsole(this.getClass(), "the RunDes result is " + ByteUtil.byteToHex
                ((byte) resultTail));
        CardlanLog.debugOnConsole(this.getClass(), "the tailOutBytes is " + ByteUtil
                .byteArrayToHexArray(tailOutBytes));

        if (ByteUtil.notNull(headOutBytes) && ByteUtil.notNull(tailOutBytes)) {
            returnBytes = ByteUtil.addBytes(headOutBytes, tailOutBytes);
        } else {
            mErrorMsg = "get Cpu Card Key failed";
        }

        CardlanLog.debugOnConsole(this.getClass(), "the returnBytes array is " + ByteUtil
                .byteArrayToHexArray(returnBytes));
        return returnBytes;
    }

    public String getmErrorMsg() {
        return mErrorMsg;
    }

    //File selection
    private String cmd_file_select = "00A40000023F0100";
    //Initialize consumption _ headers
    private String cmd_init_consume = "805001020B";
    //Initialize the consumption _ key index
    private String cmd_init_consume_key_index = "01";
    //Initial consumption _ transaction amount (cent)
    private String cmd_init_consume_fee = "00000001";
    //Initialize the consumer transaction terminal number
    private String cmd_init_consume_terminal_no = "100000000321";
    //Initializes the consumption terminator
    private String cmd_init_consume_end = "0F";

    /**
     *　Gets the key for the consumption argument
     * @param consumeInitBytes Consumes the bytes returned after initialization
     * @return byte[]
     */
    public byte[] getConsumeInitKey(byte[] consumeInitBytes) {
        byte[] returnBytes = null;
        return returnBytes;
    }

    /**
     * Call the{@link CardLanDes#callRunDes(char, char, byte[], byte[], byte[])}. Gets the encrypted byte array
     *
     * @param srcBytes The encrypted source array
     * @param keyBytes　Encrypted key
     * @return
     */
    public byte[] callRunDes(byte[] srcBytes , byte[] keyBytes) {
        mErrorMsg = null;
        byte[] returnBytes = null;
        if (!ByteUtil.notNull(srcBytes)) {
            mErrorMsg = "the srcBytes length is null";
            return returnBytes;
        }
        if (!ByteUtil.notNull(keyBytes)) {
            mErrorMsg = "the keyBytes length is null";
            return returnBytes;
        }
        returnBytes = new byte[srcBytes.length];
        char resultHead = mCardLanDes.callRunDes((char) 0, (char) 0, srcBytes,
                returnBytes, keyBytes);
        if (resultHead == 00) {
            //TODO
        }
        return returnBytes;
    }

    /**
     * Call back any length of MAC,
     * {@link CardLanDes#MacAnyLength(byte[] initIn, byte[] srcBytes, byte[] outBytes, byte[] keyBytes)}.
     * @param srcBytes
     * @param keyBytes Encrypted key converted array
     * @return
     */
    public byte[] callMacAnyLength(byte[] srcBytes, byte[] keyBytes) {
        mErrorMsg = null;
        byte[] returnBytes = null;
        if (!ByteUtil.notNull(srcBytes)) {
            mErrorMsg = "the srcBytes length is null";
            return returnBytes;
        }
        if (!ByteUtil.notNull(keyBytes)) {
            mErrorMsg = "the keyBytes length is null";
            return returnBytes;
        }
        byte[] initInBytes = new byte[srcBytes.length];
        returnBytes = new byte[srcBytes.length];

        char macResult = mCardLanDes.MacAnyLength(initInBytes, srcBytes, returnBytes, keyBytes);
        if (macResult == 00) {
            //TODO
        }
        return returnBytes;
    }



}
