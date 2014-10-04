package net.bggm.fotoframe.flickr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import net.bggm.fotoframe.volleySingleton;
import net.bggm.fotoframe.flickr.flickrPhotoList.flickrPhoto;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Xml;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;


public class flickrSyncService extends Service {
    private final String APIKEY="211f5919e44a6090d50ee2e0581fe252";
    private Messenger uiMessenger;
    private final String ns = null;
    ArrayList<String> photoSets = new ArrayList<String>();
    private boolean syncDisk = false;
    private boolean syncUI = false;
    private String newImg;
    private String formatPref = "b";
    private int maxResX = 1024;
    private int maxResY = 800;
    public static final String SYNC_AFTER= "syncAfterThis";
    private String finalPhotoSet = null;
    private boolean processing=false;
    boolean[] syncList;
    
	public flickrSyncService() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	private Looper flickrLooper;
	private ServiceHandler flickrHandler;

	@Override
	public void onCreate() {
	    // Start up the thread running the service.  Note that we create a
	    // separate thread because the service normally runs in the process's
	    // main thread, which we don't want to block.  We also make it
	    // background priority so CPU-intensive work will not disrupt our UI.
	    HandlerThread thread = new HandlerThread("flickrSyncThread", Process.THREAD_PRIORITY_BACKGROUND);
	    thread.start();
	
	    // Get the HandlerThread's Looper and use it for our Handler
	    flickrLooper = thread.getLooper();
	    flickrHandler = new ServiceHandler(flickrLooper);
	    
	    startSyncTimer();
	    
	}

	private void startSyncTimer() {
		//Create a timer thread to request sync every X ms
		    new Thread(new Runnable(){
		        public void run() {
			        // TODO Auto-generated method stub
			        while(true)
			        {
			        	try {
				            Thread.sleep(10000);
				            Message msg = flickrHandler.obtainMessage(); //this gets a new message (more efficient than instantiation)
				  	      	msg.obj = photoSets;
				            flickrHandler.sendMessage(msg);
				        } catch (InterruptedException e) {
				            // TODO Auto-generated catch block
				            e.printStackTrace();
				        } 
			        }
		        }
		    }).start();
	}

	@Override
	public int onStartCommand(Intent intentToSyncFlickr, int flags, int startId) {
	     // Toast.makeText(this, "flickrSync service starting", Toast.LENGTH_SHORT).show();
	      // For each start request, send a message to start a job and deliver the
	      // start ID so we know which request we're stopping when we finish the job
	      Message msg = flickrHandler.obtainMessage(); //this gets a new message (more efficient than instantiation)

	      try{
		      msg.obj = intentToSyncFlickr.getSerializableExtra("PhotoSets");
		      flickrHandler.sendMessage(msg);
		      uiMessenger = intentToSyncFlickr.getParcelableExtra("uiMessenger");
	      }
	      catch(Exception e){
	      	e.printStackTrace();
	      }

	      // If we get killed, after returning from here, restart
	      return START_STICKY;
	  }

	@Override
	public IBinder onBind(Intent intent) {
	    // We don't provide binding, so return null
	    return null;
	}

	@Override
	public void onDestroy() {
	 // Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
	}
	  
	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
		boolean running=false;
		public ServiceHandler(Looper looper) {
	          super(looper);
	    }
		
	    @SuppressWarnings("unchecked")
		@Override
	    public void handleMessage(Message msg) {
	    	if(!processing){
	    		processing=true;
		    	photoSets = (ArrayList<String>)msg.obj;
		    	//Store our final photo set for sync purposes
		    	//finalPhotoSet = photoSets.get(photoSets.size()-1);
		    	syncList = new boolean[photoSets.size()];
		    	//Clear out our current photo list
		    	flickrPhotoList.getInstance(getApplicationContext()).setPhotos(new ArrayList<flickrPhoto>());
		    	//For each photo set, grab necessary info
		    	for (String photoSet : photoSets)	{
					String URL = "https://api.flickr.com/services/rest/?method=flickr.photosets.getPhotos&api_key="+APIKEY+"&photoset_id="+photoSet;
					try {
			   		 	grabPhotoSet(URL, false);
			   		 	//if(syncDisk)
			   		 		//syncMediaDir();
					} catch (XmlPullParserException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	}  
		    	processing=false;
	    	}
	    	
	    }
	}
	
	public void handlePhotoSetComplete(String photoSetId){
		int index = photoSets.indexOf(photoSetId);
		Log.w("flickrSyncService handlePhotoSetComplete", "PhotoSet synced.  ID was "+photoSetId);
		if(index<syncList.length)
			syncList[index]=true;
		
		boolean done = true;
		for (boolean i : syncList){
			done = i&&done;
		}

		if(done){
			Log.w("flickrSyncService handlePhotoSetComplete", "All IDs done. Calling a media sync.");
			syncMediaDir();
		}
		
	}
	
	public void grabPhotoSet(String urlString, boolean sync) throws XmlPullParserException, IOException {
        StringRequest request = new StringRequest(Request.Method.GET, urlString,
        	    new Response.Listener<String>() 
        	    {
        	        @Override
        	        public void onResponse(String response) {
        	        	try {
        	        		List<flickrPhoto> photos = new ArrayList<flickrPhoto>();
        	        		String id = null;
        	                XmlPullParser parser = Xml.newPullParser();
        	                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        	                parser.setInput(new StringReader(response));
        	                parser.nextTag();
        	                parser.require(XmlPullParser.START_TAG, ns, "rsp");
        	                while (parser.next() != XmlPullParser.END_DOCUMENT) {
        	                    if (parser.getEventType() != XmlPullParser.START_TAG) 
        	                        continue;
        	                    
        	                    String name = parser.getName();
        	                    // Starts by looking for the flickrPhoto tag
        	                    //Log.w("tag", "Looking for photo tag");
        	                    
        	                    if (name.equals("photo")) {
        	                    	Log.w("tag" , "Found photo.  About to call readflickrPhoto");
        	                    	photos.add(readPhotoTag(parser));
        	                    	//addPhotoToList(readPhotoTag(parser).id);
        	                    	flickrPhotoList.getInstance(getApplicationContext()).addPhotoSet(photos);
        	                    	//Since we added our photos, sync our directory TODO: make this a handler call to the service?

        	                    } else 
        	                    	if (name.equals("photoset")){  //TODO make this more elegant
            	                    	id = parser.getAttributeValue(ns, "id");
            	                    }
            	                    else{
        	                    	Log.w("tag", "Found "+name);
            	                    }
        	                    Log.w("tag", "Next flickrPhoto");
        	                }
        	                Log.w("flickrSyncService grabPhotoSet", "Photoset processed. Total photos: "+photos.size());
	                    	//syncMediaDir();
        	                //syncDisk = true;
        	                handlePhotoSetComplete(id);
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
       
        if(sync)
        	request.setTag(SYNC_AFTER);
        volleySingleton.getInstance(this.getApplicationContext()).getRequestQueue().add(request);
        //volleySingleton.getInstance(this.getApplicationContext()).getRequestQueue().getCache().
    }
    
    private void grabImgWVolley(flickrPhoto flickrPhoto)
    {
    	//Log.w("flickrSyncService","Entering grab image w volley function");
    	//Check and see if we're trying to grab an original photo with incomplete information
    	if(formatPref=="o" && (flickrPhoto.osecret==null || flickrPhoto.oformat==null)){
	    	try{
	    		grabPhotoByGetInfo(flickrPhoto.id);
	    	} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
	    	} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	//Exit the function, grabPhotoByGetInfo should call us again with complete information;
	    	return;
    	}
    			
    	final String title=flickrPhoto.getTitle(formatPref);
    	final String url="https://farm"+flickrPhoto.farm+".staticflickr.com/"+flickrPhoto.server+"/"+title;
    	File file = new File(getAlbumStorageDir("myPics"), title);
        //If the file already exists, exit the function
    	if(file.exists())
        	return;
    	
    	//Otherwise, use ImageRequest to grab our image
    	ImageRequest ir = new ImageRequest(url, 
         		new Response.Listener<Bitmap>() {
 		            @Override
 		            public void onResponse(Bitmap response) {
 		                try{
 		                	//Log.w("volleySingleton","Entering volley image request");
        	        		if(formatPref=="o")
        	        		{
        	        			Log.w("volleySingleton","Resizing img to maxResX and Y");
        	        			response = resizeBitmap(response,maxResX,maxResY);
        	        		}
 		                	
 			                OutputStream fOut = null;
 			                File file = new File(getAlbumStorageDir("myPics"), title);
 			                if(!file.exists()){
 				                fOut = new FileOutputStream(file);
 				                response.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
 				                fOut.flush();
 				                fOut.close();
 		 
	 				            MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
	 				            // Log.w("volleySingleton","image download success. URL: "+imgURI);
	 				            newImg = title;
	 				            syncUI = true;
 			                }
 			            }
 		                catch(Exception e)
 		                {
 		                	e.printStackTrace();
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
    
    public void syncMediaDir(){
	
    	Log.w("flickrSyncService syncMediaDir","Syncing the media directory...");
    	//Grab our photos, as needed
    	List<flickrPhoto> photos = flickrPhotoList.getInstance(this).getPhotos();
    	List<File> photoFiles = new ArrayList<File>();
    	for (flickrPhoto flickrPhoto : photos) {
    		grabImgWVolley(flickrPhoto);
    		photoFiles.add(new File(getAlbumStorageDir("myPics"), flickrPhoto.getTitle(formatPref)));
    	}

    	List<File> fileList = new ArrayList<File>(Arrays.asList(getAlbumStorageDir("myPics").listFiles())); //new ArrayList is only needed if you absolutely need an ArrayList
    	fileList.removeAll(photoFiles);
    	
    	if(fileList.size()>0){
    		syncUI = true;
	        for (File file : fileList)	{
				Log.w("delete","Want to remove "+file.toString());
				file.delete();
				//getContentResolver().delete(Uri.fromFile(file), null, null);
	    	}
    	}
        
        
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()))); 
    	if(syncUI)
    	{
    		sendUISyncMsg();
    		syncUI=false;
    	}

	
    }
    
    private void sendUISyncMsg(){
    	Message imgToDisplay = Message.obtain();
		Log.w("flickrSyncService+sendUISyncMsg","Need to sync files in UI thread.  Calling our handler");
		if(newImg != null && newImg.length() != 0)
			imgToDisplay.obj = newImg;
		try{
			uiMessenger.send(imgToDisplay);
		}
		catch(Exception e)
		{ ///We really should do something here
			
		}
    }
    
    // Parses the contents of an flickrPhoto. 
    private flickrPhoto readPhotoTag(XmlPullParser parser) throws XmlPullParserException, IOException {
    //	Log.w("tag" , "readflickrPhoto entered");
    	parser.require(XmlPullParser.START_TAG, ns, "photo");
        String id = parser.getAttributeValue(ns, "id");
        String secret = parser.getAttributeValue(ns, "secret");
        String server = parser.getAttributeValue(ns, "server");
        String farm = parser.getAttributeValue(ns, "farm");
        String osecret = parser.getAttributeValue(ns, "originalsecret");
        String oformat = parser.getAttributeValue(ns, "originalformat");
        //  Log.w("Attr",id+osecret+oformat);
        //Log.w("tag" , "readflickrPhoto exit");
     
        return new flickrPhoto(id, secret, server, farm, osecret, oformat);   
    }
    
    public void grabPhotoByGetInfo(String photoID) throws XmlPullParserException, IOException {
    	String URL = "https://api.flickr.com/services/rest/?method=flickr.photos.getInfo&api_key="+APIKEY+"&photo_id="+photoID;
        StringRequest request = new StringRequest(Request.Method.GET, URL, 
        	    new Response.Listener<String>() 
        	    {
        	        @Override
        	        public void onResponse(String response) {
        	        	flickrPhoto parsedPhoto = null;
						
        	        	try {
        	        		parsedPhoto = parsePhotoGetInfo(response);
        	        		//Log.w("Attr","Caught new photo.  Original format is "+newPhoto.oformat);
        	        		//Update our flickrPhotoList with the new information
        	        		flickrPhotoList.getInstance(getApplicationContext()).addPhoto(parsedPhoto);
        	        		//Grab the photo with Volley
        	        		grabImgWVolley(parsedPhoto);
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
    //TODO Cleanup
    public flickrPhoto parsePhotoGetInfo(String xmlString) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(new StringReader(xmlString));
        parser.nextTag();
        flickrPhoto newPhoto=null;
        //List<flickrPhoto> entries = new ArrayList<flickrPhoto>();
       // Log.w("tag" , "Entered readFeed");

        parser.require(XmlPullParser.START_TAG, ns, "rsp");
        parser.next();
        
        if (parser.next() != XmlPullParser.END_DOCUMENT && parser.getName().equals("photo")) {
        	Log.w("tag", "Photo Info Found "+parser.getName());
        	newPhoto = readPhotoTag(parser);
        }
        return newPhoto;
    }
    
    private Bitmap resizeBitmap(Bitmap in, int desiredHeight, int desiredWidth) { 	 
    	int width = in.getWidth();
    	int height = in.getHeight();
    	float scaleWidth = ((float) desiredWidth) / width;
    	float scaleHeight = ((float) desiredHeight) / height;
    	 
    	// CREATE A MATRIX FOR THE MANIPULATION
    	Matrix matrix = new Matrix();
    	 
    	// RESIZE THE BIT MAP
    	matrix.postScale(scaleWidth, scaleHeight);
    	 
    	// RECREATE THE NEW BITMAP
    	Bitmap resizedBitmap = Bitmap.createBitmap(in, 0, 0, width, height, matrix, false);
    	
    	return resizedBitmap;
    	 
    	}
}