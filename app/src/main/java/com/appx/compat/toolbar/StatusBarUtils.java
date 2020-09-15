package com.appx.compat.toolbar;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by Edgar on 2020/9/14.
 */
public class StatusBarUtils {

    private static final String TAG = "StatusBarUtils";
    private static final int OPPO_SYSTEM_UI_FLAG_STATUS_BAR_TINT = 0x00000010;
    private static final String STATUS_BAR_VIEW_TAG = "STATUS_BAR_VIEW";

    private StatusBarUtils() {}

    /**
     * 设置沉浸式
     * @param window window
     * @param isLightMode 是否为light mode
     */
    public static void tintStatusBar(Window window, boolean isLightMode) {
        View decorView = window.getDecorView();
        addStatusBarView((ViewGroup) decorView);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility()|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setLightStatusBar(window, isLightMode);
    }

    public static void setLightStatusBar(Window window, boolean isLightMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int vis = window.getDecorView().getSystemUiVisibility();
            if (isLightMode) {
                vis |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                vis &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            window.getDecorView().setSystemUiVisibility(vis);
        } else if (setFlymeLightStatusBar(window.getAttributes(), isLightMode)) {
            Log.d(TAG,"Flyme os setLightStatusBar");
        } else if (setMIUIStatusBarLightMode(window, isLightMode)) {
            Log.d(TAG,"MIUI os setLightStatusBar");
        } else {
            setColorOsStatusBarLight(window.getDecorView(), isLightMode);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    private static boolean setMIUIStatusBarLightMode(Window window, boolean isLightMode) {
        try {
            Class<? extends Window> clazz = window.getClass();
            int darkModeFlag;
            Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
            darkModeFlag = field.getInt(layoutParams);
            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            extraFlagField.invoke(window, isLightMode ? 0 : darkModeFlag, darkModeFlag);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * flyme适配
     */
    private static boolean setFlymeLightStatusBar(WindowManager.LayoutParams winParams, boolean isLightMode) {
        try {
            Field f = winParams.getClass().getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
            f.setAccessible(true);
            int bits = f.getInt(winParams);
            Field f2 = winParams.getClass().getDeclaredField("meizuFlags");
            f2.setAccessible(true);
            int meizuFlags = f2.getInt(winParams);
            int oldFlags = meizuFlags;
            if (isLightMode) {
                meizuFlags |= bits;
            } else {
                meizuFlags &= ~bits;
            }
            if (oldFlags != meizuFlags) {
                f2.setInt(winParams, meizuFlags);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Oppo light statusbar
     */
    private static void setColorOsStatusBarLight(View decorView, boolean isLightMode) {
        int vis = decorView.getSystemUiVisibility();
        if (isLightMode) {
            vis |= OPPO_SYSTEM_UI_FLAG_STATUS_BAR_TINT;
        } else {
            vis &= ~OPPO_SYSTEM_UI_FLAG_STATUS_BAR_TINT;
        }
        decorView.setSystemUiVisibility(vis);
    }

    private static void addStatusBarView(ViewGroup decorView) {
        if (decorView.findViewWithTag(STATUS_BAR_VIEW_TAG) == null) {
            ViewGroup parent = (ViewGroup) decorView.getChildAt(0);
            View view = new View(parent.getContext());
            view.setTag(STATUS_BAR_VIEW_TAG);
            view.setBackgroundColor(Color.TRANSPARENT);
            view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,getStatusBarHeight(parent.getContext())));
            parent.addView(view,0);
        }
    }

    public static int getStatusBarHeight(Context context) {
        int height = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            height = context.getResources().getDimensionPixelSize(resourceId);
        }
        return height;
    }
}
