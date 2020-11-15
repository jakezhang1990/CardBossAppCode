package com.xlabs.cardbossapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cardlan.twoshowinonescreen.CardLanStandardBus;
import com.cardlan.utils.ByteUtil;
import com.xlabs.cardbossapp.util.CardReadWriteUtil;
import com.xlabs.cardbossapp.data.KeyErrorException;
/**
 * @Author: jakezhang
 * Company:DHC
 * Description： 描述内容
 * Date: 2020/11/10 14:21
 *一：卡数据格式：
 * 1、IC 卡数据为十六进制；
 * 2、金额精确到分，即以分存储数据；
 * 3、金 额 代 码 ， 如 100.00 元 ， 其 十 六 进 制 数 为 2710 ， 则 其 金 额 代 码 为 102700 ， 金 额 累 加 和 校 验 值 =0x10+0x27+0x00=0x37；金额累加和取反校验值= 0xC8；
 * 4、限额低位、限额高位和限额日/月未使用，其值为 0x00；
 * 5、消费次数，办卡时，消费次数设为 1，然后每对卡金额操作一次则消费次数加 1，如新卡其消费次数为 1，即 0x010000，消费次数要按低中高位存储；
 * 6、更新块 0 时，同时要更新块 2，块 2 数据为块 0 数据的备份数据；
 * 7、转款只需要改写 2 字节和 3 字节的金额块即可，金额范围 0-65535（16 进制最大 655.35 元）。4 字节、6-E 字 节，不能改变其内容。改写金额完毕后，除改变金额校验外，整块的校验 0 字节和 F 字节也需要改变。遵循先 写块 2 再写块 0 的原则，块 2 是块 0 的备份块。
 *
 * 二：卡读写密钥：
 * KEYA = 0x0A + 4 字节序列号的取反 +0x81
 * 密码 A 为常规验证密码
 * 控制字=7F078869
 * KEYB = A2020BC1027D
 *
 * 三：使用扇区：
 * 卡机所使识别的扇区号为 10 扇区号，扇区号由 0 开始；
 *
 *
 * 卡片共16个扇区0-15，一个扇区有4个块，0-3.
 *  keyA keyB为卡片的校验秘钥，按照卡片的校验规则即可，比如卡片是用keyA校验，填keyA，用keyB校验就填keyB。
 *  序列号是从SDK的cardReset（）读出来的。
 *
 *  卡片用的keyA，还是keyB，常规验证密码需要和卡片厂家沟通。
 *  结果：
 *      采用keyA。初始秘钥12个F，也就是FFFFFFFFFFFF. 也有可能是其他的。待确认。这个秘钥是否是动态变化的。
 */
public class MainActivity extends Activity/*AppCompatActivity*/ implements View.OnClickListener {

    CardReadWriteUtil mReadWriteUtil = new CardReadWriteUtil();
    CardLanStandardBus mCardLanDevCtrl = new CardLanStandardBus();
    private TextView mInitDev_status_value;

    private TextView mTvResetCard;
    private TextView mWrite_value;

    Toast mToast;
    private String mReadOrWriteKeyHexStr;
    TerminalConsumeDataForSystem terminal;
    private EditText mEditxt_sector_read , mEditxt_read_index ,mEditxt_read_key_type ,mEditxt_read_key;
    private TextView mRead_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mBtn_initDev = findViewById(R.id.mBtn_initDev);
        mBtn_initDev.setOnClickListener(this);
        mInitDev_status_value = findViewById(R.id.mTxtView_initDev_status_value);

        Button mBtn_reset_card = findViewById(R.id.mBtn_reset_card);
        mBtn_reset_card.setOnClickListener(this);
        mTvResetCard = findViewById(R.id.tv_reset_card);

        Button mBtn_write_card = findViewById(R.id.mBtn_write_card);
        mBtn_write_card.setOnClickListener(this);
        mWrite_value = findViewById(R.id.mTxtView_write_statusvalue);


        mEditxt_sector_read = findViewById(R.id.mEditxt_sector_read);//扇区 取值10号扇区
        mEditxt_read_index = findViewById(R.id.mEditxt_read_index);//块 块号

        mEditxt_read_key_type=findViewById(R.id.mEditxt_read_key_type);//读写操作类型 0x0a 是读 0x0b是写
        mEditxt_read_key=findViewById(R.id.mEditxt_read_key);//读卡的秘钥 key 默认值12F 什么意思？

        mRead_result = findViewById(R.id.mTxtView_read_result);

        Button mBtn_read_card = findViewById(R.id.mBtn_read_card);
        mBtn_read_card.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        String rkey = mEditxt_read_key_type.getText().toString();
        Toast.makeText(this, "rkey="+rkey, Toast.LENGTH_SHORT).show();

        char readSector = stringToChar(mEditxt_sector_read.getText().toString());
        char readindex = stringToChar(mEditxt_read_index.getText().toString());

        if(v.getId()==R.id.mBtn_initDev){
            //init Devices 初始化之后才可以读写
            int initResult = mCardLanDevCtrl.callInitDev();
            if (0 == initResult || -2 == initResult || -3 == initResult || -4 == initResult) {
                mInitDev_status_value.setText(getResources().getString(R.string.init_dev) + " success!");

            } else {
                mInitDev_status_value.setText(getResources().getString(R.string.init_dev) + " failure!");
            }
            showToast(mInitDev_status_value.getText().toString());
        }else if(v.getId()==R.id.mBtn_reset_card){
            //reset

            mReadOrWriteKeyHexStr = null;
//                byte[] cardNumber = new byte[S_Reset_buffer_size];
//                int cardResult = mCardLanDevCtrl.callCardReset(cardNumber);
            byte[] resetbyte = mReadWriteUtil.getCardResetBytes();
            if (ByteUtil.notNull(resetbyte)) {
                mReadOrWriteKeyHexStr = ByteUtil.byteArrayToHexString(resetbyte);
                mTvResetCard.setText(mReadOrWriteKeyHexStr);

            } else {
                showToast("reset：failure!");
                mTvResetCard.setText("");
            }
        }else if (v.getId()==R.id.mBtn_read_card){
            //read M1 card

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
            showToast(ByteUtil.byteArrayToHexString(readTemp));
        }else if(v.getId()==R.id.mBtn_write_card){
            //write M1 card

            mReadOrWriteKeyHexStr = null;
            byte[] resetBytes = mReadWriteUtil.getCardResetBytes();
            if (!ByteUtil.notNull(resetBytes)) {
                showToast(getResources().getString(R.string.not_find_card));
                return;
            }
            try {
                mReadOrWriteKeyHexStr = ByteUtil.byteArrayToHexString
                        (terminal.calculateNormalCardKey(resetBytes));
            } catch (KeyErrorException e) {
                e.printStackTrace();
            }

            //int money= 100000;
            int writeResult = mReadWriteUtil.callWriteJNI("4",
                    "1",
                    ByteUtil.byteArrayToHexString(new byte[4]),
                    mReadOrWriteKeyHexStr, null);
            if (writeResult == 5) {
                mWrite_value.setText(getResources().getString(R.string.m1_write_value) + writeResult);
                showToast(getResources().getString(R.string.writing_successfully));
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