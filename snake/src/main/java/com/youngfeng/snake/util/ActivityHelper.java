package com.youngfeng.snake.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Activity utils
 *
 * @author Scott Smith 2017-12-17 17:00
 */
public class ActivityHelper {

    public static void convertFromTranslucent(Activity activity) {
        try {
            @SuppressLint("PrivateApi")
            Method method = Activity.class.getDeclaredMethod("convertFromTranslucent");
            method.setAccessible(true);
            method.invoke(activity);
        } catch (Throwable e) {
        }
    }

    public static void convertToTranslucent(@NonNull Activity activity, TranslucentConversionListener listener) {
        if (activity.isFinishing()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            convertToTranslucentAfterLollipop(activity, listener);
        }
    }

    public static void convertToTranslucent(@NonNull Activity activity) {
        convertToTranslucent(activity, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private static void convertToTranslucentAfterLollipop(@NonNull Activity activity, TranslucentConversionListener conversionListener) {
        try {
            @SuppressLint("PrivateApi")
            Method getActivityOptions = Activity.class.getDeclaredMethod("getActivityOptions");
            getActivityOptions.setAccessible(true);
            Object options = getActivityOptions.invoke(activity);

            Class<?>[] classes = Activity.class.getDeclaredClasses();
            Class<?> translucentConversionListenerClazz = null;
            for (Class clazz : classes) {
                if (clazz.getSimpleName().contains("TranslucentConversionListener")) {
                    translucentConversionListenerClazz = clazz;
                    break;
                }
            }

            ConversionInvocationHandler invocationHandler = new ConversionInvocationHandler(new WeakReference<TranslucentConversionListener>(conversionListener));
            Object newProxy = Proxy.newProxyInstance(Activity.class.getClassLoader(), new Class<?>[]{translucentConversionListenerClazz}, invocationHandler);

            @SuppressLint("PrivateApi")
            Method convertToTranslucent = Activity.class.getDeclaredMethod("convertToTranslucent",
                    translucentConversionListenerClazz, ActivityOptions.class);
            convertToTranslucent.setAccessible(true);
            convertToTranslucent.invoke(activity, newProxy, options);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static int getWindowBackgroundResourceId(@NonNull Activity activity) {
        TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.windowBackground
        });
        final int backgroundResourceId = a.getResourceId(0, 0);
        a.recycle();

        return backgroundResourceId;
    }

    public static void setWindowTranslucent(@NonNull final Activity activity, boolean translucent, final TranslucentConversionListener translucentConversionListener) {
        if (translucent) {
            convertToTranslucent(activity, new TranslucentConversionListener() {
                @Override
                public void onTranslucentConversionComplete(boolean drawComplete) {
                    ActivityManager.get().setWindowTranslucent(activity, drawComplete);

                    if (null != translucentConversionListener) {
                        translucentConversionListener.onTranslucentConversionComplete(drawComplete);
                    }
                }
            });
        } else {
            int resourceId = ActivityManager.get().getBackgroundResourceId(activity);
            if (resourceId > 0) {
                activity.getWindow().setBackgroundDrawableResource(resourceId);
            }

            convertFromTranslucent(activity);
            ActivityManager.get().setWindowTranslucent(activity, false);
        }
    }

    public static boolean isTranslucent(@NonNull Activity activity) {
        try {
            TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]{
                    android.R.attr.windowIsTranslucent
            });
            boolean isTranslucent = a.getBoolean(0, false);
            a.recycle();

            return isTranslucent;
        } catch (Throwable e) {
            return false;
        }
    }
}
