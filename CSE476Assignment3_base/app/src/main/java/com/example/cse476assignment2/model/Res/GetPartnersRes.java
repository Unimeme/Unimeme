package com.example.cse476assignment2.model.Res;

import java.util.List;

public class GetPartnersRes {
    public List<PartnerItem> Partners;

    public static class PartnerItem {
        public int user_id;
        public String username;
    }
}