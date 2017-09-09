package de.mannodermaus.rxbonjour.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import de.mannodermaus.rxbonjour.BonjourEvent;
import de.mannodermaus.rxbonjour.BonjourService;
import de.mannodermaus.rxbonjour.RxBonjour;
import de.mannodermaus.rxbonjour.example.rv.Rv;
import de.mannodermaus.rxbonjour.platforms.android.AndroidPlatform;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;

/**
 * @author marcel
 */
public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "RxBonjour Sample";

    @BindView(R.id.et_type) EditText etInput;
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.spinner) Spinner spDrivers;
    @BindView(R.id.rv) Rv rvItems;

    private DriverSpinnerAdapter spinnerAdapter;
    private final ServiceListAdapter listAdapter = new ServiceListAdapter();

    private Unbinder unbinder;
    private Disposable nsdDisposable = Disposables.empty();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);

        // Setup RecyclerView
        rvItems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvItems.setEmptyView(findViewById(R.id.tv_empty));
        rvItems.setAdapter(listAdapter);

        // Setup Spinner
        spinnerAdapter = new DriverSpinnerAdapter(this);
        spDrivers.setAdapter(spinnerAdapter);
        spDrivers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                restartDiscovery();
            }

            @Override public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
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

    @OnClick(R.id.button_apply)
    void onApplyClicked() {
        CharSequence input = etInput.getText();
        if (input != null && input.length() > 0) {
            // For non-empty input, restart the discovery with the new input
            restartDiscovery();
        }
    }

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
        listAdapter.clearItems();

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
        DriverSpinnerAdapter.DriverLib driverLibrary = spinnerAdapter.getItem(spDrivers.getSelectedItemPosition());
        assert driverLibrary != null;

        RxBonjour rxBonjour = new RxBonjour.Builder()
                .driver(driverLibrary.factory.invoke(this))
                .platform(AndroidPlatform.create(this))
                .create();

        nsdDisposable = rxBonjour.newDiscovery(type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(s -> progressBar.setVisibility(View.VISIBLE))
                .doOnComplete(() -> progressBar.setVisibility(View.INVISIBLE))
                .doOnError(e -> progressBar.setVisibility(View.INVISIBLE))
                .subscribe(
                        event -> {
                            // Depending on the type of event and the availability of the item, adjust the adapter
                            BonjourService item = event.getService();
                            Log.i(LOG_TAG, "Event: " + item);
                            if (event instanceof BonjourEvent.Added) {
                                if (!listAdapter.containsItem(item)) listAdapter.addItem(item);
                            } else if (event instanceof BonjourEvent.Removed) {
                                if (listAdapter.containsItem(item)) listAdapter.removeItem(item);
                            }
                        },
                        error -> {
                            error.printStackTrace();
                            Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        });
    }
}
