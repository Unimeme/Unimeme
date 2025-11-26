package com.example.cse476assignment2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cse476assignment2.R;
import com.example.cse476assignment2.model.Res.GetPartnersRes;

import java.util.List;

public class PartnersAdapter extends RecyclerView.Adapter<PartnersAdapter.Holder> {

    public interface OnPartnerClick {
        void onClick(GetPartnersRes.PartnerItem partner);
    }

    private List<GetPartnersRes.PartnerItem> list;
    private final OnPartnerClick listener;

    public PartnersAdapter(List<GetPartnersRes.PartnerItem> list, OnPartnerClick listener) {
        this.list = list;
        this.listener = listener;
    }

    public void update(List<GetPartnersRes.PartnerItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_partner, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        GetPartnersRes.PartnerItem item = list.get(position);

        holder.txtName.setText(item.username);

        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView txtName;

        Holder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtPartnerName);
        }
    }
}
