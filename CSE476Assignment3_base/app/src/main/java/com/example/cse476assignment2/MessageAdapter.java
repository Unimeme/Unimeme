package com.example.cse476assignment2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cse476assignment2.R;
import com.example.cse476assignment2.model.Res.GetThreadRes;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    private List<GetThreadRes.MessageItem> list;
    private long myUserId;

    public MessageAdapter(List<GetThreadRes.MessageItem> list, long myUserId) {
        this.list = list;
        this.myUserId = myUserId;
    }

    public void update(List<GetThreadRes.MessageItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        GetThreadRes.MessageItem m = list.get(position);
        return (m.sender_id == myUserId) ? TYPE_ME : TYPE_OTHER;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_ME) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_me, parent, false);
            return new MeHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_other, parent, false);
            return new OtherHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        GetThreadRes.MessageItem m = list.get(pos);

        if (h instanceof MeHolder) {
            ((MeHolder) h).text.setText(m.content);
        } else {
            ((OtherHolder) h).text.setText(m.content);
        }
    }

    static class MeHolder extends RecyclerView.ViewHolder {
        TextView text;
        MeHolder(View v) {
            super(v);
            text = v.findViewById(R.id.txtMessageMe);
        }
    }

    static class OtherHolder extends RecyclerView.ViewHolder {
        TextView text;
        OtherHolder(View v) {
            super(v);
            text = v.findViewById(R.id.txtMessageOther);
        }
    }
}
