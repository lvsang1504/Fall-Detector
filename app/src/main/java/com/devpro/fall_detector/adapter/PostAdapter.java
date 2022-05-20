package com.devpro.fall_detector.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.devpro.fall_detector.R;
import com.devpro.fall_detector.listeners.MapListener;
import com.devpro.fall_detector.models.FallResponse;
import com.devpro.fall_detector.utilities.TimeAgo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private List<FallResponse> mData;
    MapListener listener;

    public PostAdapter(List<FallResponse> mData, MapListener listener) {
        this.mData = mData;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        /////*     initialize view   */////
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.list_post_item, viewGroup, false);
        ViewHolder vh = new ViewHolder(view);
        return vh;

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        // set values for each item

        Date date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = mData.get(position).time;
        try {
            date = sdf.parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        viewHolder.fallText.setText("Phát hiện té ngã " + new TimeAgo().covertTimeToText(mData.get(position).time));
        viewHolder.text_date.setText(sdf.format(date));

        viewHolder.btn_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.mapViewClicks(mData.get(viewHolder.getAdapterPosition()));
            }
        });

        Log.d("SOS", "Vị trí latitude: " + mData.get(position).longitude + " và latitude: "+ mData.get(position).latitude);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView fallText, text_date;
        CardView btn_map;

        public ViewHolder(View v) {
            super(v);
            fallText = v.findViewById(R.id.id_postText_TextView);
            text_date = v.findViewById(R.id.text_date);
            btn_map = v.findViewById(R.id.btn_map);
        }

    }
}
