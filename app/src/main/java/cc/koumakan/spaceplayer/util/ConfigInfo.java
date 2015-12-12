package cc.koumakan.spaceplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

/**
 * Created by Remilia Scarlet
 * on 2015/12/7 15:45.
 * <br>
 * 设置信息类，用sharepreference保存信息
 */
public class ConfigInfo {
    private static final String sp_author = "Space Player Develop Team";
    private static final String sp_developer = "Remilia Scarlet\nLiHQ";

    private static SharedPreferences sharedPreferences;
    private static Editor editor;


    /**
     * 初始化时读取去所有信息
     *
     * @param context  上下文参数
     * @param listener 更改时的回掉函数
     */
    public static void init(Context context, OnSharedPreferenceChangeListener listener) {
        sharedPreferences = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.apply();
        if (null != listener) sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * 更改设置
     */
    public static void set(String key, String value) {
        editor.putString(key, value);
        editor.commit();
    }

    /**
     * 读取设置
     */
    public static String get(String key) {
        return sharedPreferences.getString(key, null);
    }

    /**
     * 系统信息获取
     */
    public static String sget(String key) {
        if (key.equals("author"))
            return sp_author;
        else if (key.equals("developer"))
            return sp_developer;
        else
            return null;
    }
}
