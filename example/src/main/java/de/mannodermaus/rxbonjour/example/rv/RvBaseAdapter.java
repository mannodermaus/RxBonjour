package de.mannodermaus.rxbonjour.example.rv;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter implementation with convenience methods for the insertion, deletion
 * and other modifications of adapter items.
 *
 * @param <E> Item type to be held by the adapter
 */
public abstract class RvBaseAdapter<E> extends RecyclerView.Adapter<RvBaseHolder<E>> {

    /**
     * Key which can be used for adapter items used in onSave/onRestoreInstanceState
     */
    protected static final String INSTANCE_STATE_ITEMS = "isitems";

    /**
     * Int constant to represent "no position", used when invoking indexOf() with an item not present in the adapter
     */
    protected static final int NO_POSITION = -1;

    /**
     * List of unfiltered items
     */
    protected List<E> mItems;

    /**
     * Optional item click listener notified of events
     */
    private OnItemClickListener<E> mOnItemClickListener;

    /**
     * Constructor without any initial items to load the adapter with
     */
    public RvBaseAdapter() {
        // Initialize items
        mItems = new ArrayList<>();
    }

    /**
     * Constructor with initial items
     *
     * @param initialItems Initial items to load the adapter with
     */
    public RvBaseAdapter(List<E> initialItems) {
        this();
        if (initialItems != null) this.mItems.addAll(initialItems);
    }

	/* Begin public */

    public void setOnItemClickListener(OnItemClickListener<E> listener) {
        this.mOnItemClickListener = listener;
    }

    /**
     * Returns the list of items held by this adapter
     *
     * @return The list of items held by the adapter
     */
    public List<E> getItems() {
        return mItems;
    }

    /**
     * Clears the list of items held by this adapter
     */
    public void clearItems() {
        mItems.clear();
        notifyDataSetChanged();
    }

    /**
     * Gets the item at the given position from the item list, or returns null if there is no such position.
     *
     * @param position Position at which to get the item
     * @return The item at that position, or null for out-of-bounds values
     */
    public @Nullable E getItemAt(int position) {
        if (position >= 0 && position < getItemCount()) {
            return mItems.get(position);

        } else {
            return null;
        }
    }

    /**
     * Obtains the index of the provided item within this adapter.
     *
     * @param item Item to obtain the index of within the adapter
     * @return The position within the adapter's items that holds the provided item (comparisons made using equals()), or NO_POSITION if the item doesn't exist in the adapter.
     */
    public int indexOf(E item) {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).equals(item)) return i;
        }
        return NO_POSITION;
    }

    /**
     * Checks if the adapter contains the provided item. Ensure that custom objects implement
     * equals() and hashCode() in order for this to work reliably!
     *
     * @param item Item to check for in the list
     * @return True if the item is contained in the item list, false otherwise
     */
    public boolean containsItem(E item) {
        return mItems.contains(item);
    }

    /**
     * Re-sets the item list to the given values
     *
     * @param items Values to replace the adapter's current contents with
     */
    public void setItems(E[] items) {
        // Convert to a List and replace
        this.setItems(Arrays.asList(items));
    }

    /**
     * Re-sets the item list to the given values
     *
     * @param items Values to replace the adapter's current contents with
     */
    public void setItems(Set<E> items) {
        // Convert to a List and replace
        List<E> list = new ArrayList<>(items.size());
        list.addAll(items);
        this.setItems(list);
    }

    /**
     * Re-sets the item list to the given values
     *
     * @param items Values to replace the adapter's current contents with
     */
    public void setItems(List<E> items) {
        // Replace the item list and notify
        int oldSize = mItems.size();
        mItems = items;
        notifyItemRangeChanged(0, oldSize);
    }

    /**
     * Appends the provided item to the end of the item list
     *
     * @param item Item to append to the list
     */
    public void addItem(E item) {
        // Simply append to the end of the list
        this.insertItem(getItemCount(), item);
    }

    /**
     * Appends the provided items to the end of the item list
     *
     * @param items Items to append to the list
     */
    public void addItems(Collection<E> items) {
        // Append to the end of the list
        this.insertItems(getItemCount(), items);
    }

    /**
     * Updates the item at the given position within the item list.
     *
     * @param position Position at which to insert the item
     * @param item     Item to insert into the list
     * @throws IndexOutOfBoundsException for invalid indices
     */
    public void updateItem(int position, E item) {
        mItems.set(position, item);
        this.notifyItemChanged(position);
    }

    /**
     * Replaces the given item with the new one. This method appends the new item to the end of the list if the old one isn't contained in the adapter.
     *
     * @param oldItem Item to replace
     * @param newItem Item to replace the old one with
     */
    public void replaceItem(E oldItem, E newItem) {
        // If the old item exists in the adapter, replace it. Otherwise, append the new item at the end
        int index = this.indexOf(oldItem);
        if (index != NO_POSITION) this.updateItem(index, newItem);
        else this.addItem(newItem);
    }

    /**
     * Inserts the provided item at the given position within the item list. This method
     * takes care of bounds-checking, so that indices outside the item list's bounds are
     * automatically corrected (i.e., trying to insert at position 5 with only 1 item in the list
     * resulting in the item being appended to the end of the list).
     *
     * @param position Position at which to insert the item
     * @param item     Item to insert into the list
     */
    public void insertItem(int position, E item) {
        // Cap the position index at 0 and the total item size, then add it
        int actualPosition = Math.min(Math.max(position, 0), getItemCount());
        mItems.add(actualPosition, item);
        this.notifyItemInserted(actualPosition);
    }

    /**
     * Inserts the provided item collection at the given position within the item list.
     * This method takes care of bounds-checking, so that indices outside the item list's bounds
     * are automatically corrected.
     *
     * @param position Position at which to insert the items
     * @param items    Items to insert into the list
     */
    public void insertItems(int position, Collection<E> items) {
        // Cap the position index at 0 and the total item size, then add them
        int actualPosition = Math.min(Math.max(position, 0), getItemCount());
        mItems.addAll(actualPosition, items);
        this.notifyItemRangeInserted(actualPosition, items.size());
    }

    /**
     * Removes the provided item from the item list.
     * If the item doesn't exist in the list, this method does nothing.
     *
     * @param item Item to remove from the list
     * @return True if the item could be successfully removed, false if it doesn't exist in the list
     */
    public boolean removeItem(E item) {
        // Find the position of the item in the list and delegate
        int position = mItems.indexOf(item);
        return position > -1 && this.removeItem(position);
    }

    /**
     * Removes the item at the given position from the item list.
     * If the position is out of the item list's bounds, this method does nothing.
     *
     * @param position Position of the item to remove
     * @return True if the item could be successfully removed, false if an out-of-bounds value was passed in
     */
    public boolean removeItem(int position) {
        if (position >= 0 && position < getItemCount()) {
            mItems.remove(position);
            this.notifyItemRemoved(position);
            return true;
        }
        return false;
    }

	/* Begin overrides */

    @Override public int getItemCount() {
        return mItems.size();
    }

    @Override public final RvBaseHolder<E> onCreateViewHolder(ViewGroup parent, int viewType) {
        // Obtain the layout inflater and delegate to the abstract creation method
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RvBaseHolder<E> holder = this.createViewHolder(inflater, parent, viewType);

        // Attach item click listener, if any, then return
        if (mOnItemClickListener != null) holder.setItemClickListener(mOnItemClickListener);
        return holder;
    }

    @Override public final void onBindViewHolder(RvBaseHolder<E> holder, int position) {
        // Obtain the item to bind to the provided holder and delegate the bind process to it
        E item = mItems.get(position);
        holder.performBind(item);
    }

	/* Begin abstract */

    /**
     * Callback method invoked upon each demand of the adapter for a new ViewHolder.
     * Usually, the parameters can be passed through straight to the implementing
     * {@link RvBaseHolder} class for View inflation.
     *
     * @param inflater Layout inflater which can be used to inflate the ViewHolder's layout
     * @param parent   Parent of the View to inflate
     * @param viewType Type of view to inflate
     * @return A new instance of the ViewHolder implementation to be used by the adapter
     */
    protected abstract RvBaseHolder<E> createViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType);

	/* Begin inner classes */

    public interface OnItemClickListener<E> {
        void onItemClick(RvBaseHolder<E> holder, E item);
    }
}