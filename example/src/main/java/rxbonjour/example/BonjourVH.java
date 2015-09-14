package rxbonjour.example;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.Iterator;

import butterknife.Bind;
import butterknife.ButterKnife;
import rxbonjour.example.rv.RvBaseHolder;
import rxbonjour.model.BonjourService;

/**
 * @author marcel
 */
public class BonjourVH extends RvBaseHolder<BonjourService> {

	@Bind(R.id.tv_name) TextView tvName;
	@Bind(R.id.tv_type) TextView tvType;
	@Bind(R.id.tv_host_port_v4) TextView tvHostPortV4;
	@Bind(R.id.tv_host_port_v6) TextView tvHostPortV6;
	@Bind(R.id.tv_txtrecords) TextView tvTxtRecords;

	/**
	 * Constructor
	 *
	 * @param inflater  Layout inflater used to inflate the ViewHolder's layout
	 * @param parent    Parent of the View to inflate
	 */
	protected BonjourVH(LayoutInflater inflater, ViewGroup parent) {
		super(inflater, parent, R.layout.item_bonjourservice);
		ButterKnife.bind(this, itemView);
	}

	@Override protected void onBindItem(BonjourService item) {
		Context context = tvName.getContext();
		tvName.setText(item.getName());
		tvType.setText(item.getType());

		// Display host address information
		Inet4Address inet4Address = item.getV4Host();
		Inet6Address inet6Address = item.getV6Host();
		tvHostPortV4.setText(inet4Address != null ? context.getString(R.string.format_host_address_v4, inet4Address, item.getPort()) : "");
		tvHostPortV6.setText(inet6Address != null ? context.getString(R.string.format_host_address_v6, inet6Address, item.getPort()) : "");

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
