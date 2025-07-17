package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.CheckboxItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LangModel
import io.bidswipe.app.utils.Prefs

class LanguageAdapter(private val languages: List<LangModel?>,
                      private val mClicks: RecyclerClicks
) : BaseAdapter<LangModel, CheckboxItemBinding>(languages) {
    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        CheckboxItemBinding.inflate(inflater, parent, false)

    override fun onBind(holder: BaseViewHolder<CheckboxItemBinding>, position: Int, item: LangModel?) {
        with(holder) {

            val isSelected = languages[position]?.locale?.language == Prefs(mCtx).localeLanguage()

            bind.item.text = languages[position]?.title
            bind.root.isChecked = isSelected

            bind.root.addOnCheckedStateChangedListener { _, _ ->
                mClicks.itemClick(position)
            }
        }
    }
}