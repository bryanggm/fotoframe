package net.bggm.fotoframe;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.bggm.fotoframe.flickr.flickrSyncService;

import com.example.android.networkusage.R;
import com.example.android.networkusage.R.anim;
import com.example.android.networkusage.R.drawable;
import com.example.android.networkusage.R.id;
import com.example.android.networkusage.R.layout;




import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class ImgFlipperActivity extends Activity {

	private ViewFlipper gallery;
//	private TextView txtPages;
	private ImageButton playButton;
	private LinearLayout utilityBar;
	private float fromPosition;
	private int count;
	private List<File> viewables = new ArrayList<File>();
	private LayoutInflater inflater = null;
	private int viewableCount;
	private int displayFile;
	private flipperThread flipper;
	private ArrayList<String> flickrPhotoSets;
	private Intent intentToSyncFlickr;
	private int fliptime=3000;
	
	Runnable hideUtilityBar;
	Handler utilityBarHandler=new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		View decorView = getWindow().getDecorView();

		// Hide the status bar.
		int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
		decorView.setSystemUiVisibility(uiOptions);
		ActionBar actionBar = getActionBar();
		actionBar.hide();
		
		//Set up our photo sets
		flickrPhotoSets = new ArrayList<String>();
        flickrPhotoSets.add("72157647023347908");
        flickrPhotoSets.add("72157644442295196");
        
        //Start flickrSyncService
        intentToSyncFlickr= new Intent(this, flickrSyncService.class);
        intentToSyncFlickr.putExtra("PhotoSets", (Serializable)flickrPhotoSets);
        Messenger uiMessenger = new Messenger(syncHandler);
        intentToSyncFlickr.putExtra("uiMessenger", uiMessenger);
        startService(intentToSyncFlickr);
		int fliptime=3000;
		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		

		hideUtilityBar=new Runnable() {
		            @Override
		            public void run() {	                
		                utilityBar.setVisibility(View.GONE); //This will remove the View. and free s the space occupied by the View    
		            }
		        };
		
		
		//this.txtPages = (TextView)findViewById(R.id.txtPages);
		this.playButton = (ImageButton)findViewById(R.id.playButton);
		this.utilityBar = (LinearLayout)findViewById(R.id.utility_bar);
		this.gallery = (ViewFlipper)findViewById(R.id.gallery);		
		this.gallery.setOnTouchListener(new OnTouchListener() {	
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				utilityBar.setVisibility(View.VISIBLE);
				utilityBarHandler.removeCallbacks(hideUtilityBar);
				utilityBarHandler.postDelayed(hideUtilityBar, 3000);
				switch (event.getAction())
		        {
		        case MotionEvent.ACTION_DOWN:
		            fromPosition = event.getX();
		            break;
		        case MotionEvent.ACTION_UP:
		            float toPosition = event.getX();
		            if (fromPosition > toPosition + 20)
		            {
		            	stopFlipping();
		            	next();
		            	return true;
		            }
		            else if (fromPosition < toPosition - 20)
		            {
		            	stopFlipping();
				        previous();
		            	return true;
		            } 
		        default:
		            break;
		        }
		        return true;
			}
		});
		
		flipper = new flipperThread(flipHandler);
		flipper.start();
        
		syncFiles();
		if( savedInstanceState != null ) {
			Log.w("ImgFlipper", "Reloading instance");
			gallery.addView(addImage(loadImage(savedInstanceState.getInt("count"))));
			}
		else
			gallery.addView(addImage(loadImage(0)));
		utilityBar.setVisibility(View.GONE);
		
		updateTextView();

	}
	
	static class SyncHandler extends Handler {
	    private final WeakReference<ImgFlipperActivity> myActivity; 

	    SyncHandler(ImgFlipperActivity activity) {
	    	myActivity = new WeakReference<ImgFlipperActivity>(activity);
	    }
	    @Override
	    public void handleMessage(Message msg)
	    {
	    	ImgFlipperActivity activity = myActivity.get();
	         if (activity != null) {
	        	 activity.handleSyncMessage(msg);
	         }
	    }
	}
	
	public void handleSyncMessage(Message msg) {
    	syncFiles();	    		
    	if(msg.obj != null){
    		displayFile = viewables.indexOf(new File(getAlbumStorageDir("myPics"), (String)msg.obj));
    		goToImg(displayFile);
    	}
    }
	
	static class FlipHandler extends Handler {
	    private final WeakReference<ImgFlipperActivity> myActivity; 

	    FlipHandler (ImgFlipperActivity activity) {
	    	myActivity = new WeakReference<ImgFlipperActivity>(activity);
	    }
	    @Override
	    public void handleMessage(Message msg)
	    {
	    	ImgFlipperActivity activity = myActivity.get();
	         if (activity != null) {
	        	 activity.handleFlipMessage(msg);
	         }
	    }
	}
	
	public void handleFlipMessage(Message msg) {
		next();
    }
	
	private final SyncHandler syncHandler = new SyncHandler(this);
	
	private final FlipHandler flipHandler = new FlipHandler(this);


	
	public void syncPhotos()
    {
    	startService(intentToSyncFlickr);
    }
	
	private void syncFiles()
	{
		viewables = new ArrayList<File>();
		try {
			//TODO see if we can get rid of this casting
			viewables = Arrays.asList(getAlbumStorageDir("myPics").listFiles());
			viewableCount = viewables.size();
			//If this pathname does not denote a directory, then listFiles() returns null. 
			if(viewableCount>1)
				flipImages(fliptime);
			else
				flipper.interrupt();
			Log.w("imgFlipper","Files found: "+String.valueOf(viewableCount));
			updateTextView();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void next()
    {
		if (count >= viewables.size() - 1)
			count = -1;
		
		count++;
		addNextImage(count, false);
    	gallery.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.go_next_in));
        gallery.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.go_next_out));
        gallery.showNext();   
        removeImages();	
        updateTextView();
    }
	
	public void previous()
    {
		if (count <= 0)
			count = viewables.size();
		
		count--;
		addNextImage(count, true);
    	gallery.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.go_prev_in));
        gallery.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.go_prev_out));
        gallery.showNext();
        removeImages();	
        updateTextView();
    }
	
	public void goToImg(int dest)
    {
		count = dest;
		addNextImage(count, true);
    	gallery.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.go_next_in));
        gallery.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.go_next_out));
        gallery.showNext();
        removeImages();	
        updateTextView();
    }
	
	private void updateTextView()
	{
		//String pages = String.format(getString(R.string.str_pages), (count + 1), viewables.size());
		//Log.w("img","Setting text to: "+String.valueOf(viewables.size()));
	//	txtPages.setText(pages);
	}
	
	private void addNextImage(int position, boolean isLeft)
	{		
		if (isLeft)
		{
			if (position >= 0)
			{
				gallery.addView(addImage(loadImage(position)));
			}
		} else 
		{
			if (position < viewables.size())
				gallery.addView(addImage(loadImage(position)));
		}
	}
	
	private void removeImages()
	{
		if (gallery.getChildCount() > 2)
		{
			gallery.removeViewAt(0);
			System.gc();
		}
	}
	
	private View addImage(Bitmap bitmap)
	{
		ImageView view = (ImageView)inflater.inflate(R.layout.gallery_item, null);
		view.setImageBitmap(bitmap);
		return view;
	}
	
	public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        file.mkdirs();
        return file;
    }

	private Bitmap loadImage(int index)
	{
		try{
			FileInputStream fileInputStream;
			Bitmap myBitmap;
			fileInputStream = new FileInputStream(viewables.get(index));
			myBitmap = BitmapFactory.decodeStream(fileInputStream);
			fileInputStream.close();
			return myBitmap;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void flipImages(int interval){
		//Create a timer thread to request sync every X ms
		//playButton.setText("Pause");
		playButton.setImageResource(R.drawable.ic_media_pause);
		flipper.setInterval(interval);
		if(!flipper.isRunning())
			flipper.begin();
	}
	private void stopFlipping()
	{
		flipper.interrupt();
		//playButton.setText("Play");
		playButton.setImageResource(R.drawable.ic_media_play);
	}

    class flipperThread extends Thread {
        private int interval;
       // private boolean interrupt=false;
        private boolean running=false;
        private Handler flipHandle;

        public flipperThread(Handler flipHandle) {
            this.flipHandle = flipHandle;
        }

        @Override
        public void run() {
        	while(true){
	        	try {
	        		if(running){
	        			flipHandle.sendEmptyMessage(0);
	        			Thread.sleep(interval);
	        		}
	        		
		        } catch (InterruptedException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		        } 
        	}
        }
        
        public void interrupt()
        {
        	running = false;
        }
        public void begin(){
        	running = true;
        }
        
        public boolean isRunning(){
        	if(running)
        		return true;
        	else
        		return false;
        }
        
        public void setInterval(int interval)
        {
        	this.interval = interval;
        }
    }
    
    /** Called when the user touches the button */
    public void clickPlayButton(View view) {
    	if(flipper.isRunning()){
    		stopFlipping();
    	}
    	else{
    		flipImages(fliptime);
    	}
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
       super.onSaveInstanceState(outState);
       outState.putInt("count", count);
    }
}
