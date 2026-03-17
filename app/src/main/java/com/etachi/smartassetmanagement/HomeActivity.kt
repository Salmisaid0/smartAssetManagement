package com.etachi.smartassetmanagement

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. Find the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // 2. Get the NavController
        val navController = navHostFragment.navController

        // 3. Find the BottomNavigationView
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // 4. Link them together
        bottomNav.setupWithNavController(navController)
    }
}