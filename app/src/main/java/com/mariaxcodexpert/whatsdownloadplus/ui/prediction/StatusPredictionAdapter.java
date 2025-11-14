package com.mariaxcodexpert.whatsdownloadplus.ui.prediction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.ContactEntity;

import java.util.List;

public class StatusPredictionAdapter extends RecyclerView.Adapter<StatusPredictionAdapter.ViewHolder> {

    private final Context context;
    private List<ContactEntity> contacts;
    private List<String> predictedTypes;
    private List<String> predictedTimes;

    public StatusPredictionAdapter(Context context, List<ContactEntity> contacts,
                                   List<String> predictedTypes, List<String> predictedTimes) {
        this.context = context;
        this.contacts = contacts;
        this.predictedTypes = predictedTypes;
        this.predictedTimes = predictedTimes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_prediction, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactEntity contact = contacts.get(position);
        holder.contactName.setText(contact.name);
        holder.predictedType.setText(predictedTypes.get(position));
        holder.predictionTime.setText(predictedTimes.get(position));

        Glide.with(context)
                .load(contact.profilePicPath)
                .placeholder(R.drawable.ic_person)
                .into(holder.contactImage);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void updateData(List<ContactEntity> contacts, List<String> types, List<String> times) {
        this.contacts = contacts;
        this.predictedTypes = types;
        this.predictedTimes = times;
        notifyDataSetChanged();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView contactImage;
        TextView contactName, predictedType, predictionTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            contactImage = itemView.findViewById(R.id.contactImage);
            contactName = itemView.findViewById(R.id.contactName);
            predictedType = itemView.findViewById(R.id.predictedType);
            predictionTime = itemView.findViewById(R.id.predictionTime);
        }
    }
}
