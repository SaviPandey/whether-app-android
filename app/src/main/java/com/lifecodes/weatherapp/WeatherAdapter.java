package com.lifecodes.weatherapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.ViewHolder> {

    private ArrayList<WeatherModel> arrayList;
    Context context;

    public WeatherAdapter(ArrayList<WeatherModel> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        WeatherModel model = arrayList.get(position);
        holder.iconIv.setImageDrawable(model.getIcon());
        holder.tempTv.setText(String.format("%.0f", model.getTemp()) + "°");

        Date timeD = new Date(Integer.parseInt(String.valueOf(model.getTime())) * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        holder.timeTv.setText(sdf.format(timeD));

//        long unixTime = System.currentTimeMillis() / 1000L;
//        Log.d("unix", String.valueOf(unixTime));
//        if (model.getTime() <= unixTime) {
//            holder.rootLayout.setBackground(context.getResources().getDrawable(R.drawable.rounded_light));
//        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView iconIv;
        TextView tempTv, timeTv;
        LinearLayout rootLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            iconIv = itemView.findViewById(R.id.icon_iv);
            tempTv = itemView.findViewById(R.id.temp_tv);
            timeTv = itemView.findViewById(R.id.time_tv);
            rootLayout = itemView.findViewById(R.id.rootLayout);
        }
    }
}
