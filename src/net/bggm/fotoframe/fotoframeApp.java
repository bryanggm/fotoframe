package net.bggm.fotoframe;


import net.bggm.fotoframe.flickr.flickrPhotoList;
import net.bggm.fotoframe.util.fileManager;
import net.bggm.fotoframe.util.volleySingleton;

import android.app.Application;


public class fotoframeApp extends Application {
	//private Context myContext;

	public static final String PREFS_NAME = "fotoframePrefs";
	
    @Override
    public void onCreate() {
        super.onCreate();
        initSingletons(); 
    }
    
    protected void initSingletons(){
    	volleySingleton.getInstance(this.getApplicationContext()).getRequestQueue().start();
       	flickrPhotoList.getInstance(this);
		fileManager.getInstance(this).updateFilePaths();
    }      
}