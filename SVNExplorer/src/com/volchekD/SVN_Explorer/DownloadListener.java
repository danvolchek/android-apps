package com.volchekD.SVN_Explorer;

import android.graphics.Bitmap;


public interface DownloadListener {
	void downloadFinished(String result, String path, String error, Bitmap b, boolean user);
	void newStarted();
	boolean isGradeFile(String ff);
}