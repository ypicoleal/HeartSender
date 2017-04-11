package com.github.ypicoleal.heartsender;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;


public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DevicesViewHolder> {

    ArrayList<String> devices;
    ListItemClickListener mOnClickListener;

    public DevicesAdapter(ListItemClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    @Override
    public DevicesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.device;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, parent, false);

        return new DevicesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DevicesViewHolder holder, int position) {
        holder.title.setText(devices.get(position));
    }

    @Override
    public int getItemCount() {
        if (devices == null) {
            return 0;
        }
        return devices.size();
    }

    public void setDevices(ArrayList<String> devices) {
        this.devices = devices;
        notifyDataSetChanged();
    }

    public interface ListItemClickListener {
        void onListItemClick(int position);
    }

    class DevicesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title;

        public DevicesViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.device_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mOnClickListener.onListItemClick(getAdapterPosition());
        }
    }
}
