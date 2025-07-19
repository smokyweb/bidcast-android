@file:Suppress("PropertyName")

package io.bidswipe.app.base

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.bidswipe.app.utils.runSafe

abstract class BaseAdapter<TYPE : Any?, BIND : ViewBinding>(
    private val dataList: List<TYPE?>,
) : RecyclerView.Adapter<BaseAdapter.BaseViewHolder<BIND>>() {

    class BaseViewHolder<VIEW : ViewBinding>(val bind: VIEW) : RecyclerView.ViewHolder(bind.root)

    protected lateinit var mCtx: Context
    protected var TAG: String = javaClass.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<BIND> {
        mCtx = parent.context
        return BaseViewHolder(bindView(LayoutInflater.from(mCtx), parent))
    }

    override fun onBindViewHolder(viewHolder: BaseViewHolder<BIND>, position: Int) {
        runSafe {
            onBind(viewHolder, position, dataList[position])
        }
    }

    override fun getItemCount() = dataList.size

    abstract fun bindView(inflater: LayoutInflater, parent: ViewGroup): BIND

    abstract fun onBind(holder: BaseViewHolder<BIND>, position: Int, item: TYPE?)
}