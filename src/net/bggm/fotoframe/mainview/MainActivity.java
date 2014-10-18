 package net.bggm.fotoframe.mainview;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import net.bggm.fotoframe.flickr.flickrSyncService;
import net.bggm.fotoframe.util.fileManager;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.example.android.networkusage.R;

public class MainActivity extends FragmentActivity{

	private LoopedViewPagerAdapter adapter;
	private LoopedViewPager viewPager;
	private ArrayList<String> flickrPhotoSets;
	private Intent intentToSyncFlickr;
	private final SyncHandler syncHandler = new SyncHandler(this);
	private final FlipHandler flipHandler = new FlipHandler(this);
	private flipperThread flipper;
	private LinearLayout utilityBar;
	private ImageButton playButton;
	private Runnable hideUtilityBar;
	private Handler utilityBarHandler=new Handler();
	private int fliptime = 10000;
	private float fromPosition;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);

		adapter = new LoopedViewPagerAdapter(getSupportFragmentManager(), MainActivity.this);
        viewPager = (LoopedViewPager)findViewById(R.id.pager);
		viewPager.setAdapter(adapter);
		
		syncFlickr();
		
		// Hide the status bar.
		View decorView = getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
		decorView.setSystemUiVisibility(uiOptions);
		ActionBar actionBar = getActionBar();
		if(actionBar!=null)
			actionBar.hide();
		
		//Utility Bar
		this.utilityBar = (LinearLayout)findViewById(R.id.utility_bar);
		hideUtilityBar=new Runnable() {
            @Override
            public void run() {	                
                utilityBar.setVisibility(View.GONE); //This will remove the View. and free s the space occupied by the View    
            }
        };
		utilityBar.setVisibility(View.GONE);
		
		//PlayButton
		this.playButton = (ImageButton)findViewById(R.id.playButton);

		Intent i = getIntent();
		int position = i.getIntExtra("position", 0);
		viewPager.setCurrentItem(position);
		
		flipper = new flipperThread(flipHandler);
		flipper.start();
		startFlipping(fliptime);
	}
	
	   /** Called when the user touches the button */
    public void clickPlayButton(View view) {
    	if(flipper.isRunning()){
    		stopFlipping();
    	}
    	else{
    		startFlipping(fliptime);
    		playButton.setImageResource(R.drawable.ic_media_pause);
    	}
    }
    
    public void passTouchEvent(MotionEvent event) {
 		viewPager.setFixedScrolling(false);
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
            if (fromPosition > toPosition + 50)
            {
            	stopFlipping();  //TODO Change to message based protocol?
            }
            else if (fromPosition < toPosition - 50)
            {
            	stopFlipping();
            } 
        default:
            break;
        }
    	
    }
    
    static class SyncHandler extends Handler {
	    private final WeakReference<MainActivity> myActivity; 

	    SyncHandler(MainActivity activity) {
	    	myActivity = new WeakReference<MainActivity>(activity);
	    }
	    @Override
	    public void handleMessage(Message msg)
	    {
	    	MainActivity activity = myActivity.get();
	         if (activity != null) {
	        	 activity.handleSyncMessage(msg);
	         }
	    }
	}
	
	public void handleSyncMessage(Message msg) {  
		fileManager.getInstance(this).updateFilePaths();
		viewPager.getAdapter().notifyDataSetChanged();
		
    	if(msg.obj != null){
    	    int index = fileManager.getInstance(this).getIndexByPath((String)msg.obj);
    		viewPager.setCurrentItem(index);
    		
    	}
    }
    
    public void syncFlickr(){
		if(flickrPhotoSets==null){
			//Set up our photo sets
			flickrPhotoSets = new ArrayList<String>();
	        flickrPhotoSets.add("72157647023347908");
	        flickrPhotoSets.add("72157644442295196");
		}
        
		if(intentToSyncFlickr==null){
			intentToSyncFlickr= new Intent(this, flickrSyncService.class);
			intentToSyncFlickr.putExtra("PhotoSets", (Serializable)flickrPhotoSets);
			Messenger uiMessenger = new Messenger(syncHandler);
			intentToSyncFlickr.putExtra("uiMessenger", uiMessenger);
		}
		
		startService(intentToSyncFlickr);
    }
    
    static class FlipHandler extends Handler {
	    private final WeakReference<MainActivity> myActivity; 

	    FlipHandler (MainActivity activity) {
	    	myActivity = new WeakReference<MainActivity>(activity);
	    }
	    @Override
	    public void handleMessage(Message msg)
	    {
	    	MainActivity activity = myActivity.get();
	         if (activity != null) {
	        	 activity.handleFlipMessage(msg);
	         }
	    }
	}
	
	public void handleFlipMessage(Message msg) {
		viewPager.setCurrentItem(viewPager.getCurrentItem()+1);
    }
	
	public void stopFlipping() {
		viewPager.setFixedScrolling(false);
		flipper.interrupt();
		playButton.setImageResource(R.drawable.ic_media_play);
	}
	
	private void startFlipping(int interval){
		viewPager.setFixedScrolling(true);
		//Create a timer thread to request sync every X ms
		playButton.setImageResource(R.drawable.ic_media_pause);
		flipper.setInterval(interval);
		if(!flipper.isRunning())
			flipper.begin();
	}
	
	class flipperThread extends Thread {
        private int interval=3000;
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

  
}
