package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.CrunchiRepository
import com.example.ui.CrunchiApp
import com.example.ui.CrunchiViewModel
import com.example.ui.CrunchiViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local Room database
        val database = AppDatabase.getDatabase(this)
        val repository = CrunchiRepository(database)
        
        // Instantiate the main view model
        val viewModelFactory = CrunchiViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[CrunchiViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CrunchiApp(viewModel = viewModel)
            }
        }
    }
}
