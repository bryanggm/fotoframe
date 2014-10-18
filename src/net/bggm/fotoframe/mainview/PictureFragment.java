/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bggm.fotoframe.mainview;

import net.bggm.fotoframe.util.TouchImageView;
import net.bggm.fotoframe.util.fileManager;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.networkusage.R;


public class PictureFragment extends Fragment {
    final static String ARG_POSITION = "position";
    int mCurrentPosition = -1;
    
	private TextView txtPages;
	private TouchImageView imgDisplay;
	private fileManager files;
	private Activity parent;
	
	 @Override
	 public void onCreate(Bundle savedInstanceState) {
	 super.onCreate(savedInstanceState);
	 mCurrentPosition = getArguments() != null ? getArguments().getInt("position") : 1;
	 }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    	View pictureView = inflater.inflate(R.layout.picture_view, container,
    			 false);
    	
        imgDisplay = (TouchImageView) pictureView.findViewById(R.id.imgDisplay);
        txtPages = (TextView) pictureView.findViewById(R.id.txtPages);
        txtPages.setText(String.valueOf(mCurrentPosition));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        txtPages.setText(String.valueOf(mCurrentPosition));
        Bitmap bitmap = BitmapFactory.decodeFile(files.get(mCurrentPosition), options);
        imgDisplay.setImageBitmap(bitmap);
        return pictureView;
    }
    
	 static PictureFragment newInstance(int position) {
		 PictureFragment f = new PictureFragment();
		 Bundle args = new Bundle();
		 args.putInt("position", position);
		 f.setArguments(args);
		 return f;
	 }
  
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        parent = activity;
        files = fileManager.getInstance(parent);
    }
}