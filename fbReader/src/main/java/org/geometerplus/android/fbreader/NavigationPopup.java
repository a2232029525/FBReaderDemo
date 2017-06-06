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
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.orhanobut.logger.Logger;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.util.OrientationUtil;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.options.ColorProfile;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor;
import org.geometerplus.zlibrary.ui.android.R;

/**
 * 快速翻看
 */
final class NavigationPopup extends ZLApplication.PopupPanel {
    final static String ID = "NavigationPopup";

    private volatile NavigationWindow myWindow;
    private volatile FBReader myActivity;
    private volatile RelativeLayout myRoot;
    private ZLTextWordCursor myStartPosition;
    private final FBReaderApp myFBReader;
    private volatile boolean myIsInProgress;
    private ZLTextView.PagePosition pagePosition;
    private TextView light;
    private TextView dark;

    NavigationPopup(FBReaderApp fbReader) {
        super(fbReader);
        myFBReader = fbReader;
    }

    public void setPanelInfo(FBReader activity, RelativeLayout root) {
        myActivity = activity;
        myRoot = root;
    }

    public void runNavigation() {
        if (myWindow == null || myWindow.getVisibility() == View.GONE) {
            myIsInProgress = false;
            Application.showPopup(ID);
        }
    }

    @Override
    protected void show_() {
        setStatusBarVisibility(true);
        if (myActivity != null) {
            createPanel(myActivity, myRoot);
        }
        if (myWindow != null) {
            myWindow.show();
            setupNavigation();
        }
    }

    @Override
    protected void hide_() {
        setStatusBarVisibility(false);
        if (myWindow != null) {
            myWindow.hide();
        }
    }

    private void setStatusBarVisibility(boolean visible) {
        if (visible) {
            myActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); // 设置状态栏
        } else {
            myActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void update() {
        if (!myIsInProgress && myWindow != null) {
            setupNavigation();
        }
    }

    private void gotoPage(int page) {
        final ZLTextView view = myFBReader.getTextView();
        if (page == 1) {
            view.gotoHome();
        } else {
            view.gotoPage(page);
        }
        myFBReader.getViewWidget().reset();
        myFBReader.getViewWidget().repaint();
    }

    /**
     * 创建底部的设置菜单
     * @param activity    需要创建的activity
     * @param root  布局
     */
    private void createPanel(FBReader activity, RelativeLayout root) {
        if (myWindow != null && activity == myWindow.getContext()) {
            return;
        }

        activity.getLayoutInflater().inflate(R.layout.navigation_panel, root);
        myWindow = (NavigationWindow) root.findViewById(R.id.navigation_panel);

        final SeekBar slider = (SeekBar) myWindow.findViewById(R.id.navigation_slider);
        final TextView text = (TextView) myWindow.findViewById(R.id.navigation_text);
        final TextView toc = (TextView) myWindow.findViewById(R.id.navigation_toc);
        final TextView fonts = (TextView) myWindow.findViewById(R.id.navigation_fonts);
        final TextView search = (TextView) myWindow.findViewById(R.id.navigation_search);
        light = (TextView) myWindow.findViewById(R.id.navigation_light);
        dark = (TextView) myWindow.findViewById(R.id.navigation_dark);
        final TextView pre_character = (TextView) myWindow.findViewById(R.id.pre_character);
        final TextView next_character = (TextView) myWindow.findViewById(R.id.next_character);

        toc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Application.hideActivePopup();
                final Intent intent =
                        new Intent(myActivity.getApplicationContext(), TOCActivity.class);
                FBReaderIntents.putBookExtra(intent, myFBReader.getCurrentBook());
                FBReaderIntents.putBookmarkExtra(intent, myFBReader.createBookmark(80, true));
                OrientationUtil.startActivity(myActivity, intent);
            }
        });

        fonts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Application.hideActivePopup();
                ((SettingPopup) myFBReader.getPopupById(SettingPopup.ID)).runNavigation();
            }
        });
        /**
         * 这个是搜索所有的结果数据
         */
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.e("这里是搜索本书的数据");
                Application.hideActivePopup();
                myFBReader.runAction(ActionCode.SEARCH);
            }
        });

        dark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myFBReader.ViewOptions.ColorProfileName.setValue(ColorProfile.NIGHT);
                myFBReader.getViewWidget().reset();
                myFBReader.getViewWidget().repaint();
                light.setVisibility(View.VISIBLE);
                dark.setVisibility(View.INVISIBLE);
            }
        });

        light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dark.setVisibility(View.VISIBLE);
                light.setVisibility(View.INVISIBLE);
                myFBReader.ViewOptions.ColorProfileName.setValue(ColorProfile.DAY);
                myFBReader.getViewWidget().reset();
                myFBReader.getViewWidget().repaint();
            }
        });

        pre_character.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoPage(pagePosition.Current - 30);
            }
        });

        next_character.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoPage(pagePosition.Current + 30);
            }
        });


        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private void gotoPage(int page) {
                final ZLTextView view = myFBReader.getTextView();
                if (page == 1) {
                    view.gotoHome();
                } else {
                    view.gotoPage(page);
                }
            }

            private void gotoPagePer(int page) {
                final ZLTextView view = myFBReader.getTextView();
                view.gotoPageByPec(page);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                myIsInProgress = true;
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                myFBReader.getViewWidget().reset();
                myFBReader.getViewWidget().repaint();
                myIsInProgress = false;
                //y 松手直接进行跳转
                if (myStartPosition != null &&
                        !myStartPosition.equals(myFBReader.getTextView().getStartCursor())) {
                    myFBReader.addInvisibleBookmark(myStartPosition);
                    myFBReader.storePosition();
                }
                myStartPosition = null;
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    gotoPagePer(progress);
                    text.setText(makeProgressTextPer(myFBReader.getTextView().pagePositionPec()));
                }
            }
        });
    }

    private void setupNavigation() {
        final SeekBar slider = (SeekBar) myWindow.findViewById(R.id.navigation_slider);
        final TextView text = (TextView) myWindow.findViewById(R.id.navigation_text);

        final ZLTextView textView = myFBReader.getTextView();
        pagePosition = textView.pagePosition();

        String progress = textView.pagePositionPec();

        slider.setMax(textView.pagePosition2());
        slider.setProgress(textView.pagePosition1());
        text.setText(makeProgressTextPer(progress));

    }


    private String makeProgressTextPer(String progress) {
        final StringBuilder builder = new StringBuilder();
        builder.append(progress);
        final TOCTree tocElement = myFBReader.getCurrentTOCElement();
        if (tocElement != null) {
            builder.append("  ");
            builder.append(tocElement.getText());
        }

        if (myFBReader.ViewOptions.ColorProfileName.getValue().equals(ColorProfile.DAY)) {
            dark.setVisibility(View.VISIBLE);
        } else {
            light.setVisibility(View.VISIBLE);
        }

        return builder.toString();
    }

    final void removeWindow(Activity activity) {
        if (myWindow != null && activity == myWindow.getContext()) {
            final ViewGroup root = (ViewGroup) myWindow.getParent();
            myWindow.hide();
            root.removeView(myWindow);
            myWindow = null;
        }
    }
}
