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

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

import org.geometerplus.android.fbreader.api.ApiListener;
import org.geometerplus.android.fbreader.api.ApiServerImplementation;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.api.MenuNode;
import org.geometerplus.android.fbreader.api.PluginApi;
import org.geometerplus.android.fbreader.formatPlugin.PluginUtil;
import org.geometerplus.android.fbreader.httpd.DataService;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.DeviceType;
import org.geometerplus.android.util.SearchDialogUtil;
import org.geometerplus.android.util.UIMessageUtil;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.options.ColorProfile;
import org.geometerplus.fbreader.formats.ExternalFormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.options.Config;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.view.ZLTextRegion;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidCurlWidget;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//import org.geometerplus.android.fbreader.sync.SyncOperations;
//import org.geometerplus.zlibrary.ui.android.error.ErrorKeys;

/**
 * 主界面，入口
 */
public final class FBReader extends FBReaderMainActivity implements ZLApplicationWindow {

    public static final int RESULT_DO_NOTHING = RESULT_FIRST_USER;
    public static final int RESULT_REPAINT = RESULT_FIRST_USER + 1;

    public static Intent defaultIntent(Context context) {
        return new Intent(context, FBReader.class).setAction(FBReaderIntents.Action.VIEW).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    public static void openBookActivity(Context context, Book book, Bookmark bookmark) {
        final Intent intent = defaultIntent(context);
        FBReaderIntents.putBookExtra(intent, book);
        FBReaderIntents.putBookmarkExtra(intent, bookmark);
        context.startActivity(intent);
    }

    private FBReaderApp myFBReaderApp;
    private volatile Book myBook;

    private RelativeLayout myRootView;
    /**
     * 翻页组件
     */
    private ZLAndroidWidget myMainView;
    private ZLAndroidCurlWidget myCurlView;

    private volatile boolean myShowStatusBarFlag;
    private String myMenuLanguage;

    final DataService.Connection DataConnection = new DataService.Connection();

    volatile boolean IsPaused = false;
    private volatile long myResumeTimestamp;
    volatile Runnable OnResumeAction = null;

    private Intent myOpenBookIntent = null;


    private static final String PLUGIN_ACTION_PREFIX = "___";
    private final List<PluginApi.ActionInfo> myPluginActions = new LinkedList<PluginApi.ActionInfo>();
    private final BroadcastReceiver myPluginInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final ArrayList<PluginApi.ActionInfo> actions = getResultExtras(true).<PluginApi.ActionInfo> getParcelableArrayList(PluginApi.PluginInfo.KEY);
            if (actions != null) {
                synchronized (myPluginActions) {
                    int index = 0;
                    while (index < myPluginActions.size()) {
                        myFBReaderApp.removeAction(PLUGIN_ACTION_PREFIX + index++);
                    }
                    myPluginActions.addAll(actions);
                    index = 0;
                    for (PluginApi.ActionInfo info : myPluginActions) {
                        myFBReaderApp.addAction(PLUGIN_ACTION_PREFIX + index++, new RunPluginAction(FBReader.this, myFBReaderApp, info.getId()));
                    }
                }
            }
        }
    };

    /**
     * 打开图书
     *
     * @param intent
     * @param action
     * @param force
     */
    private synchronized void openBook(Intent intent, final Runnable action, boolean force) {
        if (!force && myBook != null) {
            return;
        }

        myBook = FBReaderIntents.getBookExtra(intent, myFBReaderApp.Collection);
        final Bookmark bookmark = FBReaderIntents.getBookmarkExtra(intent);
        if (myBook == null) {
            final Uri data = intent.getData();
            if (data != null) {
                myBook = createBookForFile(ZLFile.createFileByPath(data.getPath()));
            }
        }
        if (myBook != null) {
            ZLFile file = BookUtil.fileByBook(myBook);
            if (!file.exists()) {
                if (file.getPhysicalFile() != null) {
                    file = file.getPhysicalFile();
                }
                UIMessageUtil.showErrorMessage(this, "fileNotFound", file.getPath());
                myBook = null;
            }else {
                NotificationUtil.drop(this, myBook);
            }
        }
        Config.Instance().runOnConnect(new Runnable() {
            public void run() {
                myFBReaderApp.openBook(myBook, bookmark, action);
                AndroidFontUtil.clearFontCache();
            }
        });
    }

    private Book createBookForFile(ZLFile file) {
        if (file == null) {
            return null;
        }
        Book book = myFBReaderApp.Collection.getBookByFile(file.getPath());
        if (book != null) {
            return book;
        }
        if (file.isArchive()) {
            for (ZLFile child : file.children()) {
                book = myFBReaderApp.Collection.getBookByFile(child.getPath());
                if (book != null) {
                    return book;
                }
            }
        }
        return null;
    }

    private Runnable getPostponedInitAction() {
        return new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        final Intent intent = getIntent();
                        if (intent != null && FBReaderIntents.Action.PLUGIN.equals(intent.getAction())) {
                            new RunPluginAction(FBReader.this, myFBReaderApp, intent.getData()).run();
                        }
                    }
                });
            }
        };
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

//        bindService(new Intent(this, DataService.class), DataConnection, DataService.BIND_AUTO_CREATE);

        final Config config = Config.Instance();
        //ConfigShadow
        config.runOnConnect(new Runnable() {
            public void run() {
                config.requestAllValuesForGroup("Options");
                config.requestAllValuesForGroup("Style");
                config.requestAllValuesForGroup("LookNFeel");
                config.requestAllValuesForGroup("Fonts");
                config.requestAllValuesForGroup("Colors");
                config.requestAllValuesForGroup("Files");
            }
        });

        final ZLAndroidLibrary zlibrary = getZLibrary();
        myShowStatusBarFlag = zlibrary.ShowStatusBarOption.getValue();

        //隐藏标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        myRootView = (RelativeLayout)findViewById(R.id.root_view);
        myMainView = (ZLAndroidWidget)findViewById(R.id.main_view);
        //按键会打开本地搜索
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        myFBReaderApp = (FBReaderApp)FBReaderApp.Instance();
        if (myFBReaderApp == null) {
            myFBReaderApp = new FBReaderApp(Paths.systemInfo(this), new BookCollectionShadow());
        }
        getCollection().bindToService(this, null);
        myBook = null;

        myFBReaderApp.setWindow(this);
        myFBReaderApp.initWindow();

        myFBReaderApp.setExternalFileOpener(new ExternalFileOpener(this));

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, myShowStatusBarFlag ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //添加3个popup到FBreaderApp
        if (myFBReaderApp.getPopupById(TextSearchPopup.ID) == null) {
            //内容查找后，显示的操作
            new TextSearchPopup(myFBReaderApp);
        }
        if (myFBReaderApp.getPopupById(NavigationPopup.ID) == null) {
            //快速翻看
            new NavigationPopup(myFBReaderApp);
        }
        if (myFBReaderApp.getPopupById(SelectionPopup.ID) == null) {
            //长按文本，显示的文本框
            new SelectionPopup(myFBReaderApp);
        }
        if (myFBReaderApp.getPopupById(SettingPopup.ID) == null) {
            new SettingPopup(myFBReaderApp);
        }


        //操作action
        myFBReaderApp.addAction(ActionCode.SHOW_PREFERENCES, new ShowPreferencesAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SHOW_TOC, new ShowTOCAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SHOW_BOOKMARKS, new ShowBookmarksAction(this, myFBReaderApp));

        myFBReaderApp.addAction(ActionCode.SHOW_MENU, new ShowMenuAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SHOW_NAVIGATION, new ShowNavigationAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SEARCH, new SearchAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SHARE_BOOK, new ShareBookAction(this, myFBReaderApp));

        myFBReaderApp.addAction(ActionCode.SELECTION_SHOW_PANEL, new SelectionShowPanelAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_HIDE_PANEL, new SelectionHidePanelAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new SelectionCopyAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_SHARE, new SelectionShareAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_BOOKMARK, new SelectionBookmarkAction(this, myFBReaderApp));


        myFBReaderApp.addAction(ActionCode.SHOW_CANCEL_MENU, new ShowCancelMenuAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.OPEN_START_SCREEN, new StartScreenAction(this, myFBReaderApp));

        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_SYSTEM, new SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_SYSTEM));
        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_SENSOR, new SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_SENSOR));
        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT, new SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_PORTRAIT));
        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE, new SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_LANDSCAPE));

        //是否支持横竖屏
        if (getZLibrary().supportsAllOrientations()) {
            myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT, new SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_REVERSE_PORTRAIT));
            myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE, new SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_REVERSE_LANDSCAPE));
        }

//        myFBReaderApp.addAction(ActionCode.OPEN_WEB_HELP, new OpenWebHelpAction(this, myFBReaderApp));

        myFBReaderApp.addAction(ActionCode.SWITCH_TO_DAY_PROFILE, new SwitchProfileAction(this, myFBReaderApp, ColorProfile.DAY));
        myFBReaderApp.addAction(ActionCode.SWITCH_TO_NIGHT_PROFILE, new SwitchProfileAction(this, myFBReaderApp, ColorProfile.NIGHT));
        new OpenPhotoAction(this,myFBReaderApp,myRootView);
        final Intent intent = getIntent();
        final String action = intent.getAction();

        myOpenBookIntent = intent;
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
          if (FBReaderIntents.Action.PLUGIN_CRASH.equals(action)) {
                myFBReaderApp.ExternalBook = null;
                myOpenBookIntent = null;
                getCollection().bindToService(this, new Runnable() {
                    public void run() {
                        myFBReaderApp.openBook(null, null, null);
                    }
                });
            }
        }
    }

    /**
     * 是每次在display Menu之前，都会去调用，
     * 只要按一次Menu按鍵，就会调用一次,
     * 所以可以在这里动态的改变menu。
     *
     * @param menu
     *
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setStatusBarVisibility(true);

        //设置menu
        setupMenu(menu);

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * 只会调用一次，他只会在Menu显示之前去调用一次，之后就不会在去调用
     *
     * @param menu
     *
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        //设置menu
        setupMenu(menu);

        return true;
    }

    /**
     * 每次菜单被关闭时调用
     *
     * @param menu
     */
    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        setStatusBarVisibility(false);
    }

    /**
     * 菜单项被点击时调用，也就是菜单项的监听方法。
     *
     * @param item
     *
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        setStatusBarVisibility(false);
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        final String action = intent.getAction();
        final Uri data = intent.getData();

        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            super.onNewIntent(intent);
        }else if (Intent.ACTION_VIEW.equals(action) && data != null && "fbreader-action".equals(data.getScheme())) {
            myFBReaderApp.runAction(data.getEncodedSchemeSpecificPart(), data.getFragment());
        }else if (Intent.ACTION_VIEW.equals(action) || FBReaderIntents.Action.VIEW.equals(action)) {
            myOpenBookIntent = intent;
            if (myFBReaderApp.Model == null && myFBReaderApp.ExternalBook != null) {
                final BookCollectionShadow collection = getCollection();
                final Book b = FBReaderIntents.getBookExtra(intent, collection);
                if (!collection.sameBook(b, myFBReaderApp.ExternalBook)) {
                    try {
                        final ExternalFormatPlugin plugin = (ExternalFormatPlugin)BookUtil.getPlugin(PluginCollection.Instance(Paths.systemInfo(this)), myFBReaderApp.ExternalBook);
                        startActivity(PluginUtil.createIntent(plugin, FBReaderIntents.Action.PLUGIN_KILL));
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }else if (FBReaderIntents.Action.PLUGIN.equals(action)) {
            new RunPluginAction(this, myFBReaderApp, data).run();
        }else if (Intent.ACTION_SEARCH.equals(action)) {
            Logger.e("这里是输入要搜索的数据后，回车走的方法------");
            final String pattern = intent.getStringExtra(SearchManager.QUERY);
            final Runnable runnable = new Runnable() {
                public void run() {
                    final TextSearchPopup popup = (TextSearchPopup)myFBReaderApp.getPopupById(TextSearchPopup.ID);
                    popup.initPosition();
                    myFBReaderApp.MiscOptions.TextSearchPattern.setValue(pattern);
                    if (myFBReaderApp.getTextView().search(pattern, true, false, false, false) != 0) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                myFBReaderApp.showPopup(popup.getId());
                            }
                        });
                    }else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                UIMessageUtil.showErrorMessage(FBReader.this, "textNotFound");
                                popup.StartPosition = null;
                            }
                        });
                    }
                }
            };
            UIUtil.wait("search", runnable, this);
        }if (FBReaderIntents.Action.PLUGIN_CRASH.equals(intent.getAction())) {
            final Book book = FBReaderIntents.getBookExtra(intent, myFBReaderApp.Collection);
            myFBReaderApp.ExternalBook = null;
            myOpenBookIntent = null;
            getCollection().bindToService(this, new Runnable() {
                public void run() {
                    final BookCollectionShadow collection = getCollection();
                    Book b = collection.getRecentBook(0);
                    if (collection.sameBook(b, book)) {
                        b = collection.getRecentBook(1);
                    }
                    myFBReaderApp.openBook(b, null, null);
                }
            });
        }else {
            super.onNewIntent(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        getCollection().bindToService(this, new Runnable() {
            public void run() {
                new Thread() {
                    public void run() {
                        getPostponedInitAction().run();
                    }
                }.start();

                myFBReaderApp.getViewWidget().repaint();
            }
        });

        initPluginActions();

        final ZLAndroidLibrary zlibrary = getZLibrary();

        //检查屏幕大小是否符合，不符重开activity
        Config.Instance().runOnConnect(new Runnable() {
            public void run() {
                final boolean showStatusBar = zlibrary.ShowStatusBarOption.getValue();
                if (showStatusBar != myShowStatusBarFlag) {
                    finish();
                    startActivity(new Intent(FBReader.this, FBReader.class));
                }
                zlibrary.ShowStatusBarOption.saveSpecialValue();
                myFBReaderApp.ViewOptions.ColorProfileName.saveSpecialValue();
                SetScreenOrientationAction.setOrientation(FBReader.this, zlibrary.getOrientationOption().getValue());
            }
        });

        ((PopupPanel)myFBReaderApp.getPopupById(TextSearchPopup.ID)).setPanelInfo(this, myRootView);
        ((NavigationPopup)myFBReaderApp.getPopupById(NavigationPopup.ID)).setPanelInfo(this, myRootView);
        ((PopupPanel)myFBReaderApp.getPopupById(SelectionPopup.ID)).setPanelInfo(this, myRootView);
        ((SettingPopup) myFBReaderApp.getPopupById(SettingPopup.ID)).setPanelInfo(this, myRootView);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        switchWakeLock(hasFocus && getZLibrary().BatteryLevelToTurnScreenOffOption.getValue() < myFBReaderApp.getBatteryLevel());
    }

    private void initPluginActions() {
        synchronized (myPluginActions) {
            int index = 0;
            while (index < myPluginActions.size()) {
                myFBReaderApp.removeAction(PLUGIN_ACTION_PREFIX + index++);
            }
            myPluginActions.clear();
        }

        sendOrderedBroadcast(new Intent(PluginApi.ACTION_REGISTER), null, myPluginInfoReceiver, null, RESULT_OK, null, null);
    }


    @Override
    protected void onResume() {
        super.onResume();

        myStartTimer = true;
        Config.Instance().runOnConnect(new Runnable() {
            public void run() {

                //同步数据
//                SyncOperations.enableSync(FBReader.this, myFBReaderApp.SyncOptions);

                final int brightnessLevel = getZLibrary().ScreenBrightnessLevelOption.getValue();

                if (brightnessLevel != 0) {
                    getViewWidget().setScreenBrightness(brightnessLevel);
                }else {
                    setScreenBrightnessAuto();
                }
                if (getZLibrary().DisableButtonLightsOption.getValue()) {
                    setButtonLight(false);
                }

                getCollection().bindToService(FBReader.this, new Runnable() {
                    public void run() {
                        final BookModel model = myFBReaderApp.Model;
                        if (model == null || model.Book == null) {
                            return;
                        }
                        onPreferencesUpdate(myFBReaderApp.Collection.getBookById(model.Book.getId()));
                    }
                });
            }
        });

        //注册电池电量监听
        registerReceiver(myBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        IsPaused = false;
        myResumeTimestamp = System.currentTimeMillis();
        if (OnResumeAction != null) {
            final Runnable action = OnResumeAction;
            OnResumeAction = null;
            action.run();
        }

        registerReceiver(mySyncUpdateReceiver, new IntentFilter(FBReaderIntents.Event.SYNC_UPDATED));

        SetScreenOrientationAction.setOrientation(this, getZLibrary().getOrientationOption().getValue());
       if (myOpenBookIntent != null) {
            //打开图书
            final Intent intent = myOpenBookIntent;
            myOpenBookIntent = null;
            getCollection().bindToService(this, new Runnable() {
                public void run() {
                    openBook(intent, null, true);
                }
            });
        }else if (myFBReaderApp.Model == null && myFBReaderApp.ExternalBook != null) {
            getCollection().bindToService(this, new Runnable() {
                public void run() {
                    myFBReaderApp.openBook(myFBReaderApp.ExternalBook, null, null);
                }
            });
        }else {
            getCollection().bindToService(this, new Runnable() {
                public void run() {
                    myFBReaderApp.useSyncInfo(true);
                }
            });
        }

        PopupPanel.restoreVisibilities(myFBReaderApp);
        ApiServerImplementation.sendEvent(this, ApiListener.EVENT_READ_MODE_OPENED);
    }

    @Override
    protected void onPause() {
//        SyncOperations.quickSync(this, myFBReaderApp.SyncOptions);

        IsPaused = true;
        try {
            unregisterReceiver(mySyncUpdateReceiver);
        }catch (IllegalArgumentException e) {
        }

        try {
            unregisterReceiver(myBatteryInfoReceiver);
        }catch (IllegalArgumentException e) {
            // do nothing, this exception means that myBatteryInfoReceiver was not registered
        }

        myFBReaderApp.stopTimer();
        if (getZLibrary().DisableButtonLightsOption.getValue()) {
            setButtonLight(true);
        }
        myFBReaderApp.onWindowClosing();

        super.onPause();
    }

    @Override
    protected void onStop() {
        ApiServerImplementation.sendEvent(this, ApiListener.EVENT_READ_MODE_CLOSED);
        PopupPanel.removeAllWindows(myFBReaderApp, this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        getCollection().unbind();
//        unbindService(DataConnection);
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        myFBReaderApp.onWindowClosing();
        super.onLowMemory();
    }

    @Override
    public boolean onSearchRequested() {
        final FBReaderApp.PopupPanel popup = myFBReaderApp.getActivePopup();
        myFBReaderApp.hideActivePopup();
        if (DeviceType.Instance().hasStandardSearchDialog()) {
            final SearchManager manager = (SearchManager)getSystemService(SEARCH_SERVICE);
            manager.setOnCancelListener(new SearchManager.OnCancelListener() {
                public void onCancel() {
                    if (popup != null) {
                        myFBReaderApp.showPopup(popup.getId());
                    }
                    manager.setOnCancelListener(null);
                }
            });
            startSearch(myFBReaderApp.MiscOptions.TextSearchPattern.getValue(), true, null, false);
        }else {
            SearchDialogUtil.showDialog(this, FBReader.class, myFBReaderApp.MiscOptions.TextSearchPattern.getValue(), new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface di) {
                    if (popup != null) {
                        myFBReaderApp.showPopup(popup.getId());
                    }
                }
            });
        }
        return true;
    }

    public void showSelectionPanel() {
        final ZLTextView view = myFBReaderApp.getTextView();
        ((SelectionPopup)myFBReaderApp.getPopupById(SelectionPopup.ID)).move(view.getSelectionStartY(), view.getSelectionEndY());
        myFBReaderApp.showPopup(SelectionPopup.ID);
    }

    public void hideSelectionPanel() {
        final FBReaderApp.PopupPanel popup = myFBReaderApp.getActivePopup();
        if (popup != null && popup.getId() == SelectionPopup.ID) {
            myFBReaderApp.hideActivePopup();
        }
    }

    private void onPreferencesUpdate(Book book) {
        AndroidFontUtil.clearFontCache();
        myFBReaderApp.onBookUpdated(book);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
            case REQUEST_PREFERENCES:
                if (resultCode != RESULT_DO_NOTHING && data != null) {
                    final Book book = FBReaderIntents.getBookExtra(data, myFBReaderApp.Collection);
                    if (book != null) {
                        getCollection().bindToService(this, new Runnable() {
                            public void run() {
                                onPreferencesUpdate(book);
                            }
                        });
                    }
                }
                break;
            case REQUEST_CANCEL_MENU:
                runCancelAction(data);
                break;
        }
    }


    private void runCancelAction(Intent intent) {
        finish();
    }

    public void navigate() {
        ((NavigationPopup)myFBReaderApp.getPopupById(NavigationPopup.ID)).runNavigation();
    }

    private Menu addSubmenu(Menu menu, String id) {
        return menu.addSubMenu(ZLResource.resource("menu").getResource(id).getValue());
    }

    private void addMenuItem(Menu menu, String actionId, Integer iconId, String name) {
        if (name == null) {
            name = ZLResource.resource("menu").getResource(actionId).getValue();
        }
        final MenuItem menuItem = menu.add(name);
        if (iconId != null) {
            menuItem.setIcon(iconId);
        }
        menuItem.setOnMenuItemClickListener(myMenuListener);
        myMenuItemMap.put(menuItem, actionId);
    }

    private void addMenuItem(Menu menu, String actionId, String name) {
        addMenuItem(menu, actionId, null, name);
    }

    private void addMenuItem(Menu menu, String actionId, int iconId) {
        addMenuItem(menu, actionId, iconId, null);
    }

    private void addMenuItem(Menu menu, String actionId) {
        addMenuItem(menu, actionId, null, null);
    }

    private void fillMenu(Menu menu, List<MenuNode> nodes) {
        for (MenuNode n : nodes) {
            if (n instanceof MenuNode.Item) {
                final Integer iconId = ((MenuNode.Item)n).IconId;
                if (iconId != null) {
                    addMenuItem(menu, n.Code, iconId);
                }else {
                    addMenuItem(menu, n.Code);
                }
            }else /* if (n instanceof MenuNode.Submenu) */ {
                final Menu submenu = addSubmenu(menu, n.Code);
                fillMenu(submenu, ((MenuNode.Submenu)n).Children);
            }
        }
    }

    /**
     * 创建menu
     *
     * @param menu
     */
    private void setupMenu(Menu menu) {
        //获取配置的语言
        final String menuLanguage = ZLResource.getLanguageOption().getValue();

        if (menuLanguage.equals(myMenuLanguage)) {
            return;
        }

        myMenuLanguage = menuLanguage;

        menu.clear();
        //添加menu
        fillMenu(menu, MenuData.topLevelNodes());

        synchronized (myPluginActions) {
            int index = 0;
            for (PluginApi.ActionInfo info : myPluginActions) {
                if (info instanceof PluginApi.MenuActionInfo) {
                    addMenuItem(menu, PLUGIN_ACTION_PREFIX + index++, ((PluginApi.MenuActionInfo)info).MenuItemName);
                }
            }
        }

        refresh();
    }

    protected void onPluginNotFound(final Book book) {
        final BookCollectionShadow collection = getCollection();
        collection.bindToService(this, new Runnable() {
            public void run() {
                final Book recent = collection.getRecentBook(0);
                if (recent != null && !collection.sameBook(recent, book)) {
                    myFBReaderApp.openBook(recent, null, null);
                }else {
                    myFBReaderApp.openHelpBook();
                }
            }
        });
    }

    /**
     * 设置状态栏显示
     *
     * @param visible
     */
    private void setStatusBarVisibility(boolean visible) {
        final ZLAndroidLibrary zlibrary = getZLibrary();

        if (DeviceType.Instance() != DeviceType.KINDLE_FIRE_1ST_GENERATION && !myShowStatusBarFlag) {

            if (visible) {
                //window非全屏显示
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            }else {
                //取消window非全屏显示
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            }

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (myMainView != null && myMainView.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return (myMainView != null && myMainView.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
    }

    private void setButtonLight(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            setButtonLightInternal(enabled);
        }
    }

    @TargetApi (Build.VERSION_CODES.FROYO)
    private void setButtonLightInternal(boolean enabled) {
        final WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.buttonBrightness = enabled ? -1.0f : 0.0f;
        getWindow().setAttributes(attrs);
    }

    private PowerManager.WakeLock myWakeLock;
    private boolean myWakeLockToCreate;
    private boolean myStartTimer;

    public final void createWakeLock() {
        if (myWakeLockToCreate) {
            synchronized (this) {
                if (myWakeLockToCreate) {
                    myWakeLockToCreate = false;
                    myWakeLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "FBReader");
                    myWakeLock.acquire();
                }
            }
        }
        if (myStartTimer) {
            myFBReaderApp.startTimer();
            myStartTimer = false;
        }
    }

    private final void switchWakeLock(boolean on) {
        if (on) {
            if (myWakeLock == null) {
                myWakeLockToCreate = true;
            }
        }else {
            if (myWakeLock != null) {
                synchronized (this) {
                    if (myWakeLock != null) {
                        myWakeLock.release();
                        myWakeLock = null;
                    }
                }
            }
        }
    }

    private BroadcastReceiver myBatteryInfoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final int level = intent.getIntExtra("level", 100);
            final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
            setBatteryLevel(level);
            switchWakeLock(hasWindowFocus() && getZLibrary().BatteryLevelToTurnScreenOffOption.getValue() < level);
        }
    };

    private BookCollectionShadow getCollection() {
        return (BookCollectionShadow)myFBReaderApp.Collection;
    }

    // methods from ZLApplicationWindow interface
    @Override
    public void showErrorMessage(String key) {
        UIMessageUtil.showErrorMessage(this, key);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void showErrorMessage(String key, String parameter) {
        UIMessageUtil.showErrorMessage(this, key, parameter);
    }

    @Override
    public FBReaderApp.SynchronousExecutor createExecutor(String key) {
        return UIUtil.createExecutor(this, key);
    }

    private int myBatteryLevel;

    @Override
    public int getBatteryLevel() {
        return myBatteryLevel;
    }

    private void setBatteryLevel(int percent) {
        myBatteryLevel = percent;
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public ZLViewWidget getViewWidget() {
        return myMainView;
    }

    @Override
    public void hideViewWidget(boolean flag) {
        if (myCurlView != null && myMainView != null) {
            if (flag) {
                myCurlView.setVisibility(View.VISIBLE);
                myMainView.setVisibility(View.GONE);
            } else {
                myCurlView.setVisibility(View.GONE);
                myMainView.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "view 切换错误", Toast.LENGTH_SHORT).show();
        }
    }

    private final HashMap<MenuItem, String> myMenuItemMap = new HashMap<MenuItem, String>();

    private final MenuItem.OnMenuItemClickListener myMenuListener = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            myFBReaderApp.runAction(myMenuItemMap.get(item));
            return true;
        }
    };

    @Override
    public void refresh() {
        runOnUiThread(new Runnable() {
            public void run() {
                for (Map.Entry<MenuItem, String> entry : myMenuItemMap.entrySet()) {
                    final String actionId = entry.getValue();
                    final MenuItem menuItem = entry.getKey();
                    menuItem.setVisible(myFBReaderApp.isActionVisible(actionId) && myFBReaderApp.isActionEnabled(actionId));
                    switch (myFBReaderApp.isActionChecked(actionId)) {
                        case TRUE:
                            menuItem.setCheckable(true);
                            menuItem.setChecked(true);
                            break;
                        case FALSE:
                            menuItem.setCheckable(true);
                            menuItem.setChecked(false);
                            break;
                        case UNDEFINED:
                            menuItem.setCheckable(false);
                            break;
                    }
                }
            }
        });
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();

//        final Intent intent = new Intent(FBReaderIntents.Action.ERROR, new Uri.Builder().scheme(exception.getClass().getSimpleName()).build());
//        intent.setPackage(FBReaderIntents.DEFAULT_PACKAGE);
//        intent.putExtra(ErrorKeys.MESSAGE, exception.getMessage());
//        final StringWriter stackTrace = new StringWriter();
//        exception.printStackTrace(new PrintWriter(stackTrace));
//        intent.putExtra(ErrorKeys.STACKTRACE, stackTrace.toString());
        /*
        if (exception instanceof BookReadingException) {
			final ZLFile file = ((BookReadingException)exception).File;
			if (file != null) {
				intent.putExtra("file", file.getPath());
			}
		}
		*/
//        try {
//            startActivity(intent);
//        }catch (ActivityNotFoundException e) {
//            // ignore
//            e.printStackTrace();
//        }
    }

//    @Override
//    public void setWindowTitle(final String title) {
//        runOnUiThread(new Runnable() {
//            public void run() {
//                setTitle(title);
//            }
//        });
//    }

    private BroadcastReceiver mySyncUpdateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            myFBReaderApp.useSyncInfo(myResumeTimestamp + 10 * 1000 > System.currentTimeMillis());
        }
    };

    public void outlineRegion(ZLTextRegion.Soul soul) {
        myFBReaderApp.getTextView().outlineRegion(soul);
        myFBReaderApp.getViewWidget().repaint();
    }

    public void hideOutline() {
        myFBReaderApp.getTextView().hideOutline();
        myFBReaderApp.getViewWidget().repaint();
    }
    public void setting() {
        ((SettingPopup) myFBReaderApp.getPopupById(SettingPopup.ID)).runNavigation();
    }

}
