package de.mannodermaus.rxbonjour.samples.android

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import java.util.Arrays

/* Some additional utilities unrelated to RxBonjour itself */

class CustomRecyclerView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0)
  : RecyclerView(context, attrs, defStyle) {

  /** Reference to the empty view, if any */
  private var emptyView: View? = null

  /** Context menu info connected to this View */
  private var contextMenuInfo: ContextMenu.ContextMenuInfo? = null

  /** Data set observer for empty view coordination */
  private val observer = object : RecyclerView.AdapterDataObserver() {
    override fun onChanged() {
      checkIfEmpty()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
      checkIfEmpty()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
      checkIfEmpty()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
      checkIfEmpty()
    }
  }

  /* Begin overrides */

  override fun scrollTo(x: Int, y: Int) {
    // Prevent "UnsupportedOperationException", because android:animateLayoutChanges depends on it. Works fine this way!
  }

  override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
    // First, unregister any previous adapter
    val oldAdapter = getAdapter()
    oldAdapter?.unregisterAdapterDataObserver(observer)

    // Call through to set the adapter
    super.setAdapter(adapter)

    // Register the new adapter
    adapter?.registerAdapterDataObserver(observer)

    // Check if empty right away
    checkIfEmpty()
  }

  override fun showContextMenuForChild(originalView: View): Boolean {
    // Initialize the context menu info for this item
    return try {
      if (getChildAdapterPosition(originalView) != RecyclerView.NO_POSITION) {
        // Obtain the ID of the child as well and create a context menu info for it
        val holder = this.getChildViewHolder(originalView)
        contextMenuInfo = RvContextMenuInfo(holder)
        super.showContextMenuForChild(originalView)

      } else {
        false
      }

    } catch (ex: ClassCastException) {
      // If the RecyclerView isn't set up for context menus
      false
    }
  }

  override fun getContextMenuInfo(): ContextMenu.ContextMenuInfo? = contextMenuInfo

  /* Begin public */

  /**
   * Sets a reference to an empty View, which is displayed when the adapter doesn't have any items to display.
   *
   * @param emptyView Empty View to link to this RecyclerView
   */
  fun setEmptyView(emptyView: View) {
    this.emptyView = emptyView

    // Check if empty right away
    checkIfEmpty()
  }

  /* Begin private */

  private fun checkIfEmpty() {
    // Only proceed if an empty view exists
    val adapter = adapter
    if (emptyView != null && adapter != null) {
      // Display the empty View whenever this RecyclerView doesn't show any items
      val empty = adapter.itemCount == 0
      emptyView!!.visibility = if (empty) View.VISIBLE else View.GONE
      visibility = if (empty) View.GONE else View.VISIBLE
    }
  }

  /* Begin inner classes */

  /**
   * ContextMenuInfo implementation for a RecyclerView. Instances of this class are contained within
   * the "onCreateContextMenu()" and "onContextItemSelected()" callbacks.
   */
  class RvContextMenuInfo
  constructor(holder: RecyclerView.ViewHolder) : ContextMenu.ContextMenuInfo {
    val position = holder.adapterPosition
    val id = holder.itemId
  }
}

/**
 * RecyclerView adapter implementation with convenience methods for the insertion, deletion
 * and other modifications of adapter items.
 *
 * @param <E> Item type to be held by the adapter
 */
abstract class RecyclerBaseAdapter<E>() : RecyclerView.Adapter<RecyclerBaseHolder<E>>() {

  /** List of unfiltered items */
  protected var mutableItems: MutableList<E> = ArrayList()

  /** Optional item click listener notified of events */
  private var itemClickListener: OnItemClickListener<E>? = null

  /**
   * Returns the list of items held by this adapter
   *
   * @return The list of items held by the adapter
   */
  val items: List<E>
    get() = mutableItems

  /**
   * Constructor with initial items
   *
   * @param initialItems Initial items to load the adapter with
   */
  constructor(initialItems: List<E>?) : this() {
    if (initialItems != null) this.mutableItems.addAll(initialItems)
  }

  /* Begin public */

  fun setOnItemClickListener(listener: OnItemClickListener<E>) {
    this.itemClickListener = listener
  }

  /**
   * Clears the list of items held by this adapter
   */
  fun clearItems() {
    mutableItems.clear()
    notifyDataSetChanged()
  }

  /**
   * Gets the item at the given position from the item list, or returns null if there is no such position.
   *
   * @param position Position at which to get the item
   * @return The item at that position, or null for out-of-bounds values
   */
  fun getItemAt(position: Int): E? = if (position in 0..(itemCount - 1)) {
    mutableItems[position]
  } else {
    null
  }

  /**
   * Obtains the index of the provided item within this adapter.
   *
   * @param item Item to obtain the index of within the adapter
   * @return The position within the adapter's items that holds the provided item (comparisons made using equals()), or NO_POSITION if the item doesn't exist in the adapter.
   */
  fun indexOf(
      item: E): Int = mutableItems.indices.firstOrNull { mutableItems[it] == item } ?: NO_POSITION

  /**
   * Checks if the adapter contains the provided item. Ensure that custom objects implement
   * equals() and hashCode() in order for this to work reliably!
   *
   * @param item Item to check for in the list
   * @return True if the item is contained in the item list, false otherwise
   */
  fun containsItem(item: E): Boolean = mutableItems.contains(item)

  /**
   * Re-sets the item list to the given values
   *
   * @param items Values to replace the adapter's current contents with
   */
  fun setItems(items: Array<E>) {
    // Convert to a List and replace
    this.setItems(Arrays.asList(*items))
  }

  /**
   * Re-sets the item list to the given values
   *
   * @param items Values to replace the adapter's current contents with
   */
  fun setItems(items: Set<E>) {
    // Convert to a List and replace
    val list = ArrayList<E>(items.size)
    list.addAll(items)
    this.setItems(list)
  }

  /**
   * Re-sets the item list to the given values
   *
   * @param items Values to replace the adapter's current contents with
   */
  fun setItems(items: MutableList<E>) {
    // Replace the item list and notify
    val oldSize = mutableItems.size
    mutableItems = items
    notifyItemRangeChanged(0, oldSize)
  }

  /**
   * Appends the provided item to the end of the item list
   *
   * @param item Item to append to the list
   */
  fun addItem(item: E) {
    // Simply append to the end of the list
    this.insertItem(itemCount, item)
  }

  /**
   * Appends the provided items to the end of the item list
   *
   * @param items Items to append to the list
   */
  fun addItems(items: Collection<E>) {
    // Append to the end of the list
    this.insertItems(itemCount, items)
  }

  /**
   * Updates the item at the given position within the item list.
   *
   * @param position Position at which to insert the item
   * @param item     Item to insert into the list
   * @throws IndexOutOfBoundsException for invalid indices
   */
  fun updateItem(position: Int, item: E) {
    mutableItems[position] = item
    this.notifyItemChanged(position)
  }

  /**
   * Replaces the given item with the new one. This method appends the new item to the end of the list if the old one isn't contained in the adapter.
   *
   * @param oldItem Item to replace
   * @param newItem Item to replace the old one with
   */
  fun replaceItem(oldItem: E, newItem: E) {
    // If the old item exists in the adapter, replace it. Otherwise, append the new item at the end
    val index = this.indexOf(oldItem)
    if (index != NO_POSITION)
      this.updateItem(index, newItem)
    else
      this.addItem(newItem)
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
  fun insertItem(position: Int, item: E) {
    // Cap the position index at 0 and the total item size, then add it
    val actualPosition = Math.min(Math.max(position, 0), itemCount)
    mutableItems.add(actualPosition, item)
    this.notifyItemInserted(actualPosition)
  }

  /**
   * Inserts the provided item collection at the given position within the item list.
   * This method takes care of bounds-checking, so that indices outside the item list's bounds
   * are automatically corrected.
   *
   * @param position Position at which to insert the items
   * @param items    Items to insert into the list
   */
  fun insertItems(position: Int, items: Collection<E>) {
    // Cap the position index at 0 and the total item size, then add them
    val actualPosition = Math.min(Math.max(position, 0), itemCount)
    mutableItems.addAll(actualPosition, items)
    this.notifyItemRangeInserted(actualPosition, items.size)
  }

  /**
   * Removes the provided item from the item list.
   * If the item doesn't exist in the list, this method does nothing.
   *
   * @param item Item to remove from the list
   * @return True if the item could be successfully removed, false if it doesn't exist in the list
   */
  fun removeItem(item: E): Boolean {
    // Find the position of the item in the list and delegate
    val position = mutableItems.indexOf(item)
    return position > -1 && this.removeItem(position)
  }

  /**
   * Removes the item at the given position from the item list.
   * If the position is out of the item list's bounds, this method does nothing.
   *
   * @param position Position of the item to remove
   * @return True if the item could be successfully removed, false if an out-of-bounds value was passed in
   */
  fun removeItem(position: Int): Boolean {
    if (position in 0..(itemCount - 1)) {
      mutableItems.removeAt(position)
      this.notifyItemRemoved(position)
      return true
    }
    return false
  }

  /* Begin overrides */

  override fun getItemCount(): Int = mutableItems.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerBaseHolder<E> {
    // Obtain the layout inflater and delegate to the abstract creation method
    val inflater = LayoutInflater.from(parent.context)
    val holder = this.createViewHolder(inflater, parent, viewType)

    // Attach item click listener, if any, then return
    itemClickListener?.let { holder.setItemClickListener(it) }
    return holder
  }

  override fun onBindViewHolder(holder: RecyclerBaseHolder<E>, position: Int) {
    // Obtain the item to bind to the provided holder and delegate the bind process to it
    val item = mutableItems[position]
    holder.performBind(item)
  }

  /* Begin abstract */

  /**
   * Callback method invoked upon each demand of the adapter for a new ViewHolder.
   * Usually, the parameters can be passed through straight to the implementing
   * [RvBaseHolder] class for View inflation.
   *nflate the ViewHolder's layout
   * @param parent   Parent of the View to inflate
   * @param viewType Type of view to inflate
   * @return A new instance of the ViewHolder implementation to be used by the adapter
   */
  protected abstract fun createViewHolder(inflater: LayoutInflater, parent: ViewGroup,
      viewType: Int): RecyclerBaseHolder<E>

  /* Begin inner classes */

  interface OnItemClickListener<E> {
    fun onItemClick(holder: RecyclerBaseHolder<E>, item: E)
  }

  companion object {
    /** Key which can be used for adapter items used in onSave/onRestoreInstanceState */
    protected val INSTANCE_STATE_ITEMS = "isitems"

    /** Int constant to represent "no position", used when invoking indexOf() with an item not present in the adapter */
    protected val NO_POSITION = -1
  }
}

/**
 * RecyclerView view holder base implementation with convenience methods for click events.
 *
 * @param <E> Item type to be held by the view holder
 */
abstract class RecyclerBaseHolder<E>
protected constructor(
    inflater: LayoutInflater,
    parent: ViewGroup,
    @LayoutRes layoutRes: Int)
  : RecyclerView.ViewHolder(
    inflater.inflate(layoutRes, parent, false)), View.OnClickListener, View.OnLongClickListener {

  /* Begin public */

  private var item: E? = null
  private var itemClickListener: RecyclerBaseAdapter.OnItemClickListener<E>? = null

  init {
    // Attach an OnClickListener to the item view
    itemView.setOnClickListener(this)
    itemView.setOnLongClickListener(this)
    itemView.setOnHoverListener { v, event ->
      item != null && this@RecyclerBaseHolder.onHover(v, event, item)
    }
  }

  /* Begin abstract */

  /**
   * Invoked upon binding the view holder to an item. This callback is usually used to setup the holder's UI
   * with information obtained from the provided item.
   *
   * @param item Item with which to setup the view holder's UI components
   */
  protected abstract fun onBindItem(item: E)

  /* Begin package */

  /**
   * Internal bind method called from a [RvBaseAdapter].
   *
   * @param item Item to bind to the view
   */
  internal fun performBind(item: E) {
    // Save the item reference and call the abstract bind implementation
    this.item = item
    this.onBindItem(item)
  }

  internal fun setItemClickListener(listener: RecyclerBaseAdapter.OnItemClickListener<E>) {
    this.itemClickListener = listener
  }

  /* Begin protected */

  /**
   * Invoked when the holder's item view is clicked. Does nothing by default
   *
   * @param v    Item view of the view holder
   * @param item The item it is currently bound to
   */
  protected fun onClick(v: View, item: E) {
    // If an item click listener is set, call it
    itemClickListener?.onItemClick(this, item)
  }

  /**
   * Invoked when the holder's item view is long-clicked. Returns false by default
   *
   * @param v    Item view of the view holder
   * @param item The item it is currently bound to
   * @return True if the callback consumed the event, false otherwise
   */
  protected fun onLongClick(v: View, item: E): Boolean = false

  /**
   * Invoked when the holder's item view triggered a hover event. Returns false by default
   *
   * @param v     Item view of the view holder
   * @param event Motion event containing the hover
   * @param item  The item it is currently bound to
   * @return True if the callback consumed the event, false otherwise
   */
  protected fun onHover(v: View, event: MotionEvent, item: E?): Boolean = false

  /* Begin overrides */

  override fun onClick(v: View) {
    item?.let { this.onClick(v, it) }
  }

  override fun onLongClick(v: View): Boolean = item?.let { this.onLongClick(v, it) } == true
}
