package com.example.android.networkusage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Xml;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.example.android.networkusage.photoList.Entry;

public class flickrSync extends IntentService {
    private final String APIKEY="211f5919e44a6090d50ee2e0581fe252";
    
    private final String ns = null;
    
	public flickrSync() {
		super("flickrSync");
		// TODO Auto-generated constructor stub
	}

	@Override
    protected void onHandleIntent(Intent intentToSyncFlickr) {
        // Gets data from the incoming Intent
        
		ArrayList<String> photoSets = (ArrayList<String>)intentToSyncFlickr.getSerializableExtra("PhotoSets");
		
		for (String photoSet : photoSets)	{
			String URL = "https://api.flickr.com/services/rest/?method=flickr.photosets.getPhotos&api_key="+APIKEY+"&photoset_id="+photoSet;
			try {
	   		 	syncPhotoSet(URL);
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
	
	public void syncPhotoSet(String urlString) throws XmlPullParserException, IOException {
        StringRequest request = new StringRequest(Request.Method.GET, urlString, 
        	    new Response.Listener<String>() 
        	    {
        	        @Override
        	        public void onResponse(String response) {
        	        	List<Entry> entries;
						
        	        	try {
							entries = parse(response);
							photoList.getInstance(getApplicationContext()).setEntries(entries);
							syncMediaDir();
							
						} catch (XmlPullParserException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
        	        	
        	        }
        	    }, 
        	    new Response.ErrorListener() 
        	    {
        	         @Override
        	         public void onErrorResponse(VolleyError error) {                                
        	         // handle error response
        	       }
        	    }
        	);
        volleySingleton.getInstance(this.getApplicationContext()).getRequestQueue().add(request);
    }
    
    private void grabImgWVolley(Entry entry)
    {
    	
    	final String title=entry.title;
    	final String url="https://farm"+entry.farm+".staticflickr.com/"+entry.server+"/"+title;
    	
    	ImageRequest ir = new ImageRequest(url, 
         		new Response.Listener<Bitmap>() {
 		            @Override
 		            public void onResponse(Bitmap response) {
 		                try{
 			                OutputStream fOut = null;
 			                File file = new File(getAlbumStorageDir("myPics"), title);
 			                if(!file.exists()){
 				                fOut = new FileOutputStream(file);
 				
 				                response.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
 				                fOut.flush();
 				                fOut.close();
 				
 				               String imgURI = MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
 				               Log.w("tag","image download success. URL: "+imgURI);
 			                }
 			                else
 			                	Log.w("tag","Image exists.  Skipping.");
 			            }
 		                catch(Exception e)
 		                {
 		                	
 		                }
 		            }
         		}
         , 0, 0, null,   
         new Response.ErrorListener() {
         	public void onErrorResponse(VolleyError error) {
         		//Log.w("tag", "Image Load Error: " + error.getMessage());
         	}
         }
         );
        volleySingleton.getInstance(this).getRequestQueue().add(ir);
    }  
    
    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        file.mkdirs();
        return file;
    }
    
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
    
    public void syncMediaDir(){
    	//Grab our photos, as needed
    	List<Entry> photos = photoList.getInstance(this).getEntries();
    	List<File> photoFiles = new ArrayList<File>();
    	for (Entry entry : photos) {
    		grabImgWVolley(entry);
    		photoFiles.add(new File(getAlbumStorageDir("myPics"), entry.title));
    	}

    	List<File> fileList = new ArrayList<File>(Arrays.asList(getAlbumStorageDir("myPics").listFiles())); //new ArrayList is only needed if you absolutely need an ArrayList
    	fileList.removeAll(photoFiles);
		
        for (File file : fileList)	{
			Log.w("delete","Want to remove "+file.toString());
			file.delete();
			//getContentResolver().delete(Uri.fromFile(file), null, null);
    	}
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()))); 
    	//needSync = false;
    }
    
    public List<Entry> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }
    
    public List<Entry> parse(String xmlString) throws XmlPullParserException, IOException {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(xmlString));
            parser.nextTag();
            return readFeed(parser);  
    }

    private List<Entry> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Entry> entries = new ArrayList<Entry>();
        Log.w("tag" , "Entered readFeed");

        parser.require(XmlPullParser.START_TAG, ns, "rsp");
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            Log.w("tag", "Looking for photo tag");
            if (name.equals("photo")) {
            	Log.w("tag" , "Found photo.  About to call readEntry");
            	entries.add(readEntry(parser));
            } else {
            	Log.w("tag", "Found "+name);
            }
            Log.w("tag", "Next entry");
        }
        Log.w("tag", "All entries read");
        
        return entries;
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
    // off
    // to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
    private Entry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
    	Log.w("tag" , "readEntry entered");
    	parser.require(XmlPullParser.START_TAG, ns, "photo");
        String id = null;
        String secret = null;
        String server = null;
        String farm = null;
        id = parser.getAttributeValue(ns, "id");
        secret = parser.getAttributeValue(ns, "secret");
        server = parser.getAttributeValue(ns, "server");
        farm = parser.getAttributeValue(ns, "farm"); 
        Log.w("tag" , "readEntry exit");
     
        return new Entry(id, secret, server, farm);   
    }

    
   
}