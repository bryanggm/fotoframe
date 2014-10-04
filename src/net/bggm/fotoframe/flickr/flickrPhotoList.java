package net.bggm.fotoframe.flickr;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class flickrPhotoList {
	
	private static flickrPhotoList instance;
	private List<flickrPhoto> photos;
	//private Context myContext;
 
    private flickrPhotoList(Context context) {
      //  myContext = context;
    	photos = new ArrayList<flickrPhoto>();
    }
 
    public static flickrPhotoList getInstance(Context context) {
        if (instance == null) {
            instance = new flickrPhotoList(context);
        }
        return instance;
    }
 
    public List<flickrPhoto> getPhotos() {
        return photos;
    }
    
    public void setPhotos(List<flickrPhoto> in) {
         photos = in;
    }
    
    public void addPhotoSet(List<flickrPhoto> in){
    	for(flickrPhoto photo : in){
    		addPhoto(photo);
    	}
    }
    
    public void addPhoto(flickrPhoto in){
    	int index = findPhotoById(in.id);
    	if(index<0)
    		photos.add(in);
    	else
    		photos.set(index, in);
    }
    
    private int findPhotoById(String IdToFind){
    	for (int i = 0; i < photos.size(); i++)
        {
    		flickrPhoto photo = photos.get(i);
            if (photo.id.equals(IdToFind))
                return i;
        } 
        return -1;
    }
    
    // This class represents a single flickr photo.
    public static class flickrPhoto {
        public final String id;
        public final String secret;
        public final String server;
        public final String farm;
        //public final String title;
        public final String osecret;
        public final String oformat;

        public flickrPhoto(String id, String secret, String server, String farm, String osecret, String oformat) {
        	this.id=id;
        	this.secret=secret;
        	this.server=server;
        	this.farm=farm;
        	this.osecret=osecret;
        	this.oformat=oformat;
        }
        
        public String getTitle(String format)
        {
        	if(format=="o")
        		{
        			Log.w("URLS", "https://farm"+farm+".staticflickr.com/"+server+"/"+id+"_"+osecret+"_o."+oformat);
        			return id+"_"+osecret+"_o."+oformat;
        		}
        	else
        		return id+"_"+secret+"_"+format+".jpg";
        	
        }
    }
}
