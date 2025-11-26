package com.example.cse476assignment2.model.Res;

import java.util.List;

public class GetThreadRes {
    public List<MessageItem> Messages;

    public static class MessageItem {
        public int message_id;
        public int sender_id;
        public int receiver_id;
        public String content;
        public String created_at;
    }
}