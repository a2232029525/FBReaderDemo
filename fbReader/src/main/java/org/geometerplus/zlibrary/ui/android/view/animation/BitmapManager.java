package org.geometerplus.zlibrary.ui.android.view.animation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import org.geometerplus.zlibrary.core.view.ZLViewEnums;


public interface BitmapManager {
	Bitmap getBitmap(ZLViewEnums.PageIndex index);
	void drawBitmap(Canvas canvas, int x, int y, ZLViewEnums.PageIndex index, Paint paint);
	// added by leixun, i'm sorry
	void shift(boolean forward);
}