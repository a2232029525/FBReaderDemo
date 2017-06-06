/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.zlibrary.ui.android.library;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Process;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.zlibrary.ui.android.error.BugReportActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 自己捕获异常，并处理，当发生无法捕捉的异常
 */
public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Context myContext;

    public UncaughtExceptionHandler(Context context) {
        myContext = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        final StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        System.err.println(stackTrace);

        //发送错误到FixBooksDirectoryActivity;开启一个activity用于处理和显示页面
        Intent intent = new Intent(FBReaderIntents.Action.CRASH, new Uri.Builder().scheme(exception.getClass().getSimpleName()).build());

        intent.setPackage(FBReaderIntents.DEFAULT_PACKAGE);
        try {
            myContext.startActivity(intent);
        }catch (ActivityNotFoundException e) {
            //如果进不去，则跳转到手动提交bug页面
            intent = new Intent(myContext, BugReportActivity.class);
            intent.putExtra(BugReportActivity.STACKTRACE, stackTrace.toString());
            myContext.startActivity(intent);
        }

        if (myContext instanceof Activity) {
            ((Activity)myContext).finish();
        }

        Process.killProcess(Process.myPid());
        System.exit(10);
    }
}
