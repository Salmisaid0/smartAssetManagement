package com.etachi.smartassetmanagement

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. Find the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? androidx.navigation.fragment.NavHostFragment

        // 2. Setup Navigation if fragment exists
        if (navHostFragment != null) {
            val navController = navHostFragment.navController
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
            bottomNav.setupWithNavController(navController)
        }
    }
}