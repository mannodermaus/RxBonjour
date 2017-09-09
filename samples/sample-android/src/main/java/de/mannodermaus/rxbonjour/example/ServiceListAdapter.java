package de.mannodermaus.rxbonjour.example;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.Iterator;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.mannodermaus.rxbonjour.BonjourService;
import de.mannodermaus.rxbonjour.example.rv.RvBaseAdapter;
import de.mannodermaus.rxbonjour.example.rv.RvBaseHolder;

public class ServiceListAdapter extends RvBaseAdapter<BonjourService> {

    @Override
    protected RvBaseHolder<BonjourService> createViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return new Holder(inflater, parent);
    }

    static final class Holder extends RvBaseHolder<BonjourService> {
        @BindView(R.id.tv_name) TextView tvName;
        @BindView(R.id.tv_type) TextView tvType;
        @BindView(R.id.tv_host_port_v4) TextView tvHostPortV4;
        @BindView(R.id.tv_host_port_v6) TextView tvHostPortV6;
        @BindView(R.id.tv_txtrecords) TextView tvTxtRecords;

        /**
         * Constructor
         *
         * @param inflater Layout inflater used to inflate the ViewHolder's layout
         * @param parent   Parent of the View to inflate
         */
        protected Holder(LayoutInflater inflater, ViewGroup parent) {
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
            Map<String, String> txtRecords = item.getTxtRecords();
            int txtRecordCount = txtRecords.size();
            if (txtRecordCount > 0) {
                StringBuilder txtRecordsText = new StringBuilder();
                Iterator<String> keyIterator = txtRecords.keySet().iterator();
                for (int i = 0; i < txtRecordCount; i++) {
                    // Append key-value information for the TXT record
                    String key = keyIterator.next();
                    txtRecordsText
                            .append(key)
                            .append(" -> ")
                            .append(txtRecords.get(key));

                    // Add line break if more is coming
                    if (i < txtRecordCount - 1) txtRecordsText.append('\n');
                }
                tvTxtRecords.setText(txtRecordsText.toString());

            } else {
                tvTxtRecords.setText(tvTxtRecords.getResources().getString(R.string.tv_notxtrecords));
            }
        }
    }
}
