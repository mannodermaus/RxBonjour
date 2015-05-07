package rxbonjour.example.rv;

import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

/**
 * RecyclerView view holder base implementation with convenience methods for click events.
 *
 * @param <E> Item type to be held by the view holder
 */
public abstract class RvBaseHolder<E> extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

	/** The item held by the view holder */
	private E mItem;

	/** Optional item click listener */
	private RvBaseAdapter.OnItemClickListener<E> mItemClickListener;

	/**
	 * Constructor
	 *
	 * @param inflater  Layout inflater used to inflate the ViewHolder's layout
	 * @param parent    Parent of the View to inflate
	 * @param layoutRes Resource ID of the layout with which to inflate the view holder's item view
	 */
	protected RvBaseHolder(LayoutInflater inflater, ViewGroup parent, @LayoutRes int layoutRes) {
		// Inject the item view and call through with that
		super(inflater.inflate(layoutRes, parent, false));

		// Attach an OnClickListener to the item view
		itemView.setOnClickListener(this);
		itemView.setOnLongClickListener(this);
		if (Build.VERSION.SDK_INT >= ICE_CREAM_SANDWICH) {
			itemView.setOnHoverListener(new View.OnHoverListener() {
				@Override public boolean onHover(View v, MotionEvent event) {
					return mItem != null && RvBaseHolder.this.onHover(v, event, mItem);
				}
			});
		}
	}

	/* Begin abstract */

	/**
	 * Invoked upon binding the view holder to an item. This callback is usually used to setup the holder's UI
	 * with information obtained from the provided item.
	 *
	 * @param item Item with which to setup the view holder's UI components
	 */
	protected abstract void onBindItem(E item);

	/* Begin public */

	/**
	 * Returns a reference to the currently bound item to the View Holder.
	 *
	 * @return The currently bound item
	 */
	public E getItem() {
		return mItem;
	}

	/* Begin package */

	/**
	 * Internal bind method called from a {@link RvBaseAdapter}.
	 *
	 * @param item Item to bind to the view
	 */
	final void performBind(E item) {
		// Save the item reference and call the abstract bind implementation
		this.mItem = item;
		this.onBindItem(item);
	}

	final void setItemClickListener(RvBaseAdapter.OnItemClickListener<E> listener) {
		this.mItemClickListener = listener;
	}

	/* Begin protected */

	/**
	 * Invoked when the holder's item view is clicked. Does nothing by default
	 *
	 * @param v    Item view of the view holder
	 * @param item The item it is currently bound to
	 */
	protected void onClick(View v, E item) {
		// If an item click listener is set, call it
		if (mItemClickListener != null) mItemClickListener.onItemClick(this, item);
	}

	/**
	 * Invoked when the holder's item view is long-clicked. Returns false by default
	 *
	 * @param v    Item view of the view holder
	 * @param item The item it is currently bound to
	 * @return True if the callback consumed the event, false otherwise
	 */
	protected boolean onLongClick(View v, E item) {
		return false;
	}

	/**
	 * Invoked when the holder's item view triggered a hover event. Returns false by default
	 *
	 * @param v     Item view of the view holder
	 * @param event Motion event containing the hover
	 * @param item  The item it is currently bound to
	 * @return True if the callback consumed the event, false otherwise
	 */
	protected boolean onHover(View v, MotionEvent event, E item) {
		return false;
	}

	/* Begin overrides */

	@Override public final void onClick(View v) {
		if (mItem != null) this.onClick(v, mItem);
	}

	@Override public final boolean onLongClick(View v) {
		return mItem != null && this.onLongClick(v, mItem);
	}
}
