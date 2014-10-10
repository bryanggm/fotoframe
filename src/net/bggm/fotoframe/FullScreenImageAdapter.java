package net.bggm.fotoframe;


import java.util.ArrayList;

import net.bggm.fotoframe.util.TouchImageView;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.android.networkusage.R;
import com.imbryk.viewPager.LoopViewPager;


public class FullScreenImageAdapter extends PagerAdapter{

	private Activity _activity;
	private ArrayList<String> _imagePaths;

	private TextView txtPages;
	private LayoutInflater inflater;
	private TouchImageView imgDisplay;
	
	//BEGIN BORROW
	  private SparseArray<ToDestroy> mToDestroy = new SparseArray<ToDestroy>();
	
	    private boolean mBoundaryCaching;
	
	    public void setBoundaryCaching(boolean flag) {
	        mBoundaryCaching = flag;
	    }
	    
	    
	    @Override
	    public void notifyDataSetChanged() {
	        mToDestroy = new SparseArray<ToDestroy>();
	        super.notifyDataSetChanged();
	    }

	    public int toLogicalPosition(int position) {
	        int logicalCount = getLogicalCount();
	        if (logicalCount == 0)
	            return 0;
	        int logicalPosition = (position-1) % logicalCount;
	        if (logicalPosition < 0)
	            logicalPosition += logicalCount;

	        return logicalPosition;
	    }

	    public int toInnerPosition(int logicalPosition) {
	        int position = (logicalPosition + 1);
	        return position;
	    }

	    public int getRealFirstPosition() {
	        return 1;
	    }

	    public int getRealLastPosition() {
	        return getLogicalCount();
	    }
	    
	    @Override
	    public int getCount() {
	    	int count =_imagePaths.size() + 2; 
	        return count;
	    	
	    }

	    public int getLogicalCount() {
	    	int count =_imagePaths.size(); 
	    	 return count;
	    	//return 6;
	    }
	    
	    //END BORROW

	// constructor
	public FullScreenImageAdapter(Activity activity,
			ArrayList<String> imagePaths) {
		this._activity = activity;
		this._imagePaths = imagePaths;
		
	}

	public void setImages(ArrayList<String> imagePaths){
		this._imagePaths=imagePaths;
	}
	
	@Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
    }
	

	
	@Override
    public Object instantiateItem(ViewGroup container, int position) {
      
 //     Button btnClose;
		
        inflater = (LayoutInflater) _activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container, false);
 
        imgDisplay = (TouchImageView) viewLayout.findViewById(R.id.imgDisplay);
        txtPages = (TextView) viewLayout.findViewById(R.id.txtPages);
        txtPages.setText(String.valueOf(position));
        //btnClose = (Button) viewLayout.findViewById(R.id.btnClose);
        int logicalPosition = (position-1) % _imagePaths.size();
        if (logicalPosition < 0)
        	logicalPosition += _imagePaths.size();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        txtPages.setText(String.valueOf(position));
        Bitmap bitmap = BitmapFactory.decodeFile(_imagePaths.get(logicalPosition), options);
        imgDisplay.setImageBitmap(bitmap);

        ((LoopViewPager) container).addView(viewLayout);
 
        return viewLayout;
	}
	
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        int realFirst = getRealFirstPosition();
        int realLast = getRealLastPosition();
        int logicalPosition = toLogicalPosition(position);

        if (mBoundaryCaching && (position == realFirst || position == realLast)) {
            mToDestroy.put(position, new ToDestroy(container, logicalPosition,
                    object));
        } else {
        	((LoopViewPager) container).removeView((RelativeLayout) object);
        }
    }
	
	

	 /**
     * Container class for caching the boundary views
     */
    static class ToDestroy {
        ViewGroup container;
        int position;
        Object object;

        public ToDestroy(ViewGroup container, int position, Object object) {
            this.container = container;
            this.position = position;
            this.object = object;
        }
    }
}
