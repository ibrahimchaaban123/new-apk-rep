package cbfg.rvadapter;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

public abstract class RVHolder<T> extends RecyclerView.ViewHolder {
    public RVHolder(View itemView) {
        super(itemView);
    }
    public abstract void setContent(T item, boolean isSelected, Object payload);
}
