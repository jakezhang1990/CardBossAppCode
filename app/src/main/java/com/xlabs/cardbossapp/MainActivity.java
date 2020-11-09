package com.xlabs.cardbossapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cardlan.utils.ByteUtil;
import com.xlabs.cardbossapp.util.CardReadWriteUtil;
import com.xlabs.cardbossapp.data.KeyErrorException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    CardReadWriteUtil mReadWriteUtil = new CardReadWriteUtil();
    Toast mToast;
    private String mReadOrWriteKeyHexStr;
    TerminalConsumeDataForSystem terminal;
    private EditText mEditxt_sector_read , mEditxt_read_index;
    private TextView mRead_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditxt_sector_read = findViewById(R.id.mEditxt_sector_read);
        mEditxt_read_index = findViewById(R.id.mEditxt_read_index);
        mRead_result = findViewById(R.id.mTxtView_read_result);

        Button mBtn_read_card = findViewById(R.id.mBtn_read_card);
        mBtn_read_card.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        char readSector = stringToChar(mEditxt_sector_read.getText().toString());
        char readindex = stringToChar(mEditxt_read_index.getText().toString());

        if (v.getId()==R.id.mBtn_read_card){
            byte[] resetByte = mReadWriteUtil.getCardResetBytes();
            if (!ByteUtil.notNull(resetByte)) {
                showToast(getResources().getString(R.string.not_find_card));
                return;
            }
            try {
                mReadOrWriteKeyHexStr = ByteUtil.byteArrayToHexString
                        (terminal.calculateNormalCardKey(resetByte));
            } catch (KeyErrorException e) {
                e.printStackTrace();
            }

            byte sector = ByteUtil.intToByteTwo(readSector);
            byte index = ByteUtil.intToByteTwo(readindex);
            byte[] readTemp = null;
            // the subsequent reads and writes need to be written using the computed read key
            readTemp = mReadWriteUtil.callReadJNI(ByteUtil.byteToHex(sector),
                    ByteUtil.byteToHex(index), mReadOrWriteKeyHexStr, null);
            if (ByteUtil.notNull(readTemp)) {
                mRead_result.setText(ByteUtil.byteArrayToHexString(readTemp));
            }
        }
    }




    public void showToast(String msg) {
        if (mToast != null) {
            mToast.cancel();
            mToast = null;
        }
        mToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private char stringToChar(String string) {
        if (!ByteUtil.notNull(string)) {
            string = "0";
        }
        int ivalue = Integer.parseInt(string);
        return (char) ivalue;
    }
}