package rxbonjour.example;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

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
	}
}
