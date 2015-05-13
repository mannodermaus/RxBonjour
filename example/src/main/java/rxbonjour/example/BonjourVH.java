package rxbonjour.example;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Iterator;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rxbonjour.example.rv.RvBaseHolder;
import rxbonjour.model.BonjourService;

/**
 * @author marcel
 */
public class BonjourVH extends RvBaseHolder<BonjourService> {

	@InjectView(R.id.tv_name) TextView tvName;
	@InjectView(R.id.tv_type) TextView tvType;
	@InjectView(R.id.tv_host_port) TextView tvHostPort;
	@InjectView(R.id.tv_txtrecords) TextView tvTxtRecords;

	/**
	 * Constructor
	 *
	 * @param inflater  Layout inflater used to inflate the ViewHolder's layout
	 * @param parent    Parent of the View to inflate
	 */
	protected BonjourVH(LayoutInflater inflater, ViewGroup parent) {
		super(inflater, parent, R.layout.item_bonjourservice);
		ButterKnife.inject(this, itemView);
	}

	@Override protected void onBindItem(BonjourService item) {
		tvName.setText(item.getName());
		tvType.setText(item.getType());
		tvHostPort.setText(item.getHost() + ":" + item.getPort());

		// Display TXT records, if any could be resolved
		int txtRecordCount = item.getTxtRecordCount();
		if (txtRecordCount > 0) {
			StringBuilder txtRecordsText = new StringBuilder();
			Iterator<String> keyIterator = item.getTxtRecords().keySet().iterator();
			for (int i = 0; i < txtRecordCount; i++) {
				// Append key-value information for the TXT record
				String key = keyIterator.next();
				txtRecordsText
						.append(key)
						.append(" -> ")
						.append(item.getTxtRecord(key, "null"));

				// Add line break if more is coming
				if (i < txtRecordCount - 1) txtRecordsText.append('\n');
			}
			tvTxtRecords.setText(txtRecordsText.toString());

		} else {
			tvTxtRecords.setText(tvTxtRecords.getResources().getString(R.string.tv_notxtrecords));
		}
	}
}
