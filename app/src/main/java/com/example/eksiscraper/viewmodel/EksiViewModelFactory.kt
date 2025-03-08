package com.example.eksiscraper.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EksiViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EksiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EksiViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 