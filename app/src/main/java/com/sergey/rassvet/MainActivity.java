package com.sergey.rassvet;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.jaredrummler.android.colorpicker.ColorShape;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;

import static android.R.layout.simple_list_item_1;
import static android.view.View.generateViewId;
import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity implements ColorPickerDialogListener {

    Button btnConnect;
    Button btnGetTime;
    Button btnSetTime;
    Button btnCurTime;
    Button btnGetRasp;
    Button btnSortRasp;
    Button btnSetRasp;

    TextView shTime;

    TextView[] rTime= new TextView[22];
    Button[] rColor = new Button[22];

    BluetoothAdapter Bluetooth;
    ArrayList<String> pairedDeviceArrayList;
    ArrayAdapter<String> pairedDeviceAdapter;
    ListView listViewPairedDevice;
    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;
    private final UUID myUUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int TIMEOUT = 5000;
    private static final int EVENT_COUNT = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btnConnect);
        btnGetTime = findViewById(R.id.btnGetTime);
        btnSetTime = findViewById(R.id.btnSetTime);
        btnCurTime = findViewById(R.id.btnCurTime);
        btnGetRasp = findViewById(R.id.btnGetRasp);
        btnSortRasp = findViewById(R.id.btnSortRasp);
        btnSetRasp = findViewById(R.id.btnSetRasp);
        shTime =  findViewById(R.id.shTime);
        listViewPairedDevice = findViewById(R.id.list);

        CreateRasp();
    }
    public void CreateRasp() {
        ConstraintLayout layout = findViewById(R.id.lMain);
        View.OnClickListener onclkTV = this::onshTimeClick;
        View.OnClickListener onclkColor = this::onshColorClick;

        for (int j=0;j<3;j++)
            for (int i = 0; i < 8; i++) {
                if (j*8+i>EVENT_COUNT-1) break;
                rTime[j*8+i] = new TextView(this);
                rTime[j*8+i].setText(R.string.df_time);
                rTime[j*8+i].setOnClickListener(onclkTV);
                rTime[j*8+i].setX(16 + 400 * j);
                rTime[j*8+i].setY(530 + 100 * i);
                rTime[j*8+i].setClickable(true);
                rTime[j*8+i].setFocusable(true);
                layout.addView(rTime[j*8+i]);

                rColor[j*8+i] = new Button(this);
                rColor[j*8+i].setId(generateViewId ());
//                rColor[j*11+i].setWidth(200);
//                rColor[j*11+i].setHeight(30);
                rColor[j*8+i].setX(110 + 400 * j);
                rColor[j*8+i].setY(500 + 100 * i);
                SetColor(rColor[j*8+i],0x00000000);
                rColor[j*8+i].setOnClickListener(onclkColor);

//               ConstraintLayout.LayoutParams linnear_lay = new ConstraintLayout.LayoutParams(200,36);
//               linnear_lay.height=30;
//               linnear_lay.width=200;
//               rColor[j*11+i].setLayoutParams(linnear_lay);
                layout.addView(rColor[j*8+i]);
            }
    }
    public void onConnectClick(View v) {
        Bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (Bluetooth != null) { /* проверим наличие и поддержку блютус */
            /* С Bluetooth все в порядке. */
            if (Bluetooth.isEnabled()) { /* Bluetooth включен. Работаем. */
                setup();
            } else { /* Bluetooth выключен. Предложим пользователю включить его. */
                /* включить блютус */
                //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                /* включить блютус на 120сек (по умолчанию) */
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                /* включить блютус на 300сек */
//TODO разобраться почему не отклчеться блюус через заданное время
                enableBtIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            makeText(MainActivity.this, "Bluetooth не поддерживается", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {// Если не разрешили, тогда закрываем приложение
                makeText(this, "BlueTooth не включён", Toast.LENGTH_SHORT).show();
                finish();
            } else setup();
        }
    }
    public void onshTimeClick(View v) {
                new TimePickerDialog(MainActivity.this, (view, hourOfDay, minute) -> ((TextView)v).setText(GetStringTime(hourOfDay,minute)), GetHourTime(((TextView)v).getText().toString()), GetMinuteTime(((TextView)v).getText().toString()), true).show();
    }
    public void onshColorClick(View v) {
        createColorPickerDialog(v.getId());
    }
    public void onGetTimeClick(View v) { /* получение времени, отправляем FD FD, получаем часы и минуты */
        byte[] bytesToSend = {(byte) 0xFD, (byte) 0xFD};
        myThreadConnected.write(bytesToSend);
        new Thread(() -> {
            long endTime = System.currentTimeMillis() + TIMEOUT;
            while ((System.currentTimeMillis() < endTime) & (myThreadConnected.BytesRead() < 2)); /* подождем пока буфер не наполнится но не больше секунды  */
            if (myThreadConnected.BytesRead() > 1) { /* если в буфере 2 и больше байтов значит ответ есть*/
                byte[] bytesToRead = new byte[2];
                myThreadConnected.ReadBuffer(bytesToRead);
                runOnUiThread(() -> shTime.setText(GetStringTime(bytesToRead[0], bytesToRead[1])));
            } else runOnUiThread(() -> makeText(MainActivity.this, "Нет ответа от Bluetooth-устройства за "+TIMEOUT+" милисекунд", Toast.LENGTH_LONG).show());
        }).start();
    }
    public void onSetTimeClick(View v) { /* установка времени, отправляем FE FE и часы и минуты */
        byte[] bytesToSend = {(byte) 0xFE, (byte) 0xFE, 0, 0}; /* объявили массив на четыре байта, третий и четвертый байт заполним далее */
        bytesToSend[2] = (byte) GetHourTime(shTime.getText().toString());
        bytesToSend[3] = (byte) GetMinuteTime(shTime.getText().toString());
        myThreadConnected.write(bytesToSend);
    }
    public void onCurTimeClick(View v) {/* установка текущего времени */
        final Calendar cal = Calendar.getInstance();
        shTime.setText(GetStringTime(cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE)));
    }
    public void onGetRaspClick(View v) {
        byte[] bytesToSend = {(byte) 0xFF, (byte) 0xFF};
        myThreadConnected.write(bytesToSend);
        Log.v("myLogs", String.format("Отправляем %X %X",bytesToSend[0],bytesToSend[1]));
        new Thread(() -> {
            long endTime = System.currentTimeMillis() + TIMEOUT;
            while ((System.currentTimeMillis() < endTime) & (myThreadConnected.BytesRead() < EVENT_COUNT*5));/* подождем пока буфер не наполнится но не больше секунды  */
            if (myThreadConnected.BytesRead() > EVENT_COUNT*5-1) { /* если в буфере 110 и больше байтов значит ответ есть*/
                byte[] buff = new byte[110];
                int br = myThreadConnected.ReadBuffer(buff);
                Log.v("myLogs", "Получили в функции " + br + " байт");
                runOnUiThread(() -> {
                    for (int i = 0; i < EVENT_COUNT; i++) {
                        rTime[i].setText(GetStringTime(buff[i * 5], buff[i * 5 + 1]));
                        SetColor(rColor[i], (buff[i * 5 + 2] << 16) | (buff[i * 5 + 3] << 8) | buff[i * 5 + 4]);
                    }
                });
            } else runOnUiThread(() ->makeText(MainActivity.this, "Нет ответа от Bluetooth-устройства за "+TIMEOUT+" милисекунд", Toast.LENGTH_LONG).show());
        }).start();
    }
    public void onSortRaspClick(View v) {
        for (int i=0; i<EVENT_COUNT-1; i++)
            for (int j = i+1; j<EVENT_COUNT; j++)
                if (CompFirstMore(rTime[i].getText().toString(), rTime[j].getText().toString())) {
                    String tmp_time = rTime[i].getText().toString();
                    rTime[i].setText(rTime[j].getText().toString());
                    rTime[j].setText(tmp_time);

                    int tmp_color = GetBgColor(rColor[i]);
                    SetColor(rColor[i], GetBgColor(rColor[j]));
                    SetColor(rColor[j], tmp_color);
                }
    }
    private boolean CompFirstMore(String st_time1, String st_time2) {
        // true если первое время больше и надо менять местами
        int h1=GetHourTime(st_time1);
        int h2=GetHourTime(st_time2);
        if (h1>h2) return true;
        if (h1==h2) {
            int m1=GetMinuteTime(st_time1);
            int m2=GetMinuteTime(st_time2);
            return (m1>m2);
        } else return false;
    }
    public void onSetRaspClick(View v) {
        Log.v("myLogs", "Устанавливаем события");
        new Thread(() -> {
            byte[] buff = new byte[10];
            for (int i = 0; i < EVENT_COUNT; i++) {
                Log.v("myLogs", "Устанавливаем " + i + " событие");
                buff[0] = (byte) (i * 5);
                buff[1] = (byte) GetHourTime(rTime[i].getText().toString());
                buff[2] = (byte) (i * 5 + 1);
                buff[3] = (byte) GetMinuteTime(rTime[i].getText().toString());
                buff[4] = (byte) (i * 5 + 2);
                buff[5] = (byte) ((GetBgColor(rColor[i]) >> 16) & 0xFF);
                buff[6] = (byte) (i * 5 + 3);
                buff[7] = (byte) ((GetBgColor(rColor[i]) >> 8) & 0xFF);
                buff[8] = (byte) (i * 5 + 4);
                buff[9] = (byte) (GetBgColor(rColor[i]) & 0xFF);
                myThreadConnected.write(buff);
                Log.v("myLogs", "Отправили буфер");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            byte[] bytesToSend = {(byte) 0xFC, (byte) 0xFC}; /* отправить FC FC что бы перезагрузить расписание*/
            myThreadConnected.write(bytesToSend);
            runOnUiThread(() ->makeText(MainActivity.this, "Расписание отправленно", Toast.LENGTH_LONG).show());
        }).start();
    }

    @SuppressLint("DefaultLocale")
    private String GetStringTime(int Hour, int Minute) {
        return String.format("%02d:%02d",Hour,Minute);
    }
    private int GetHourTime(String time) {
        return Integer.parseInt(time.substring(0,time.indexOf(":")));
    }
    private int GetMinuteTime(String time) {
        return Integer.parseInt(time.substring(time.indexOf(":")+1));
    }
    private int GetBgColor(Button bt) {
        ColorDrawable colorDrawable = (ColorDrawable) bt.getBackground();
        return colorDrawable.getColor();
    }
    private void SetColor(Button bt, int color) {
        bt.setBackgroundColor(0xFF000000 | color);
        bt.setText(String.format("#%06X", 0xFFFFFF & color));
        int cR = (color >> 16) & 0xFF;
        int cG = (color >> 8) & 0xFF;
        int cB = (color) & 0xFF;
        /* если цвет светлый кнопки светлый то сделать цвет текста темный */
        if ((cR>0x80)&&(cG>0x80)&&(cB>0x80)) bt.setTextColor(0xFF000000); else bt.setTextColor(0xFFFFFFFF);
    }

    private void setup() {
//TODO сделать поиск неспаренных устрйоств
        /* показываем список спаренных устройств */
        Set<BluetoothDevice> pairedDevices = Bluetooth.getBondedDevices();
        if (pairedDevices.size() > 0) { // Если есть сопряжённые устройства
            pairedDeviceArrayList = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) { // Добавляем сопряжённые устройства - Имя + MAC-адресс
                pairedDeviceArrayList.add(device.getName() + "\n" + device.getAddress());
            }
            pairedDeviceAdapter = new ArrayAdapter<>(this, simple_list_item_1, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);
            // Клик по нужному устройству
            listViewPairedDevice.setOnItemClickListener((parent, view, position, id) -> {
                String  itemValue = (String) listViewPairedDevice.getItemAtPosition(position);
                String MAC = itemValue.substring(itemValue.length() - 17); // Вычленяем MAC-адрес
                BluetoothDevice device2 = Bluetooth.getRemoteDevice(MAC);
                myThreadConnectBTdevice = new ThreadConnectBTdevice(device2);
                myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
            });
        }
    }
    private class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth
        private BluetoothSocket bluetoothSocket = null;
        private ThreadConnectBTdevice(BluetoothDevice device) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() { // Коннект
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            }
            catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> makeText(MainActivity.this, "Нет коннекта, проверьте Bluetooth-устройство с которым хотите соединица!", Toast.LENGTH_LONG).show());
                try {
                    bluetoothSocket.close();
                }
            catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if(success) {  // Если законнектились, тогда открываем панель с кнопками и запускаем поток приёма и отправки данных
                runOnUiThread(() -> {
                    listViewPairedDevice.setVisibility(View.INVISIBLE);
                    btnConnect.setVisibility(View.INVISIBLE);
                    btnGetTime.setVisibility(View.VISIBLE);
                    btnSetTime.setVisibility(View.VISIBLE);
                    btnCurTime.setVisibility(View.VISIBLE);
                    btnGetRasp.setVisibility(View.VISIBLE);
                    btnSortRasp.setVisibility(View.VISIBLE);
                    btnSetRasp.setVisibility(View.VISIBLE);
                    shTime.setVisibility(View.VISIBLE);
                });
                myThreadConnected = new ThreadConnected(bluetoothSocket);
                myThreadConnected.start(); // запуск потока приёма и отправки данных
            }
        }
    }
    private static class ThreadConnected extends Thread {    // Поток - приём и отправка данных
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;
        private int ReadBytes=0;
        private final byte[] buf = new byte[255];
        public ThreadConnected(BluetoothSocket socket) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            connectedInputStream = in;
            connectedOutputStream = out;
        }
        @Override
        public void run() { // Приём данных
            while (true) {
                try {
                    byte[] buffer = new byte[1];
                    int bytes = connectedInputStream.read(buffer);
                    Log.v("myLogs", String.format("Получили в приемнике байт (%#X)", buffer[0]));
// TODO а что произойжет если будет переполнение? Мы его потеряем?
                    if (ReadBytes < buf.length) buf[ReadBytes++] = buffer[0];
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public int BytesRead() {
            return ReadBytes;
        }
        public int ReadBuffer(byte[] buffer) {
            System.arraycopy(buf,0,buffer,0,255);
            int rb=ReadBytes;
            ReadBytes=0;
            return rb;
        }
    }

    private void createColorPickerDialog(int id) {
        Button btColor = findViewById(id);
        int colorId = GetBgColor(btColor);

        ColorPickerDialog.newBuilder()
                .setColor(colorId)
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setAllowCustom(true)
                .setAllowPresets(true)
                .setColorShape(ColorShape.SQUARE)
                .setShowAlphaSlider(true)
                .setShowColorShades(true)
                .setDialogId(id)
                .show(this);
    }
    @Override
    public void onColorSelected(int dialogId, int color) {
        Button btColor = findViewById(dialogId);
        SetColor(btColor,color);
    }
    @Override
    public void onDialogDismissed(int dialogId) {}
}