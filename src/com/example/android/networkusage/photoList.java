package com.example.android.networkusage;

import java.util.List;

import android.content.Context;

public class photoList {
	
	private static photoList instance;
	private List<Entry> entries;
	private Context myContext;
 
    private photoList(Context context) {
        myContext = context;
    }
 
    public static photoList getInstance(Context context) {
        if (instance == null) {
            instance = new photoList(context);
        }
        return instance;
    }
 
    public List<Entry> getEntries() {
        return entries;
    }
    
    public void setEntries(List<Entry> in) {
         entries = in;
    }
    
    // This class represents a single flickr photo.
    public static class Entry {
        public final String id;
        public final String secret;
        public final String server;
        public final String farm;
        public final String title;

        public Entry(String id, String secret, String server, String farm) {
            this.id = id;
            this.secret = secret;
            this.server = server;
            this.farm = farm;
            this.title=id+"_"+secret+"_b.jpg";   
        }
    }
}
