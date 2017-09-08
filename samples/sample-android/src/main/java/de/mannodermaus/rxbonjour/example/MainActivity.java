package de.mannodermaus.rxbonjour.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import de.mannodermaus.rxbonjour.BonjourEvent;
import de.mannodermaus.rxbonjour.BonjourService;
import de.mannodermaus.rxbonjour.RxBonjour;
import de.mannodermaus.rxbonjour.drivers.jmdns.JmDNSDriver;
import de.mannodermaus.rxbonjour.example.rv.RvBaseAdapter;
import de.mannodermaus.rxbonjour.example.rv.RvBaseHolder;
import de.mannodermaus.rxbonjour.platforms.android.AndroidPlatform;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;

/**
 * @author marcel
 */
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.rv) de.mannodermaus.rxbonjour.example.rv.Rv rvItems;
    @BindView(R.id.et_type) EditText etInput;
    private RvBaseAdapter<BonjourService> adapter;
    private Unbinder unbinder;

    private RxBonjour rxBonjour = new RxBonjour.Builder()
            .platform(AndroidPlatform.create(this))
            .driver(JmDNSDriver.create())
            .create();

    private Disposable nsdDisposable = Disposables.empty();
//    private boolean useNsdManager = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);

        // Setup RecyclerView
        rvItems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new RvBaseAdapter<BonjourService>() {
            @Override
            protected RvBaseHolder<BonjourService> createViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
                return new BonjourVH(inflater, parent);
            }
        };
        rvItems.setEmptyView(ButterKnife.findById(this, R.id.tv_empty));
        rvItems.setAdapter(adapter);
    }

    @Override protected void onStart() {
        super.onStart();

        // Start a Bonjour lookup
        restartDiscovery();
    }

    @Override protected void onStop() {
        super.onStop();

        // Unsubscribe from the network service discovery Observable
        unsubscribe();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @OnClick(R.id.button_apply) void onApplyClicked() {
        CharSequence input = etInput.getText();
        if (input != null && input.length() > 0) {
            // For non-empty input, restart the discovery with the new input
            restartDiscovery();
        }
    }

//    @OnItemSelected(R.id.spinner)
//    void onSpinnerItemSelected(AdapterView<?> adapter, View view, int position, long id) {
//        // NsdManager implementation is represented by the second item in the spinner's array
//        useNsdManager = (position == 1);
//        restartDiscovery();
//    }

	/* Begin private */

    private void unsubscribe() {
        nsdDisposable.dispose();
    }

    private void restartDiscovery() {
        // Check the current input, only proceed if valid
        String type = etInput.getText().toString();
        if (!RxBonjour.isBonjourType(type)) {
            Toast.makeText(this, getString(R.string.toast_invalidtype, type), Toast.LENGTH_SHORT).show();
            return;
        }

        // Cancel any previous subscription
        unsubscribe();

        // Clear the adapter's items, then start a new discovery
        adapter.clearItems();

        nsdDisposable = rxBonjour.newDiscovery(type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        event -> {
                            // Depending on the type of event and the availability of the item, adjust the adapter
                            BonjourService item = event.getService();
                            Log.i("RxBonjour Event", "Event: " + item);
                            if (event instanceof BonjourEvent.Added) {
                                if (!adapter.containsItem(item)) adapter.addItem(item);
                            } else if (event instanceof BonjourEvent.Removed) {
                                if (adapter.containsItem(item)) adapter.removeItem(item);
                            }
                        },
                        error -> Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
