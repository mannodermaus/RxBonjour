package de.mannodermaus.rxbonjour.example.rv;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;

/**
 * Custom RecyclerView extension with additional properties
 *
 * @author marcel
 */
public class Rv extends RecyclerView {

    /**
     * Reference to the empty view, if any
     */
    private View mEmptyView;

    /**
     * Context menu info connected to this View
     */
    private ContextMenu.ContextMenuInfo mContextMenuInfo;

    /**
     * Data set observer for empty view coordination
     */
    private RecyclerView.AdapterDataObserver mObserver = new RecyclerView.AdapterDataObserver() {
        @Override public void onChanged() {
            checkIfEmpty();
        }

        @Override public void onItemRangeChanged(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        @Override public void onItemRangeInserted(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
            checkIfEmpty();
        }
    };

    public Rv(Context context) {
        super(context);
    }

    public Rv(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Rv(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

	/* Begin overrides */

    @Override public void scrollTo(int x, int y) {
        // Prevent "UnsupportedOperationException", because android:animateLayoutChanges depends on it. Works fine this way!
    }

    @Override public void setAdapter(Adapter adapter) {
        // First, unregister any previous adapter
        Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(mObserver);
        }

        // Call through to set the adapter
        super.setAdapter(adapter);

        // Register the new adapter
        if (adapter != null) {
            adapter.registerAdapterDataObserver(mObserver);
        }

        // Check if empty right away
        checkIfEmpty();
    }

    @Override public boolean showContextMenuForChild(View originalView) {
        // Initialize the context menu info for this item
        try {
            if (getChildAdapterPosition(originalView) != NO_POSITION) {
                // Obtain the ID of the child as well and create a context menu info for it
                ViewHolder holder = this.getChildViewHolder(originalView);
                mContextMenuInfo = new RvContextMenuInfo(holder);
                return super.showContextMenuForChild(originalView);

            } else {
                return false;
            }

        } catch (ClassCastException ex) {
            // If the RecyclerView isn't set up for context menus
            return false;
        }
    }

    @Override protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

	/* Begin public */

    /**
     * Sets a reference to an empty View, which is displayed when the adapter doesn't have any items to display.
     *
     * @param emptyView Empty View to link to this RecyclerView
     */
    public final void setEmptyView(View emptyView) {
        mEmptyView = emptyView;

        // Check if empty right away
        checkIfEmpty();
    }

	/* Begin private */

    private void checkIfEmpty() {
        // Only proceed if an empty view exists
        Adapter adapter = getAdapter();
        if (mEmptyView != null && adapter != null) {
            // Display the empty View whenever this RecyclerView doesn't show any items
            boolean empty = (adapter.getItemCount() == 0);
            mEmptyView.setVisibility(empty ? VISIBLE : GONE);
            setVisibility(empty ? GONE : VISIBLE);
        }
    }

	/* Begin inner classes */

    /**
     * ContextMenuInfo implementation for a RecyclerView. Instances of this class are contained within
     * the "onCreateContextMenu()" and "onContextItemSelected()" callbacks.
     */
    public static class RvContextMenuInfo implements ContextMenu.ContextMenuInfo {

        public final ViewHolder holder;
        public final int position;
        public final long id;

        /**
         * Constructor
         *
         * @param holder Holder from which to gather the information
         */
        public RvContextMenuInfo(ViewHolder holder) {
            this.holder = holder;
            this.position = holder.getAdapterPosition();
            this.id = holder.getItemId();
        }
    }
}
