package com.xlabs.cardbossapp.secret;

/**
 * Created by cardlan on 18-6-4.
 */

public interface ICardDataListener {
    /**
     * Read the card's data
     * @param sector
     * @param index  the index for each block in the sector
     * @param readBytes byte[] data read from the sector
     * @param readFinished data reading is finished
     */
    void readMsg(int sector, int index, byte[] readBytes, boolean readFinished);

    /**
     * The data returned when the card is reset
     * @param bytes
     */
    void cardResetMsg(byte[] bytes);

}
