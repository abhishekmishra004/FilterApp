package com.application.databind.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.application.databind.R;
import com.application.databind.ui.CameraActivity;

import org.w3c.dom.Text;

import java.util.List;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterViewModel> {

    Context context;
    List<String> values;

    public FilterAdapter(Context context, List<String> values) {
        this.context = context;
        this.values = values;
    }

    @NonNull
    @Override
    public FilterViewModel onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FilterViewModel(LayoutInflater.from(context).inflate(R.layout.layout_filter_buttons,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull FilterViewModel holder, int position) {
        holder.tvLabel.setText(values.get(position));
        holder.tvLabel.setOnClickListener(v -> {
            //Toast.makeText(context,"pos-"+position,Toast.LENGTH_SHORT).show();
            ((CameraActivity) context).clickedIndex(position);
        });
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    public static class FilterViewModel extends RecyclerView.ViewHolder {
        TextView tvLabel;
        public FilterViewModel(@NonNull View itemView) {
            super(itemView);
            tvLabel = itemView.findViewById(R.id.tv_label);
        }


    }
}
