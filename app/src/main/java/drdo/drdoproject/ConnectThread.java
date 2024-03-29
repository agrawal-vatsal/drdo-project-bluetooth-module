package drdo.drdoproject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConnectThread {

    public BluetoothSocketWrapper bluetoothSocket;
    private BluetoothDevice device;
    private boolean secure;
    private BluetoothAdapter adapter;
    private List<UUID> uuidCandidates;
    private int candidate;
    private MainActivity activity;


    /**
     * @param device         the device
     * @param secure         if connection should be done via a secure socket
     * @param adapter        the Android BT adapter
     * @param uuidCandidates a list of UUIDs. if null or empty, the Serial PP id is used
     */
    ConnectThread(BluetoothDevice device, boolean secure, BluetoothAdapter adapter,
                  List<UUID> uuidCandidates, MainActivity activity) {
        this.device = device;
        this.secure = secure;
        this.adapter = adapter;
        this.uuidCandidates = uuidCandidates;
        this.activity = activity;
        if (this.uuidCandidates == null || this.uuidCandidates.isEmpty()) {
            this.uuidCandidates = new ArrayList<>();
            this.uuidCandidates.add(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        }
    }

    public BluetoothSocketWrapper connect() throws IOException {
        boolean success = false;
        while (selectSocket()) {
            adapter.cancelDiscovery();

            try {
                bluetoothSocket.connect();
                success = true;
                break;
            } catch (IOException e) {
                //try the fallback
                try {
                    bluetoothSocket = new FallbackBluetoothSocket(bluetoothSocket.getUnderlyingSocket());
                    Thread.sleep(500);
                    bluetoothSocket.connect();
                    success = true;
                    break;
                } catch (FallbackException e1) {
                    Log.w("BT", "Could not initialize FallbackBluetoothSocket classes.", e);
                } catch (InterruptedException e1) {
                    Log.w("BT", e1.getMessage(), e1);
                } catch (IOException e1) {
                    Log.w("BT", "Fallback failed. Cancelling.", e1);
                }
            }
        }

        if (!success) {
            throw new IOException("Could not connect to device: " + device.getAddress());
        }

        receiveData(bluetoothSocket);
        return bluetoothSocket;
    }

    private boolean selectSocket() throws IOException {
        if (candidate >= uuidCandidates.size()) {
            return false;
        }

        BluetoothSocket tmp;
        UUID uuid = uuidCandidates.get(candidate++);

        Log.i("BT", "Attempting to connect to Protocol: " + uuid);
        if (secure) {
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } else {
            tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
        }
        bluetoothSocket = new NativeBluetoothSocket(tmp);

        return true;
    }

    public interface BluetoothSocketWrapper {

        InputStream getInputStream() throws IOException;

        OutputStream getOutputStream() throws IOException;

        String getRemoteDeviceName();

        void connect() throws IOException;

        String getRemoteDeviceAddress();

        void close() throws IOException;

        BluetoothSocket getUnderlyingSocket();

    }


    public static class NativeBluetoothSocket implements BluetoothSocketWrapper {

        private BluetoothSocket socket;

        NativeBluetoothSocket(BluetoothSocket tmp) {
            this.socket = tmp;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return socket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return socket.getOutputStream();
        }

        @Override
        public String getRemoteDeviceName() {
            return socket.getRemoteDevice().getName();
        }

        @Override
        public void connect() throws IOException {
            socket.connect();
        }

        @Override
        public String getRemoteDeviceAddress() {
            return socket.getRemoteDevice().getAddress();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }

        @Override
        public BluetoothSocket getUnderlyingSocket() {
            return socket;
        }

    }

    public class FallbackBluetoothSocket extends NativeBluetoothSocket {

        private BluetoothSocket fallbackSocket;

        FallbackBluetoothSocket(BluetoothSocket tmp) throws FallbackException {
            super(tmp);
            try {
                Class<?> clazz = tmp.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[]{1};
                fallbackSocket = (BluetoothSocket) m.invoke(tmp.getRemoteDevice(), params);
            } catch (Exception e) {
                throw new FallbackException(e);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return fallbackSocket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return fallbackSocket.getOutputStream();
        }


        @Override
        public void connect() throws IOException {
            fallbackSocket.connect();
        }


        @Override
        public void close() throws IOException {
            fallbackSocket.close();
        }

    }

    public static class FallbackException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        FallbackException(Exception e) {
            super(e);
        }

    }

    void sendData(BluetoothSocketWrapper socket, char data) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(4);
        output.write(data);
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(output.toByteArray());
        Log.d("TAG", data + "");
        Log.d("DATA", "DATA SENT");
    }

    private void receiveData(BluetoothSocketWrapper socket) throws IOException {
        InputStream socketInputStream = socket.getInputStream();
        byte[] buffer = new byte[256];
        int bytes;

        StringBuilder data = new StringBuilder();
        // Keep looping to listen for received messages
        while (true) {
            try {
                bytes = socketInputStream.read(buffer);            //read bytes from input buffer
                String readMessage = new String(buffer, 0, bytes);
                // Send the obtained bytes to the UI Activity via handler
                Log.i("logging", readMessage + "");
                if (data.length() + readMessage.length() < 12) {
                    data.append(readMessage);
                } else if (data.length() + readMessage.length() == 12) {
                    data.append(readMessage);
                    updateChart(data.toString());
                    data = new StringBuilder();
                } else {
                    while (data.length() != 12) {
                        data.append(readMessage.charAt(0));
                        readMessage = readMessage.substring(1);
                    }
                    updateChart(data.toString());
                    data = new StringBuilder(readMessage);
                }
            } catch (IOException e) {
                break;
            }

        }
    }

    private void updateChart(String data) {
        new AppExecutors().mainThread().execute(() -> {
            activity.updateChart(data);
            Toast.makeText(activity, data, Toast.LENGTH_SHORT).show();
        });

    }
}
