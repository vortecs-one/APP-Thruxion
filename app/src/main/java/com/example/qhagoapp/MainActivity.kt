package com.example.qhagoapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.qhagoapp.databinding.ActivityMainBinding
import com.example.qhagoapp.ui.login.LoginActivity

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Setup Toolbar
        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab?.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

        // 2. Setup NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        // 3. Setup Unified AppBarConfiguration
        // IMPORTANT: Include binding.drawerLayout here so the hamburger icon knows which drawer to open
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow, R.id.nav_settings, R.id.nav_webview
            ),
            binding.drawerLayout
        )

        // 4. Connect ActionBar to NavController with the config
        setupActionBarWithNavController(navController, appBarConfiguration)

        // 5. Setup Side Navigation Drawer (Hamburger Menu)
        binding.navView?.let {
            it.setupWithNavController(navController)
            it.setNavigationItemSelectedListener(this)
        }

        // 6. Setup Bottom Navigation (if it exists in current layout)
        binding.appBarMain.contentMain.bottomNavView?.setupWithNavController(navController)


    }

    // Add the onNavigationItemSelected method to handle all menu clicks
    override fun onNavigationItemSelected(item: MenuItem): Boolean
    {
        // Find the NavController
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Handle logout separately
        if (item.itemId == R.id.nav_logout) {
            logout()
            return true // Indicate the item was handled
        }
        // For all other items, let the Navigation Component handle it
        // This will navigate to the correct fragment
        navController.navigate(item.itemId)
        // Close the navigation drawer
        binding.drawerLayout?.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logout() {
        // Create an Intent to go back to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        // Set flags to clear the activity stack, preventing users from going "back"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // Start the LoginActivity
        startActivity(intent)
        // Finish MainActivity to remove it from memory
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.overflow, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Handle clicks on the overflow menu (e.g., top-right settings)
        return when (item.itemId) {
            R.id.nav_settings -> {
                navController.navigate(R.id.nav_settings)
                true
            }
            R.id.nav_slideshow -> {
                navController.navigate(R.id.nav_slideshow)
                true
            }
            R.id.nav_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean
    {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}