package net.bggm.fotoframe.mainview;


import net.bggm.fotoframe.util.fileManager;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;


public class LoopedViewPagerAdapter extends FragmentStatePagerAdapter  {

	private fileManager files;
	
    @Override
    public int getCount() {
        return files.size() + 2;
    }

    public int getLogicalCount() {
    	 return files.size();
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

	public  LoopedViewPagerAdapter(FragmentManager fragmentManager, Context context) {
        super(fragmentManager);
		files = fileManager.getInstance(context);
    }    
	
	@Override
	public Fragment getItem(int index){
		return PictureFragment.newInstance(toLogicalPosition(index));
	}
}
