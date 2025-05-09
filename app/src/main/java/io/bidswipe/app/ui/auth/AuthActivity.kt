package io.bidswipe.app.ui.auth

import android.os.Bundle
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityAuthBinding
import io.bidswipe.app.utils.bind

class AuthActivity : BaseActivity() {

    private val bind by bind(ActivityAuthBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)
        
    }

}