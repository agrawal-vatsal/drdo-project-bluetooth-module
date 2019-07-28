package drdo.drdoproject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static String DEVICE_NAME = "HC-05";
    public BarChart chart;
    ConnectThread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        chart = findViewById(R.id.chart);

        Intent intent = getIntent();
        DEVICE_NAME = intent.getStringExtra("deviceName");

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> bluetoothDevices = adapter.getBondedDevices();

        BluetoothDevice device = null;
        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            if (bluetoothDevice.getName().equals(DEVICE_NAME)) {
                device = bluetoothDevice;
                break;
            }
        }

        thread = new ConnectThread(device, true, adapter, null, this);

        new AppExecutors().networkIO().execute(() -> {
            try {
                thread.connect();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        findViewById(R.id.led_on).setOnClickListener(v -> {
            try {
                thread.sendData(thread.bluetoothSocket, '1');
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        findViewById(R.id.led_off).setOnClickListener(v -> {
            try {
                thread.sendData(thread.bluetoothSocket, '0');
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }


    public void updateChart(String data) {
        ArrayList<Integer> yValues = new ArrayList<>();
        ArrayList<Character> xValues = new ArrayList<>();
        boolean start = false;
        for (int i = 0; i < data.length(); i++) {
            if (start) {
                if (data.charAt(i) == '$')
                    break;
                if (Character.isLetter(data.charAt(i)))
                    xValues.add(data.charAt(i));
                else if (Character.isDigit(data.charAt(i)))
                    yValues.add(Integer.parseInt(String.format("%c", data.charAt(i))));
            } else if (data.charAt(i) == '#')
                start = true;
        }
        List<BarEntry> entries = new ArrayList<>();
        if (yValues.size() != xValues.size())
            Toast.makeText(getApplicationContext(), "Invalid data sent", Toast.LENGTH_SHORT).show();
        for (int i = 0; i < yValues.size(); i++)
            entries.add(new BarEntry(xValues.get(i), yValues.get(i)));
        BarDataSet dataSet = new BarDataSet(entries, "Label");
        BarData barData = new BarData(dataSet);
        chart.setData(barData);
        chart.invalidate();
    }

    //00:18:EF:03:DD:02
}
