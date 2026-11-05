package com.example

import android.app.Application
import com.example.data.local.CommissionWalaDatabase
import com.example.data.local.SessionPreferences
import com.example.data.repository.CommissionWalaRepository

class CommissionWalaApp : Application() {
    val database: CommissionWalaDatabase by lazy {
        CommissionWalaDatabase.getInstance(this)
    }

    val sessionPreferences: SessionPreferences by lazy {
        SessionPreferences(this)
    }

    val repository: CommissionWalaRepository by lazy {
        CommissionWalaRepository(database, sessionPreferences)
    }
}
