package drdo.drdoproject;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class BluetoothDevicesAdapter extends RecyclerView.Adapter<BluetoothDevicesAdapter.BluetoothDeviceViewHolder> {

    private ArrayList<String> dataset;
    private Context context;

    BluetoothDevicesAdapter(ArrayList<String> dataset, Context context) {
        this.dataset = dataset;
        this.context = context;
    }

    @NonNull
    @Override
    public BluetoothDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View v = inflater.inflate(R.layout.recycler_list_item, viewGroup, false);
        return new BluetoothDeviceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothDeviceViewHolder bluetoothDeviceViewHolder, int i) {
        String deviceName = dataset.get(i);
        bluetoothDeviceViewHolder.textView.setText(deviceName);
        bluetoothDeviceViewHolder.view.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("deviceName", deviceName);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    class BluetoothDeviceViewHolder extends RecyclerView.ViewHolder {

        TextView textView;
        View view;

        BluetoothDeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.list_text_view);
            view = itemView;
        }
    }
}
