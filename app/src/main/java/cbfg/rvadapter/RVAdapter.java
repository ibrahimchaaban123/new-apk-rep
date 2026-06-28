package cbfg.rvadapter;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RVAdapter<T> extends RecyclerView.Adapter<RVHolder<? extends Object>> {

    public interface ItemClickListener<T> {
        void onItemClick(android.view.View view, T data, int position);
    }

    public interface ItemLongClickListener<T> {
        boolean onItemLongClick(android.view.View view, T data, int position);
    }

    private final Context mContext;
    private final RVHolderFactory mFactory;
    private List<T> mItems = new ArrayList<>();
    private ItemClickListener<T> mClickListener;
    private ItemLongClickListener<T> mLongClickListener;

    public RVAdapter(Context context, RVHolderFactory factory) {
        this.mContext = context;
        this.mFactory = factory;
    }

    public RVAdapter<T> bind(RecyclerView rv) {
        rv.setAdapter(this);
        return this;
    }

    public void setItems(List<T> items) {
        mItems = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<T> getItems() {
        return mItems;
    }

    public void setItemClickListener(ItemClickListener<T> listener) {
        this.mClickListener = listener;
    }

    public void setItemLongClickListener(ItemLongClickListener<T> listener) {
        this.mLongClickListener = listener;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public RVHolder<? extends Object> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        T dummy = mItems.isEmpty() ? null : mItems.get(0);
        return mFactory.createViewHolder(parent, viewType, dummy);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(@NonNull RVHolder holder, int position) {
        T item = mItems.get(position);
        holder.setContent(item, false, null);
        holder.itemView.setOnClickListener(v -> {
            if (mClickListener != null) mClickListener.onItemClick(v, item, position);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (mLongClickListener != null) return mLongClickListener.onItemLongClick(v, item, position);
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}
