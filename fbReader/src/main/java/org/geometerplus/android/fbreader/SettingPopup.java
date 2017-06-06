package org.geometerplus.android.fbreader;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.geometerplus.android.fbreader.preferences.PreferenceActivity;
import org.geometerplus.android.util.OrientationUtil;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.ui.android.R;

import de.hdodenhof.circleimageview.CircleImageView;


final class SettingPopup extends ZLApplication.PopupPanel implements View.OnClickListener {
    final static String ID = "SettingPopup";

    private volatile SettingWindow myWindow;
    private volatile FBReader myActivity;
    private volatile RelativeLayout myRoot;
    private boolean myIsBrightnessAdjustmentInProgress;
    private final FBReaderApp myFBReader;
    private TextView tvFontMinus, tvFontSize, tvFontAdd;
    private TextView tvLightMinus, tvLightAdd;
    private TextView tvLineSpaceAdd, tvLineSpaceSize, tvLineSpaceMinus;
    private ZLIntegerRangeOption integerRangeOption;
    private SeekBar slider;
    private TextView tvPageMode, tvSetting;
    private TextView tvPageSimulation, tvPageCover, tvPageSlide, tvPageNone;
    private CircleImageView bgWhite, bgGrey, bgVineGrey, bgVineWhite, bgGreen, bgNight;
    private TextView tvAlignLeft, tvAlignRight, tvAlignCenter, tvAlign;
    private boolean pageMode;

    SettingPopup(FBReaderApp FBReader) {
        super(FBReader);
        myFBReader = FBReader;
    }

    public void setPanelInfo(FBReader activity, RelativeLayout root) {
        myActivity = activity;
        myRoot = root;
    }

    public void runNavigation() {
        if (myWindow == null || myWindow.getVisibility() == View.GONE) {
            Application.showPopup(ID);
        }
    }

    @Override
    protected void show_() {
        myActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        if (myActivity != null) {
            createPanel(myActivity, myRoot);
        }
        if (myWindow != null) {
            myWindow.show();
            setupLight();
        }
    }

    @Override
    protected void hide_() {
        if (myWindow != null) {
            myWindow.hide();
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void update() {
        if (myWindow != null) {
            setupLight();
        }
    }

    private void createPanel(FBReader activity, RelativeLayout root) {
        if (myWindow != null && activity == myWindow.getContext()) {
            return;
        }
        activity.getLayoutInflater().inflate(R.layout.setting_panel, root);
        myWindow = (SettingWindow) root.findViewById(R.id.setting_panel);
        integerRangeOption = myFBReader.ViewOptions.getTextStyleCollection().getBaseStyle().FontSizeOption; // 字体
        pageMode = myFBReader.PageTurningOptions.Horizontal.getValue(); // 当前翻页方式
        tvFontSize = (TextView) myWindow.findViewById(R.id.tv_font_size);
        tvLineSpaceSize = (TextView) myWindow.findViewById(R.id.tv_lineSpace_size);
        tvPageMode = (TextView) myWindow.findViewById(R.id.tv_page_mode);
        if (pageMode) {
            tvPageMode.setText("上下");
        } else {
            tvPageMode.setText("左右");
        }
        updateFontSize(); // 设置当前字号
        updateLineSpaceSize(); // 当前行间距
        slider = (SeekBar) myWindow.findViewById(R.id.light_slider);
        tvLightMinus = (TextView) myWindow.findViewById(R.id.tv_light_minus);
        tvLightAdd = (TextView) myWindow.findViewById(R.id.tv_light_add);
        tvFontMinus = (TextView) myWindow.findViewById(R.id.tv_font_minus);
        tvFontAdd = (TextView) myWindow.findViewById(R.id.tv_font_add);
        tvLineSpaceAdd = (TextView) myWindow.findViewById(R.id.tv_lineSpace_add);
        tvLineSpaceMinus = (TextView) myWindow.findViewById(R.id.tv_lineSpace_minus);
        tvSetting = (TextView) myWindow.findViewById(R.id.tv_setting);
        tvPageSimulation = (TextView) myWindow.findViewById(R.id.tv_page_simulation);
        tvPageCover = (TextView) myWindow.findViewById(R.id.tv_page_cover);
        tvPageSlide = (TextView) myWindow.findViewById(R.id.tv_page_slide);
        tvPageNone = (TextView) myWindow.findViewById(R.id.tv_page_none);
        bgWhite = (CircleImageView) myWindow.findViewById(R.id.bg_white);
        bgGrey = (CircleImageView) myWindow.findViewById(R.id.bg_grey);
        bgVineGrey = (CircleImageView) myWindow.findViewById(R.id.bg_vine_grey);
        bgVineWhite = (CircleImageView) myWindow.findViewById(R.id.bg_vine_white);
        bgGreen = (CircleImageView) myWindow.findViewById(R.id.bg_green);
        bgNight = (CircleImageView) myWindow.findViewById(R.id.bg_night);
        tvAlignLeft = (TextView) myWindow.findViewById(R.id.tv_align_left);
        tvAlignRight = (TextView) myWindow.findViewById(R.id.tv_align_right);
        tvAlignCenter = (TextView) myWindow.findViewById(R.id.tv_align_center);
        tvAlign = (TextView) myWindow.findViewById(R.id.tv_align);
        tvAlignLeft.setOnClickListener(this);
        tvAlignRight.setOnClickListener(this);
        tvAlignCenter.setOnClickListener(this);
        tvAlign.setOnClickListener(this);
        bgWhite.setOnClickListener(this);
        bgGrey.setOnClickListener(this);
        bgVineGrey.setOnClickListener(this);
        bgVineWhite.setOnClickListener(this);
        bgGreen.setOnClickListener(this);
        bgNight.setOnClickListener(this);
        tvLightMinus.setOnClickListener(this);
        tvLightAdd.setOnClickListener(this);
        tvFontMinus.setOnClickListener(this);
        tvFontAdd.setOnClickListener(this);
        tvLineSpaceMinus.setOnClickListener(this);
        tvLineSpaceAdd.setOnClickListener(this);
        tvPageMode.setOnClickListener(this);
        tvSetting.setOnClickListener(this);
        tvPageSimulation.setOnClickListener(this);
        tvPageCover.setOnClickListener(this);
        tvPageSlide.setOnClickListener(this);
        tvPageNone.setOnClickListener(this);
        selectePurItem(); //y 当前翻页方式
        updateAlignment(); //y 当前对齐方式

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seekBar) {
                myIsBrightnessAdjustmentInProgress = true;
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (myIsBrightnessAdjustmentInProgress) {
                    myIsBrightnessAdjustmentInProgress = false;
                }
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (myIsBrightnessAdjustmentInProgress) {
                        myFBReader.getViewWidget().setScreenBrightness(progress);
                        return;
                    }
                }
            }
        });
    }

    public void updateFontSize() {
        tvFontSize.setText(integerRangeOption.getValue() + "");
    }

    public void updateLineSpaceSize() {
        tvLineSpaceSize.setText(myFBReader.ViewOptions.getTextStyleCollection().getBaseStyle().LineSpaceOption.getValue() + "");
    }

    public void updateAlignment() {
        int style = myFBReader.ViewOptions.getTextStyleCollection().getBaseStyle().AlignmentOption.getValue();
        switch (style) {
            case 1:
                tvAlignLeft.setBackgroundResource(R.drawable.es);
                return;
            case 2:
                tvAlignRight.setBackgroundResource(R.drawable.eq);
                return;
            case 3:
                tvAlignCenter.setBackgroundResource(R.drawable.ew);
                return;
            case 4:
                tvAlign.setBackgroundResource(R.drawable.eo);
                return;
        }
    }

    @Override
    public void onClick(View v) {

        int i = v.getId();
        if (i == R.id.tv_light_minus) {
            slider.setProgress(slider.getProgress() - 2);
            myFBReader.getViewWidget().setScreenBrightness(slider.getProgress() - 2);
            return;
        } else if (i == R.id.tv_light_add) {
            slider.setProgress(slider.getProgress() + 2);
            myFBReader.getViewWidget().setScreenBrightness(slider.getProgress() + 2);
            return;
        } else if (i == R.id.tv_font_add) {
            integerRangeOption.setValue(integerRangeOption.getValue() + 2);
            myFBReader.clearTextCaches();
            myFBReader.getViewWidget().repaint();
            updateFontSize();
            return;
        } else if (i == R.id.tv_font_minus) {
            integerRangeOption.setValue(integerRangeOption.getValue() - 2);
            myFBReader.clearTextCaches();
            myFBReader.getViewWidget().repaint();
            updateFontSize();
            return;
        } else if (i == R.id.tv_lineSpace_add) {
            myFBReader.ViewOptions.getTextStyleCollection().getBaseStyle().LineSpaceOption.setValue(myFBReader.ViewOptions.getTextStyleCollection().getBaseStyle().LineSpaceOption.getValue() + 1);
            myFBReader.clearTextCaches();
            myFBReader.getViewWidget().repaint();
            updateLineSpaceSize();
            return;
        } else if (i == R.id.tv_lineSpace_minus) {
            myFBReader.ViewOptions.getTextStyleCollection().getBaseStyle().LineSpaceOption.setValue(myFBReader.ViewOptions.getTextStyleCollection().getBaseStyle().LineSpaceOption.getValue() - 1);
            myFBReader.clearTextCaches();
            myFBReader.getViewWidget().repaint();
            updateLineSpaceSize();
            return;
        } else if (i == R.id.tv_page_mode) {
            boolean flag = myFBReader.PageTurningOptions.Horizontal.getValue();
            if (flag) {
                myFBReader.PageTurningOptions.Horizontal.setValue(false);
                tvPageMode.setText("左右");
            } else {
                myFBReader.PageTurningOptions.Horizontal.setValue(true);
                tvPageMode.setText("上下");
            }
            return;
        } else if (i == R.id.tv_setting) {
            Application.hideActivePopup();
            final Intent intent = new Intent(myActivity.getApplicationContext(), PreferenceActivity.class);
            OrientationUtil.startActivityForResult(myActivity, intent, FBReader.REQUEST_PREFERENCES);
            return;
        } else if (i == R.id.tv_page_simulation) {
            clearSelecte();
            tvPageSimulation.setTextColor(0xC1e8554d);
            tvPageSimulation.setBackgroundResource(R.drawable.page_redline);
            myFBReader.PageTurningOptions.Animation.setValue(ZLView.Animation.curl);
            return;
        } else if (i == R.id.tv_page_cover) {
            clearSelecte();
            tvPageCover.setTextColor(0xC1e8554d);
            tvPageCover.setBackgroundResource(R.drawable.page_redline);
            myFBReader.PageTurningOptions.Animation.setValue(ZLView.Animation.slide);
            return;
        } else if (i == R.id.tv_page_slide) {
            clearSelecte();
            tvPageSlide.setTextColor(0xC1e8554d);
            tvPageSlide.setBackgroundResource(R.drawable.page_redline);
            myFBReader.PageTurningOptions.Animation.setValue(ZLView.Animation.shift);
            return;
        } else if (i == R.id.tv_page_none) {
            clearSelecte();
            tvPageNone.setTextColor(0xC1e8554d);
            tvPageNone.setBackgroundResource(R.drawable.page_redline);
            myFBReader.PageTurningOptions.Animation.setValue(ZLView.Animation.none);
            return;
        } else if (i == R.id.bg_white) {
            setWallpaper("wallpapers/bg_white.png");
            return;
        } else if (i == R.id.bg_grey) {
            setWallpaper("wallpapers/bg_grey.png");
            return;
        } else if (i == R.id.bg_vine_grey) {
            setWallpaper("wallpapers/bg_vine_grey.png");
            return;
        } else if (i == R.id.bg_vine_white) {
            setWallpaper("wallpapers/bg_vine_white.png");
            return;
        } else if (i == R.id.bg_green) {
            setWallpaper("wallpapers/bg_green.png");
            return;
        } else if (i == R.id.bg_night) {
            setWallpaper("wallpapers/bg_night.png");
            return;
        } else if (i == R.id.tv_align_left) {
            setAlign(1);
            clearAlignSelecte();
            tvAlignLeft.setBackgroundResource(R.drawable.es);
            return;
        } else if (i == R.id.tv_align_right) {
            setAlign(2);
            clearAlignSelecte();
            tvAlignRight.setBackgroundResource(R.drawable.eq);
            return;
        } else if (i == R.id.tv_align) {
            setAlign(4);
            clearAlignSelecte();
            tvAlign.setBackgroundResource(R.drawable.eo);
            return;
        } else if (i == R.id.tv_align_center) {
            setAlign(3);
            clearAlignSelecte();
            tvAlignCenter.setBackgroundResource(R.drawable.ew);
            return;
        }
    }

    private void setAlign(final int aligStyle) {
        myFBReader.ViewOptions.getTextStyleCollection().getBaseStyle().AlignmentOption.setValue(aligStyle);
        myFBReader.getViewWidget().reset();
        myFBReader.getViewWidget().repaint();
    }

    private void setWallpaper(final String wallpaper) {
        myFBReader.ViewOptions.getColorProfile().WallpaperOption.setValue(wallpaper);
        myFBReader.getViewWidget().reset();
        myFBReader.getViewWidget().repaint();
    }

    private void clearSelecte() {
        tvPageSimulation.setTextColor(0xFFFFFFFF);
        tvPageSimulation.setBackgroundResource(R.drawable.page_whiteline);
        tvPageCover.setTextColor(0xFFFFFFFF);
        tvPageCover.setBackgroundResource(R.drawable.page_whiteline);
        tvPageSlide.setTextColor(0xFFFFFFFF);
        tvPageSlide.setBackgroundResource(R.drawable.page_whiteline);
        tvPageNone.setTextColor(0xFFFFFFFF);
        tvPageNone.setBackgroundResource(R.drawable.page_whiteline);
    }

    private void clearAlignSelecte() {
        tvAlignLeft.setBackgroundResource(R.drawable.er);
        tvAlignRight.setBackgroundResource(R.drawable.ep);
        tvAlign.setBackgroundResource(R.drawable.en);
        tvAlignCenter.setBackgroundResource(R.drawable.ev);
    }

    private void setupLight() {
        final SeekBar slider = (SeekBar) myWindow.findViewById(R.id.light_slider); //y 屏幕亮度 1~100
        slider.setMax(100);
        slider.setProgress(myFBReader.getViewWidget().getScreenBrightness());
    }

    final void removeWindow(Activity activity) {
        if (myWindow != null && activity == myWindow.getContext()) {
            final ViewGroup root = (ViewGroup) myWindow.getParent();
            myWindow.hide();
            root.removeView(myWindow);
            myWindow = null;
        }
    }

    private void selectePurItem() {
        switch (myFBReader.PageTurningOptions.Animation.getValue().toString()) {
            case "curl":
                tvPageSimulation.setTextColor(0xC1e8554d);
                tvPageSimulation.setBackgroundResource(R.drawable.page_redline);
                return;
            case "slide":
                tvPageCover.setTextColor(0xC1e8554d);
                tvPageCover.setBackgroundResource(R.drawable.page_redline);
                return;
            case "shift":
                tvPageSlide.setTextColor(0xC1e8554d);
                tvPageSlide.setBackgroundResource(R.drawable.page_redline);
                return;
            case "none":
                tvPageNone.setTextColor(0xC1e8554d);
                tvPageNone.setBackgroundResource(R.drawable.page_redline);
                return;
        }
    }
}