package de.mannodermaus.rxbonjour.samples.android

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import de.mannodermaus.rxbonjour.BonjourBroadcastConfig
import de.mannodermaus.rxbonjour.BonjourEvent
import de.mannodermaus.rxbonjour.RxBonjour
import de.mannodermaus.rxbonjour.isBonjourType
import de.mannodermaus.rxbonjour.platforms.android.AndroidPlatform
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import java.net.InetAddress

private val LOG_TAG = "RxBonjour Sample"

/**
 * @author marcel
 */
class MainActivity : AppCompatActivity() {

    @BindView(R.id.et_type)
    lateinit var etInput: EditText
    @BindView(R.id.progress_bar)
    lateinit var progressBar: ProgressBar
    @BindView(R.id.spinner)
    lateinit var spDrivers: Spinner
    @BindView(R.id.rv)
    lateinit var rvItems: CustomRecyclerView

    lateinit var spinnerAdapter: DriverImplAdapter
    private val listAdapter = ServiceRecyclerAdapter()

    lateinit var unbinder: Unbinder
    private var nsdDisposable = Disposables.empty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        unbinder = ButterKnife.bind(this)

        // Setup RecyclerView
        rvItems.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvItems.setEmptyView(findViewById(R.id.tv_empty))
        rvItems.adapter = listAdapter

        // Setup Spinner
        spinnerAdapter = DriverImplAdapter(this)
        spDrivers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                // This will fire immediately when the first item is set
                restartDiscovery()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }
        spDrivers.adapter = spinnerAdapter
    }

    override fun onStop() {
        super.onStop()

        // Unsubscribe from the network service discovery Observable
        unsubscribe()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbinder.unbind()
    }

    @OnClick(R.id.button_apply)
    internal fun onApplyClicked() {
        val input = etInput.text
        if (input != null && input.isNotEmpty()) {
            // For non-empty input, restart the discovery with the new input
            restartDiscovery()
        }
    }

    /* Begin private */

    private fun unsubscribe() {
        nsdDisposable.dispose()
    }

    private fun restartDiscovery() {
        // Check the current input, only proceed if valid
        val type = etInput.text.toString()
        if (!type.isBonjourType()) {
            Toast.makeText(this, getString(R.string.toast_invalidtype, type), Toast.LENGTH_SHORT).show()
            return
        }

        // Cancel any previous subscription
        unsubscribe()

        // Clear the adapter's items, then start a new discovery
        listAdapter.clearItems()

        // Construct a new RxBonjour instance with the currently selected Driver.
        // Usually, you'd simply add the Driver inside the Builder
        // and provide the entry point to RxBonjour globally,
        // e.g. through Dependency Injection, or as an instance field.
        //
        // RxBonjour rxBonjour = new RxBonjour.Builder()
        //      .driver(JmDNSDriver.create())
        //      .platform(AndroidPlatform.create(this))
        //      .create();
        //
        // Since in this sample Driver implementations can be switched,
        // we're using the SpinnerAdapter for this.
        val driverLibrary = spinnerAdapter.getItem(spDrivers.selectedItemPosition)

        val rxBonjour = RxBonjour.Builder()
                .driver(driverLibrary.factory.invoke(this))
                .platform(AndroidPlatform.create(this))
                .create()

        val broadcastConfig = BonjourBroadcastConfig(
                type = "_http._tcp",
                name = "My Bonjour Service",
                address = null,
                port = 13337,
                txtRecords = mapOf(
                        "my.record" to "my value",
                        "other.record" to "0815"))
        val disposable = rxBonjour.newBroadcast(broadcastConfig)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()

        nsdDisposable = rxBonjour.newDiscovery(type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { progressBar.visibility = View.VISIBLE }
                .doOnComplete { progressBar.visibility = View.INVISIBLE }
                .doOnError { progressBar.visibility = View.INVISIBLE }
                .subscribe(
                        { event ->
                            // Depending on the type of event and the availability of the item, adjust the adapter
                            val item = event.service
                            Log.i(LOG_TAG, "Event: " + item)
                            when (event) {
                                is BonjourEvent.Added -> if (!listAdapter.containsItem(item)) listAdapter.addItem(item)
                                is BonjourEvent.Removed -> if (listAdapter.containsItem(item)) listAdapter.removeItem(item)
                            }
                        },
                        { error ->
                            error.printStackTrace()
                            Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
                        })
    }
}
