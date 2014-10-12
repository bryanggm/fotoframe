 package net.bggm.fotoframe;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;

import com.example.android.networkusage.R;

public class FullScreenViewActivity extends FragmentActivity{

	private FullScreenImageAdapter adapter;
	private ViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_fullscreen_view);

		adapter = new FullScreenImageAdapter(getSupportFragmentManager(), FullScreenViewActivity.this);

        viewPager = (ViewPager)findViewById(R.id.pager);
		viewPager.setAdapter(adapter);
		
		viewPager.setOnPageChangeListener(new OnPageChangeListener() {
	        private float mPreviousPosition = -1;
	        
	        @Override
	        public void onPageSelected(int position) {
	
	            int logicalPosition = adapter.toLogicalPosition(position);
	            if (mPreviousPosition != logicalPosition) {
	                mPreviousPosition = logicalPosition;
	            }
	        }
	        @Override
	        public void onPageScrolled(int position, float positionOffset,
	                int positionOffsetPixels) {
	           
	        }
	        @Override
	        public void onPageScrollStateChanged(int state) {
	            if (adapter != null) {
	                int position = viewPager.getCurrentItem();
	                int logicalPosition = adapter.toLogicalPosition(position);
	                if (state == ViewPager.SCROLL_STATE_IDLE
	                        && (position == 0 || position == adapter.getCount() - 1)) {
	                	viewPager.setCurrentItem(logicalPosition+1, false);
	                }
	            }
	        }
		});

		Intent i = getIntent();
		int position = i.getIntExtra("position", 0);
		viewPager.setCurrentItem(position);
	}
	
	   /** Called when the user touches the button */
    public void clickPlayButton(View view) {
    }
    
    
    
  
}
