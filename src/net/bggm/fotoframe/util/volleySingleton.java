package net.bggm.fotoframe.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

public class volleySingleton {
 
    private static volleySingleton instance;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
 
    private volleySingleton(Context context) {
        requestQueue = Volley.newRequestQueue(context);
 
        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);
 
 
            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }
 
            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });
    }
 
 
    public static volleySingleton getInstance(Context context) {
        if (instance == null) {
            instance = new volleySingleton(context);
        }
        return instance;
    }
 
    public RequestQueue getRequestQueue() {
        return requestQueue;
    }
 
    public ImageLoader getImageLoader() {
        return imageLoader;
    }


	
}