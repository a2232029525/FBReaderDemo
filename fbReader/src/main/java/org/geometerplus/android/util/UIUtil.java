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

package org.geometerplus.android.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import org.fbreader.util.Pair;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import java.util.LinkedList;
import java.util.Queue;

public abstract class UIUtil {

    private static final Object ourMonitor = new Object();
    private static ProgressDialog ourProgress;
    private static final Queue<Pair<Runnable, String>> ourTaskQueue = new LinkedList<Pair<Runnable, String>>();
    private static volatile Handler ourProgressHandler;

    private static boolean init() {
        if (ourProgressHandler != null) {
            return true;
        }
        try {
            ourProgressHandler = new Handler() {
                public void handleMessage(Message message) {
                    try {
                        synchronized (ourMonitor) {
                            //在Handler类中判断ourTaskQueue中是否为空，为空时才让进度条消失
                            if (ourTaskQueue.isEmpty()) {
                                ourProgress.dismiss();
                                ourProgress = null;
                            }else {
                                //更新显示提示内容，并且移除一个消息
                                ourProgress.setMessage(ourTaskQueue.peek().Second);
                            }
                            //确保ourTaskQueue线程同步
                            ourMonitor.notify();
                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                        ourProgress = null;
                    }
                }
            };
            return true;
        }catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static void wait(String key, String param, Runnable action, Context context) {
        waitInternal(getWaitMessage(key).replace("%s", param), action, context);
    }

    public static void wait(String key, Runnable action, Context context) {
        waitInternal(getWaitMessage(key), action, context);
    }

    private static String getWaitMessage(String key) {
        return ZLResource.resource("dialog").getResource("waitMessage").getResource(key).getValue();
    }

    private static void waitInternal(String message, Runnable action, Context context) {
        if (!init()) {
            action.run();
            return;
        }

        //在后台有多个任务下确保线程同步
        synchronized (ourMonitor) {
            //多个任务同时等待,主要解决多个任务，显示进度问题
            ourTaskQueue.offer(new Pair(action, message));

            //主线程显示提示框
            if (ourProgress == null) {
                ourProgress = ProgressDialog.show(context, null, message, true, false);
            }else {
                return;
            }
        }
        final ProgressDialog currentProgress = ourProgress;

        //子线程关闭提示框
        new Thread(new Runnable() {
            public void run() {
                while (ourProgress == currentProgress && !ourTaskQueue.isEmpty()) {
                    final Pair<Runnable, String> p = ourTaskQueue.poll();
                    p.First.run();
                    synchronized (ourMonitor) {
                        ourProgressHandler.sendEmptyMessage(0);
                        try {
                            //确保ourTaskQueue线程同步
                            ourMonitor.wait();
                        }catch (InterruptedException e) {
                        }
                    }
                }
            }
        }).start();
    }

    public static ZLApplication.SynchronousExecutor createExecutor(final Activity activity, final String key) {
        return new ZLApplication.SynchronousExecutor() {
            private final ZLResource myResource = ZLResource.resource("dialog").getResource("waitMessage");
            private final String myMessage = myResource.getResource(key).getValue();
            private volatile ProgressDialog myProgress;

            public void execute(final Runnable action, final Runnable uiPostAction) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        myProgress = ProgressDialog.show(activity, null, myMessage, true, false);
                        final Thread runner = new Thread() {
                            public void run() {
                                action.run();
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        try {
                                            myProgress.dismiss();
                                            myProgress = null;
                                        }catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        if (uiPostAction != null) {
                                            uiPostAction.run();
                                        }
                                    }
                                });
                            }
                        };
                        runner.setPriority(Thread.MAX_PRIORITY);
                        runner.start();
                    }
                });
            }

            private void setMessage(final ProgressDialog progress, final String message) {
                if (progress == null) {
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        progress.setMessage(message);
                    }
                });
            }

            public void executeAux(String key, Runnable runnable) {
                setMessage(myProgress, myResource.getResource(key).getValue());
                runnable.run();
                setMessage(myProgress, myMessage);
            }
        };
    }
}
