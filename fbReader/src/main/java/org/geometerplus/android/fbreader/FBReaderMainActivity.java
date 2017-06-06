/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;

/**
 * 该类是FBReader的父类，实现功能如下：
 * ·转屏判断
 * ·亮度判断
 * ·电量判断
 * ·wakeLock
 */
public abstract class FBReaderMainActivity extends Activity {

    public static final int REQUEST_PREFERENCES = 1;
    public static final int REQUEST_CANCEL_MENU = 2;


    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        //自定义捕获异常
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public ZLAndroidLibrary getZLibrary() {
        return ((ZLAndroidApplication)getApplication()).library();
    }

    /* ++++++ SCREEN BRIGHTNESS(屏幕亮度) ++++++ */
    protected void setScreenBrightnessAuto() {
        final WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.screenBrightness = -1.0f;
        getWindow().setAttributes(attrs);
    }

    /**
     * @param level 是一个0.0-1.0之间的一个float类型数值
     */
    public void setScreenBrightnessSystem(float level) {
        final WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.screenBrightness = level;
        getWindow().setAttributes(attrs);
    }

    public float getScreenBrightnessSystem() {
        final float level = getWindow().getAttributes().screenBrightness;
        return level >= 0 ? level : .5f;
    }
    /* ------ SCREEN BRIGHTNESS(屏幕亮度) ------ */



}
