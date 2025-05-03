package io.bidcast.app.ui.auth

import android.os.Bundle
import io.bidcast.app.base.BaseActivity
import io.bidcast.app.databinding.ActivityAuthBinding
import io.bidcast.app.utils.bind

class AuthActivity : BaseActivity() {

    private val bind by bind(ActivityAuthBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)
        
    }

}