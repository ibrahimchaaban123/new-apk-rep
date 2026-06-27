package virtual.camera.app.util;

import android.os.Build;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AbiUtils {
    private final Set<String> mLibs = new HashSet<>();

    /** LRU cache — keeps at most MAX_CACHE entries to avoid unbounded memory growth. */
    private static final int MAX_CACHE = 200;
    private static final Map<File, AbiUtils> sAbiUtilsMap =
            new LinkedHashMap<File, AbiUtils>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<File, AbiUtils> eldest) {
                    return size() > MAX_CACHE;
                }
            };

    public static boolean isSupport(File apkFile) {
        if (apkFile == null || !apkFile.exists()) return false;
        synchronized (sAbiUtilsMap) {
            AbiUtils abiUtils = sAbiUtilsMap.get(apkFile);
            if (abiUtils == null) {
                abiUtils = new AbiUtils(apkFile);
                sAbiUtilsMap.put(apkFile, abiUtils);
            }
            if (abiUtils.isEmptyAib()) return true;
            return AbiCore.is64Bit() ? abiUtils.is64Bit() : abiUtils.is32Bit();
        }
    }

    public AbiUtils(File apkFile) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(apkFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String name = zipEntry.getName();
                if (name.startsWith("lib/arm64-v8a")) {
                    mLibs.add("arm64-v8a");
                } else if (name.startsWith("lib/armeabi-v7a")) {
                    mLibs.add("armeabi-v7a");
                } else if (name.startsWith("lib/armeabi")) {
                    mLibs.add("armeabi");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(zipFile);
        }
    }

    public boolean is64Bit()    { return mLibs.contains("arm64-v8a"); }
    public boolean is32Bit()    { return mLibs.contains("armeabi") || mLibs.contains("armeabi-v7a"); }
    public boolean isEmptyAib() { return mLibs.isEmpty(); }

    public static void close(Closeable... closeables) {
        if (closeables == null) return;
        for (Closeable c : closeables) {
            if (c != null) {
                try { c.close(); } catch (IOException ignored) {}
            }
        }
    }
}
