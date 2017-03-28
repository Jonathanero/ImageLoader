package com.nero.imageloader;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by lishiyan on 17/3/28.
 */

public class SquareImageView extends ImageView{

	public SquareImageView(final Context context){
		super(context);
	}

	public SquareImageView(final Context context,final AttributeSet attrs){
		super(context,attrs);
	}

	public SquareImageView(final Context context,final AttributeSet attrs,final int defStyleAttr){
		super(context,attrs,defStyleAttr);
	}

	@Override
	protected void onMeasure(final int widthMeasureSpec,final int heightMeasureSpec){
		super.onMeasure(widthMeasureSpec,widthMeasureSpec);
	}

	@Override
	protected void onDraw(final Canvas canvas){
		super.onDraw(canvas);
	}
}
