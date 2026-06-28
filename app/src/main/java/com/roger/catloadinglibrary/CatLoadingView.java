package com.roger.catloadinglibrary;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;

public class CatLoadingView extends DialogFragment {
    private boolean clickCancelable = true;
    private int bgColor = Color.WHITE;

    public CatLoadingView() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ProgressBar pb = new ProgressBar(getContext());
        pb.setIndeterminate(true);
        return pb;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog d = super.onCreateDialog(savedInstanceState);
        if (d.getWindow() != null) d.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        d.setCancelable(clickCancelable);
        d.setCanceledOnTouchOutside(clickCancelable);
        return d;
    }

    public void show(FragmentManager manager, String tag) {
        try { super.show(manager, tag); } catch (Exception ignored) {}
    }

    public void setBackgroundColor(int colorRes) {
        this.bgColor = colorRes;
    }

    public void setClickCancelAble(boolean cancelable) {
        this.clickCancelable = cancelable;
        Dialog d = getDialog();
        if (d != null) {
            d.setCancelable(cancelable);
            d.setCanceledOnTouchOutside(cancelable);
        }
    }
}
