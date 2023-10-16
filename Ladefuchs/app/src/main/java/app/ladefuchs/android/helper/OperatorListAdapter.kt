package app.ladefuchs.android.helper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.ladefuchs.android.R
import app.ladefuchs.android.dataClasses.Operator

class BaseViewHolder(container: ViewGroup) : RecyclerView.ViewHolder(container)

class BaseItemCallback : DiffUtil.ItemCallback<Operator>() {
    override fun areItemsTheSame(oldItem: Operator, newItem: Operator) = oldItem.toString() == newItem.toString()

    override fun areContentsTheSame(oldItem: Operator, newItem: Operator) = oldItem == newItem
}

class OperatorListAdapter(private val dataSet: Array<Operator>) :
    RecyclerView.Adapter<OperatorListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewHeading: TextView
        val textViewSubHeading: TextView

        init {
            // Define click listener for the ViewHolder's View
            textViewHeading = view.findViewById(R.id.textViewHeading)
            textViewSubHeading = view.findViewById(R.id.textViewSubHeading)
        }
    }
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.textViewHeading.text = dataSet[position].toString()
        viewHolder.textViewHeading.text = dataSet[position].identifier
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.cpo_emp_picker_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun getItemCount() = dataSet.size

}

