package net.bggm.fotoframe.util;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

public class fileManager {
	    private static fileManager instance;
		String photoAlbum="myPics";
		private Context _context;
		private ArrayList<String>filePaths = new ArrayList<String>();

		// constructor
		private fileManager(Context context) {
			this._context = context;
		}
		public static fileManager getInstance(Context context) {
	        if (instance == null) {
	            instance = new fileManager(context);
	        }
	        return instance;
	    }

		/*
		 * Reading file paths from SDCard  TODO REWORK
		 */
		public void updateFilePaths() {
			

			File directory = getAlbumStorageDir(photoAlbum);

			// check for directory
			if (directory.isDirectory()) {
				// getting list of file paths
				File[] listFiles = directory.listFiles();

				// Check for count
				if (listFiles.length > 0) {

					// loop through all files
					for (int i = 0; i < listFiles.length; i++) {

						// get file path
						String filePath = listFiles[i].getAbsolutePath();
						filePaths.add(filePath);
					}
				} else {
					// image directory is empty
					Toast.makeText(
							_context,
							photoAlbum
									+ " is empty. Please load some images in it !",
							Toast.LENGTH_LONG).show();
				}

			} else {
				AlertDialog.Builder alert = new AlertDialog.Builder(_context);
				alert.setTitle("Error!");
				alert.setMessage(photoAlbum
						+ " directory path is not valid! Please set the image directory name AppConstant.java class");
				alert.setPositiveButton("OK", null);
				alert.show();
			}

		}
		
		public String getFilePathByName(String name) {

			return (new File(getAlbumStorageDir(photoAlbum), name)).getAbsolutePath();
			
		}
		
		private File getAlbumStorageDir(String albumName) {
		    // Get the directory for the user's public pictures directory.
		    File file = new File(Environment.getExternalStoragePublicDirectory(
		            Environment.DIRECTORY_PICTURES), albumName);
		    file.mkdirs();
		    return file;
		}
		
		public int size() {
		    return filePaths.size();
		}
		
		public String get(int index){
			return filePaths.get(index);
		}
		
		

}
