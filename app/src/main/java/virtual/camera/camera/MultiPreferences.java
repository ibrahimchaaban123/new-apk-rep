package virtual.camera.camera;

import android.content.Context;
import android.content.SharedPreferences;

public class MultiPreferences {
    private static MultiPreferences sInstance;
    private SharedPreferences mPrefs;

    private MultiPreferences() {}

    public static MultiPreferences getInstance() {
        if (sInstance == null) {
            synchronized (MultiPreferences.class) {
                if (sInstance == null) sInstance = new MultiPreferences();
            }
        }
        return sInstance;
    }

    public static void init(Context context) {
        getInstance().mPrefs = context.getSharedPreferences("vcamera_prefs", Context.MODE_PRIVATE);
    }

    private SharedPreferences prefs() {
        return mPrefs;
    }

    public boolean getBoolean(String key, boolean def) {
        return prefs() != null ? prefs().getBoolean(key, def) : def;
    }

    public void setBoolean(String key, boolean value) {
        if (prefs() != null) prefs().edit().putBoolean(key, value).apply();
    }

    public int getInt(String key, int def) {
        return prefs() != null ? prefs().getInt(key, def) : def;
    }

    public void setInt(String key, int value) {
        if (prefs() != null) prefs().edit().putInt(key, value).apply();
    }

    public String getString(String key, String def) {
        return prefs() != null ? prefs().getString(key, def) : def;
    }

    public void setString(String key, String value) {
        if (prefs() != null) prefs().edit().putString(key, value).apply();
    }
}
