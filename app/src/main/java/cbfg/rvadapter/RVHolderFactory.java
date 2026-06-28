package cbfg.rvadapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class RVHolderFactory {
    public abstract RVHolder<? extends Object> createViewHolder(ViewGroup parent, int viewType, Object item);

    public View inflate(int layoutRes, ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
    }
}
