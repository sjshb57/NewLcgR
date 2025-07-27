package top.easelink.lcg.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;
import java.util.Objects;

import top.easelink.lcg.appinit.LCGApp;

import static top.easelink.lcg.account.UserSPConstantsKt.SP_COOKIE;
import static top.easelink.lcg.account.UserSPConstantsKt.SP_USER;

public class SharedPreferencesHelper {

    private static final int VALUE_TYPE_STRING = 0;
    private static final int VALUE_TYPE_INT = 1;
    private static final int VALUE_TYPE_LONG = 2;
    private static final int VALUE_TYPE_FLOAT = 3;
    private static final int VALUE_TYPE_BOOLEAN = 4;

    public static SharedPreferences getUserSp() {
        return LCGApp.getContext().getSharedPreferences(SP_USER, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getCookieSp() {
        return LCGApp.getContext().getSharedPreferences(SP_COOKIE, Context.MODE_PRIVATE);
    }

    public static boolean isEmpty(SharedPreferences sp) {
        return sp == null || sp.getAll().isEmpty();
    }

    public static String getString(SharedPreferences sp, String key) {
        return sp.getString(key, "");
    }

    public static String getString(SharedPreferences sp, String key, String defValue) {
        return sp.getString(key, defValue);
    }

    public static void setPreferenceWithList(SharedPreferences sp, List<SpItem<?>> spItemList) {
        if (sp == null || spItemList == null || spItemList.isEmpty()) {
            return;
        }
        SharedPreferences.Editor spEditor = sp.edit();
        for (SpItem<?> item : spItemList) {
            setSpItem(spEditor, item);
        }
        spEditor.apply();
    }

    public static void commitPreferenceWithList(SharedPreferences sp, List<SpItem<?>> spItemList) {
        if (sp == null || spItemList == null || spItemList.isEmpty()) {
            return;
        }
        SharedPreferences.Editor spEditor = sp.edit();
        for (SpItem<?> item : spItemList) {
            setSpItem(spEditor, item);
        }
        spEditor.apply();
    }

    private static void setSpItem(SharedPreferences.Editor spEditor, SpItem<?> spItem) {
        if (spItem == null || spEditor == null) {
            return;
        }
        switch (spItem.mValueType) {
            case VALUE_TYPE_STRING:
                spEditor.putString(spItem.mKey, (String) spItem.mValue);
                break;
            case VALUE_TYPE_INT:
                spEditor.putInt(spItem.mKey, (Integer) spItem.mValue);
                break;
            case VALUE_TYPE_LONG:
                spEditor.putLong(spItem.mKey, (Long) spItem.mValue);
                break;
            case VALUE_TYPE_FLOAT:
                spEditor.putFloat(spItem.mKey, (Float) spItem.mValue);
                break;
            case VALUE_TYPE_BOOLEAN:
                spEditor.putBoolean(spItem.mKey, (Boolean) spItem.mValue);
                break;
        }
    }

    public static final class SpItem<T> {
        private final String mKey;
        private final T mValue;
        private final int mValueType;

        public SpItem(String key, T value) {
            this.mKey = Objects.requireNonNull(key);
            this.mValue = Objects.requireNonNull(value);
            this.mValueType = initValueType(value);
        }

        private int initValueType(T t) {
            if (t instanceof String) {
                return VALUE_TYPE_STRING;
            } else if (t instanceof Integer) {
                return VALUE_TYPE_INT;
            } else if (t instanceof Long) {
                return VALUE_TYPE_LONG;
            } else if (t instanceof Float) {
                return VALUE_TYPE_FLOAT;
            } else if (t instanceof Boolean) {
                return VALUE_TYPE_BOOLEAN;
            }
            throw new IllegalArgumentException("Unsupported value type");
        }
    }
}