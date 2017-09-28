package de.mannodermaus.rxbonjour.samples.android

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import de.mannodermaus.rxbonjour.BonjourService

class DriverImplAdapter(context: Context)
    : ArrayAdapter<DriverImpl>(
        context,
        R.layout.support_simple_spinner_dropdown_item,
        DriverImpl.values())

class ServiceRecyclerAdapter : RecyclerBaseAdapter<BonjourService>() {
    override fun createViewHolder(inflater: LayoutInflater, parent: ViewGroup, viewType: Int) = Holder(inflater, parent)

    class Holder(
            inflater: LayoutInflater,
            parent: ViewGroup)
        : RecyclerBaseHolder<BonjourService>(inflater, parent, R.layout.item_bonjourservice) {

        @BindView(R.id.tv_name)
        lateinit var tvName: TextView
        @BindView(R.id.tv_type)
        lateinit var tvType: TextView
        @BindView(R.id.tv_host_port_v4)
        lateinit var tvHostPortV4: TextView
        @BindView(R.id.tv_host_port_v6)
        lateinit var tvHostPortV6: TextView
        @BindView(R.id.tv_txtrecords)
        lateinit var tvTxtRecords: TextView

        init {
            ButterKnife.bind(this, itemView)
        }

        override fun onBindItem(item: BonjourService) {
            val context = tvName.context
            tvName.text = item.name
            tvType.text = item.type

            // Display host address information
            tvHostPortV4.text = item.v4Host?.let { context.getString(R.string.format_host_address_v4, it, item.port) } ?: ""
            tvHostPortV6.text = item.v6Host?.let { context.getString(R.string.format_host_address_v6, it, item.port) } ?: ""

            // Display TXT records, if any could be resolved
            val txtRecords = item.txtRecords
            val txtRecordCount = txtRecords.size
            if (txtRecordCount > 0) {
                val txtRecordsText = StringBuilder()
                val keyIterator = txtRecords.keys.iterator()
                for (i in 0 until txtRecordCount) {
                    // Append key-value information for the TXT record
                    val key = keyIterator.next()
                    txtRecordsText
                            .append(key)
                            .append(" -> ")
                            .append(txtRecords[key])

                    // Add line break if more is coming
                    if (i < txtRecordCount - 1) txtRecordsText.append('\n')
                }
                tvTxtRecords.text = txtRecordsText.toString()

            } else {
                tvTxtRecords.text = tvTxtRecords.resources.getString(R.string.tv_notxtrecords)
            }
        }
    }
}