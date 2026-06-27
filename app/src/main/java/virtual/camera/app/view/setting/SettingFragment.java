package virtual.camera.app.view.setting;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import virtual.camera.app.R;
import virtual.camera.app.app.App;
import virtual.camera.app.settings.LogUtil;
import virtual.camera.app.settings.MethodType;
import virtual.camera.camera.MultiPreferences;
import virtual.camera.app.util.AppUtil;
import virtual.camera.app.util.HandlerUtil;
import virtual.camera.app.util.ToastUtils;

public class SettingFragment extends BaseFragment {

    // ── Quality preset constants ──────────────────────────────────────────────
    public static final int QUALITY_LOW      = 0;
    public static final int QUALITY_MEDIUM   = 1;
    public static final int QUALITY_HIGH     = 2;
    public static final int QUALITY_ORIGINAL = 3;

    /** Preferred output widths for each preset. Height is derived by the engine. */
    private static final int[] QUALITY_WIDTHS  = {640,  1280, 1920, 0};
    private static final int[] QUALITY_HEIGHTS = {360,  720,  1080, 0};
    /** Target bitrates in kbps (0 = let the engine decide / keep original). */
    private static final int[] QUALITY_BITRATES = {800, 2500, 8000, 0};

    // ── Views ─────────────────────────────────────────────────────────────────
    private AppCompatButton    mProtectMethodBtn, mSave;
    private AppCompatTextView  mProtectMethodText, mTip, mAudioText;
    private AppCompatEditText  mInput;
    private SwitchCompat       mAudioSwitch;
    private AppCompatButton    mChoiseVideo;
    private AppCompatTextView  mQualityLabel;
    private AppCompatButton    mQualityBtn;
    private PopupMenu          mPopupMenu    = null;
    private PopupMenu          mQualityMenu  = null;

    private int mMethodType  = 0;
    private int mQualityType = QUALITY_ORIGINAL;
    private boolean mHasOpenDocuments = false;

    private static final int BUFFER_SIZE = 65536;

    // ── Permission launcher ───────────────────────────────────────────────────
    private final ActivityResultLauncher<String[]> permissionResult =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ? Manifest.permission.READ_MEDIA_VIDEO
                        : Manifest.permission.READ_EXTERNAL_STORAGE;
                Boolean granted = result.getOrDefault(perm, false);
                if (Boolean.TRUE.equals(granted)) {
                    openDocumentedResult.launch("video/*");
                } else {
                    ToastUtils.showToast("Storage permission is required to select a video.");
                }
            });

    private final ActivityResultLauncher<String> openDocumentedResult =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onVideoChoiseDone);

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    public void onVideoChoiseDone(Uri video) {
        if (video == null) return;
        mInput.setText(video.toString());
        mHasOpenDocuments = true;
        LogUtil.log("onVideoChoiseDone:" + video);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_camera_settings, container, false);
        initView(view);
        return view;
    }

    // ── initView ──────────────────────────────────────────────────────────────
    private void initView(View rootView) {
        mProtectMethodBtn  = rootView.findViewById(R.id.protect_method_btn);
        mSave              = rootView.findViewById(R.id.protect_save);
        mProtectMethodText = rootView.findViewById(R.id.protect_method_text);
        mTip               = rootView.findViewById(R.id.protect_tip);
        mInput             = rootView.findViewById(R.id.protect_path);
        mAudioText         = rootView.findViewById(R.id.protect_audio);
        mAudioSwitch       = rootView.findViewById(R.id.protect_audio_switch);
        mChoiseVideo       = rootView.findViewById(R.id.protect_video_select);
        mQualityLabel      = rootView.findViewById(R.id.protect_quality_label);
        mQualityBtn        = rootView.findViewById(R.id.protect_quality_btn);

        mChoiseVideo.setOnClickListener(v -> requestStorageAndPickVideo());

        mProtectMethodBtn.setOnClickListener(view -> {
            mPopupMenu = new PopupMenu(getActivitySafe(), view);
            mPopupMenu.inflate(R.menu.camera_menu);
            mPopupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.protect_method_disable_camera) {
                    onMethodTypeClick(MethodType.TYPE_DISABLE_CAMERA);
                } else if (id == R.id.protect_method_local) {
                    onMethodTypeClick(MethodType.TYPE_LOCAL_VIDEO);
                } else if (id == R.id.protect_method_network) {
                    onMethodTypeClick(MethodType.TYPE_NETWORK_VIDEO);
                }
                return true;
            });
            mPopupMenu.show();
        });

        mQualityBtn.setOnClickListener(view -> showQualityMenu(view));

        mSave.setOnClickListener(view -> saveSettings());

        // Restore saved state
        int method_type = MultiPreferences.getInstance().getInt("method_type", MethodType.TYPE_DISABLE_CAMERA);
        mQualityType = MultiPreferences.getInstance().getInt("video_quality", QUALITY_ORIGINAL);
        updateQualityButtonLabel();
        if (method_type > 0) {
            onMethodTypeClick(method_type);
        }
    }

    // ── Quality menu ──────────────────────────────────────────────────────────
    private void showQualityMenu(View anchor) {
        mQualityMenu = new PopupMenu(getActivitySafe(), anchor);
        mQualityMenu.getMenu().add(0, QUALITY_LOW,      0, R.string.video_quality_low);
        mQualityMenu.getMenu().add(0, QUALITY_MEDIUM,   1, R.string.video_quality_medium);
        mQualityMenu.getMenu().add(0, QUALITY_HIGH,     2, R.string.video_quality_high);
        mQualityMenu.getMenu().add(0, QUALITY_ORIGINAL, 3, R.string.video_quality_original);
        mQualityMenu.setOnMenuItemClickListener(item -> {
            mQualityType = item.getItemId();
            updateQualityButtonLabel();
            return true;
        });
        mQualityMenu.show();
    }

    private void updateQualityButtonLabel() {
        if (mQualityBtn == null) return;
        int labelRes;
        switch (mQualityType) {
            case QUALITY_LOW:      labelRes = R.string.video_quality_low;      break;
            case QUALITY_MEDIUM:   labelRes = R.string.video_quality_medium;   break;
            case QUALITY_HIGH:     labelRes = R.string.video_quality_high;     break;
            default:               labelRes = R.string.video_quality_original; break;
        }
        mQualityBtn.setText(labelRes);
    }

    // ── Storage permission helper ─────────────────────────────────────────────
    private void requestStorageAndPickVideo() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_VIDEO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            openDocumentedResult.launch("video/*");
        } else {
            permissionResult.launch(new String[]{permission});
        }
    }

    // ── URI → extension ───────────────────────────────────────────────────────
    private String getExtensionFromUri(Uri uri) {
        String mimeType = null;
        try {
            mimeType = getActivitySafe().getContentResolver().getType(uri);
        } catch (Throwable ignored) {}

        if (!TextUtils.isEmpty(mimeType)) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (!TextUtils.isEmpty(ext)) return "." + ext;
        }

        String path = uri.getPath();
        if (path != null) {
            int dot = path.lastIndexOf('.');
            if (dot >= 0 && dot < path.length() - 1) return path.substring(dot);
        }
        return ".mp4";
    }

    // ── Video copy ────────────────────────────────────────────────────────────
    private boolean copyLocalVideo(String u) {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            Uri uri = Uri.parse(u);

            // Delete previous copy
            String prevPath = MultiPreferences.getInstance()
                    .getString("video_path_local_final_out", "");
            if (!TextUtils.isEmpty(prevPath)) new File(prevPath).delete();

            String subfix  = getExtensionFromUri(uri);
            String outPath = App.getContext().getFilesDir() + "/video" + subfix;

            fos = new FileOutputStream(outPath);
            is  = getActivitySafe().getContentResolver().openInputStream(uri);
            if (is == null) throw new Exception("Cannot open input stream for URI: " + u);

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);   // ✓ write exactly len bytes, no corruption
            }
            fos.flush();

            // Save all settings including quality
            MultiPreferences.getInstance().setInt("method_type",    mMethodType);
            MultiPreferences.getInstance().setString("video_path_local", u);
            MultiPreferences.getInstance().setString("video_path_local_final_out", outPath);
            MultiPreferences.getInstance().setBoolean("video_path_local_audio_enable",
                    mAudioSwitch.isChecked());
            saveQualityPrefs();
            ToastUtils.showToast("Save Success...");
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            HandlerUtil.runOnMain(() -> mInput.setText(""));
            ToastUtils.showToast("Video handling failed: " + e.getMessage());
            return false;
        } finally {
            try { if (is  != null) is.close();  } catch (Throwable ignored) {}
            try { if (fos != null) fos.close(); } catch (Throwable ignored) {}
        }
    }

    /** Persists the current quality selection to MultiPreferences. */
    private void saveQualityPrefs() {
        MultiPreferences.getInstance().setInt("video_quality",          mQualityType);
        MultiPreferences.getInstance().setInt("video_target_width",     QUALITY_WIDTHS[mQualityType]);
        MultiPreferences.getInstance().setInt("video_target_height",    QUALITY_HEIGHTS[mQualityType]);
        MultiPreferences.getInstance().setInt("video_target_bitrate_k", QUALITY_BITRATES[mQualityType]);
    }

    // ── Method type switch ────────────────────────────────────────────────────
    private void onMethodTypeClick(int type) {
        mMethodType = type;
        switch (type) {
            case MethodType.TYPE_DISABLE_CAMERA:
                mProtectMethodText.setText(R.string.protect_method_disable_camera);
                mTip.setText(R.string.protect_tip_disable);
                mInput.setVisibility(View.GONE);
                mChoiseVideo.setVisibility(View.GONE);
                mAudioText.setVisibility(View.GONE);
                mAudioSwitch.setVisibility(View.GONE);
                mQualityLabel.setVisibility(View.GONE);
                mQualityBtn.setVisibility(View.GONE);
                break;

            case MethodType.TYPE_LOCAL_VIDEO:
                mProtectMethodText.setText(R.string.protect_method_local);
                mTip.setText(R.string.protect_tip_local);
                mInput.setVisibility(View.VISIBLE);
                mChoiseVideo.setVisibility(View.VISIBLE);
                mChoiseVideo.setEnabled(true);
                mChoiseVideo.setText(R.string.choise_video);
                mAudioText.setVisibility(View.VISIBLE);
                mAudioSwitch.setVisibility(View.VISIBLE);
                mQualityLabel.setVisibility(View.VISIBLE);
                mQualityBtn.setVisibility(View.VISIBLE);
                mInput.setHint("");
                mInput.setEnabled(false);
                mInput.setText(mHasOpenDocuments
                        ? MultiPreferences.getInstance().getString("video_path_local", "")
                        : "");
                mAudioSwitch.setChecked(MultiPreferences.getInstance()
                        .getBoolean("video_path_local_audio_enable", true));
                break;

            case MethodType.TYPE_NETWORK_VIDEO:
                mProtectMethodText.setText(R.string.protect_method_network);
                mTip.setText(R.string.protect_tip_network);
                mInput.setVisibility(View.VISIBLE);
                mChoiseVideo.setVisibility(View.GONE);
                mAudioText.setVisibility(View.VISIBLE);
                mAudioSwitch.setVisibility(View.VISIBLE);
                mQualityLabel.setVisibility(View.VISIBLE);
                mQualityBtn.setVisibility(View.VISIBLE);
                mInput.setHint(R.string.protect_path_hint);
                mInput.setEnabled(true);
                mInput.setText(MultiPreferences.getInstance()
                        .getString("video_path_network", ""));
                mAudioSwitch.setChecked(MultiPreferences.getInstance()
                        .getBoolean("video_path_network_audio_enable", true));
                break;
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    private void saveSettings() {
        AppUtil.killAllApps();
        switch (mMethodType) {
            case MethodType.TYPE_DISABLE_CAMERA:
                MultiPreferences.getInstance().setInt("method_type", mMethodType);
                ToastUtils.showToast("Save Success...");
                break;

            case MethodType.TYPE_LOCAL_VIDEO:
                if (TextUtils.isEmpty(mInput.getText())) {
                    ToastUtils.showToast("Video not set...");
                    return;
                }
                ProgressDialog progressDialog = new ProgressDialog(
                        getActivitySafe(), ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                progressDialog.setMessage("Handling video...");
                progressDialog.show();
                new Thread(() -> {
                    copyLocalVideo(mInput.getText().toString());
                    HandlerUtil.runOnMain(() -> {
                        try { progressDialog.dismiss(); } catch (Throwable e) { e.printStackTrace(); }
                    });
                }).start();
                break;

            case MethodType.TYPE_NETWORK_VIDEO:
                if (TextUtils.isEmpty(mInput.getText())) {
                    ToastUtils.showToast("Video not set...");
                    return;
                }
                if (!mInput.getText().toString().toLowerCase().startsWith("http")) {
                    ToastUtils.showToast("Video url should start with http or https");
                    return;
                }
                MultiPreferences.getInstance().setInt("method_type", mMethodType);
                MultiPreferences.getInstance().setString("video_path_network",
                        mInput.getText().toString());
                MultiPreferences.getInstance().setBoolean("video_path_network_audio_enable",
                        mAudioSwitch.isChecked());
                saveQualityPrefs();
                ToastUtils.showToast("Save Success...");
                break;
        }
    }
}
