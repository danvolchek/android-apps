package com.volchekD.SVN_Explorer;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PathView extends LinearLayout {
	private String path;
	private ImageView image;
	private Context con;
	public PathView(Context c, String p) {
		super(c);
		path = p;
		con = c;

		setOrientation(HORIZONTAL);
		TextView tv = (TextView) View.inflate(c, android.R.layout.simple_list_item_1, null);
		tv.setText(p);
		addView(tv);

		image = new ImageView(con);
		image.setVisibility(View.GONE);
		image.setImageDrawable(getResources().getDrawable(R.drawable.ic_navigate_next_white_36dp));
		addView(image);

	}

	public void showImage() {
		image.setVisibility(View.VISIBLE);
	}

	public void hideImage() {
		image.setVisibility(View.GONE);
	}

	public String getPath() {
		return path;
	}

	public PathView(Context context) {
		super(context);
	}


}
