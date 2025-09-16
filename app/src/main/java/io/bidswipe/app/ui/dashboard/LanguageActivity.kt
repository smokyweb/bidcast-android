package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import androidx.activity.viewModels
import com.zeugmasolutions.localehelper.Locales
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.LanguageAdapter
import io.bidswipe.app.databinding.ActivityLanguageBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.setHapticClickListener

class LanguageActivity : BaseActivity() {

	private val bind by bind(ActivityLanguageBinding::inflate)
	private val viewModel by viewModels<DashViewModel>()

	private lateinit var langAdapter : LanguageAdapter
	private var selectedLang = Const.languages.find { it.locale == Locales.English } !!

	private val mClick = object : RecyclerClicks {

		override fun itemClick(pos : Int , status : String?) {
			selectedLang = Const.languages[pos]
			updateLocale(selectedLang.locale)
			Prefs(this@LanguageActivity).putString(Prefs.LANGUAGE , selectedLang.title)
			Prefs(this@LanguageActivity).putString(Prefs.LOCALE_LANGUAGE , selectedLang.locale.language)
		}
	}

	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

		langAdapter = LanguageAdapter(Const.languages , mClick)
		bind.language.adapter = langAdapter

		bind.header.onBackClick {
			finish()
		}

		bind.skip.setHapticClickListener {
			updateLocale(selectedLang.locale)
			Prefs(this@LanguageActivity).putString(Prefs.LANGUAGE , selectedLang.title)
			Prefs(this@LanguageActivity).putString(Prefs.LOCALE_LANGUAGE , selectedLang.locale.language)
			finish()
		}

		bind.select.setHapticClickListener {
			finish()
		}


	}
}