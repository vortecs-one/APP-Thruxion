package com.example.qhagoapp

import android.app.Application
import com.example.qhagoapp.network.security.TokenManager
import com.example.qhagoapp.utils.ThemeManager

class Thruxion : Application()
{
    override fun onCreate()
    {
        super.onCreate()
        TokenManager.init(this)
        ThemeManager.init(this)
    }

}