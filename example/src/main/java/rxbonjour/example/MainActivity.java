package rxbonjour.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import rx.Subscription;
import rx.functions.Action1;
import rxbonjour.RxBonjour;
import rxbonjour.example.rv.RvBaseAdapter;
import rxbonjour.example.rv.RvBaseHolder;
import rxbonjour.model.BonjourEvent;
import rxbonjour.model.BonjourService;

/**
 * @author marcel
 */
public class MainActivity extends AppCompatActivity {

	/* UI */

	@Bind(R.id.rv) rxbonjour.example.rv.Rv rvItems;
	@Bind(R.id.et_type) EditText etInput;

	/* Logic */

	private RvBaseAdapter<BonjourService> adapter;
	private Subscription nsdSubscription;
	private boolean useNsdManager = false;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);

		// Setup RecyclerView
		rvItems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
		adapter = new RvBaseAdapter<BonjourService>() {
			@Override protected RvBaseHolder<BonjourService> createViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
				return new BonjourVH(inflater, parent);
			}
		};
		rvItems.setEmptyView(ButterKnife.findById(this, R.id.tv_empty));
		rvItems.setAdapter(adapter);
	}

	@Override protected void onResume() {
		super.onResume();

		// Start a Bonjour lookup
		restartDiscovery();
	}

	@Override protected void onPause() {
		super.onPause();

		// Unsubscribe from the network service discovery Observable
		unsubscribe();
	}

	@Override protected void onDestroy() {
		super.onDestroy();
		ButterKnife.unbind(this);
	}

	@OnClick(R.id.button_apply) void onApplyClicked() {
		CharSequence input = etInput.getText();
		if (input != null && input.length() > 0) {
			// For non-empty input, restart the discovery with the new input
			restartDiscovery();
		}
	}

	@OnItemSelected(R.id.spinner) void onSpinnerItemSelected(AdapterView<?> adapter, View view, int position, long id) {
		// NsdManager implementation is represented by the second item in the spinner's array
		useNsdManager = (position == 1);
		restartDiscovery();
	}

	/* Begin private */

	private void unsubscribe() {
		if (nsdSubscription != null) {
			nsdSubscription.unsubscribe();
			nsdSubscription = null;
		}
	}

	private void restartDiscovery() {
		// Check the current input, only proceed if valid
		String input = etInput.getText().toString();
		if (!RxBonjour.isBonjourType(input)) {
			Toast.makeText(this, getString(R.string.toast_invalidtype, input), Toast.LENGTH_SHORT).show();
			return;
		}

		// Cancel any previous subscription
		unsubscribe();

		// Clear the adapter's items, then start a new discovery
		adapter.clearItems();
		nsdSubscription = RxBonjour.startDiscovery(this, input, useNsdManager)
				.subscribe(new Action1<BonjourEvent>() {
					@Override public void call(BonjourEvent bonjourEvent) {
						// Depending on the type of event and the availability of the item, adjust the adapter
						BonjourService item = bonjourEvent.getService();
						switch (bonjourEvent.getType()) {
							case ADDED:
								if (!adapter.containsItem(item)) adapter.addItem(item);
								break;

							case REMOVED:
								if (adapter.containsItem(item)) adapter.removeItem(item);
								break;
						}
					}
				}, new Action1<Throwable>() {
					@Override public void call(Throwable throwable) {
						Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
					}
				});
	}
}
