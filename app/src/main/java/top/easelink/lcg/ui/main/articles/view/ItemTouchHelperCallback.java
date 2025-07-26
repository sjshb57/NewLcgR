package top.easelink.lcg.ui.main.articles.view;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final onMoveAndSwipedListener moveAndSwipedListener;

    public ItemTouchHelperCallback(onMoveAndSwipedListener listener) {
        this.moveAndSwipedListener = listener;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getItemViewType() == FavoriteArticlesAdapter.VIEW_TYPE_NORMAL) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }
        return 0;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        if (viewHolder.getItemViewType() != target.getItemViewType()) {
            return false;
        }
        return moveAndSwipedListener.onItemMove(
                viewHolder.getBindingAdapterPosition(),
                target.getBindingAdapterPosition()
        );
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        moveAndSwipedListener.onItemRemove(viewHolder.getBindingAdapterPosition());
    }

    public interface onMoveAndSwipedListener {
        boolean onItemMove(int fromPosition, int toPosition);
        void onItemRemove(int position);
    }
}