package net.bggm.fotoframe;

import net.bggm.fotoframe.util.Utils;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.android.networkusage.R;
import com.imbryk.viewPager.LoopViewPager;

public class FullScreenViewActivity extends Activity{

	private Utils utils;
	//private FullScreenImageAdapter inadapter;
	private FullScreenImageAdapter adapter;
	private LoopViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fullscreen_view);

		viewPager = (LoopViewPager) findViewById(R.id.pager);

		utils = new Utils(getApplicationContext());

		Intent i = getIntent();
		int position = i.getIntExtra("position", 0);

		//inadapter = 
		adapter = new FullScreenImageAdapter(FullScreenViewActivity.this,
				utils.getFilePaths());
		
		viewPager.setAdapter(adapter);

		// displaying selected image first
		viewPager.setCurrentItem(position);
	}
	   /** Called when the user touches the button */
    public void clickPlayButton(View view) {
//    	if(flipper.isRunning()){
//    		stopFlipping();
//    	}
//    	else{
//    		flipImages(fliptime);
//    	}
    }
}
