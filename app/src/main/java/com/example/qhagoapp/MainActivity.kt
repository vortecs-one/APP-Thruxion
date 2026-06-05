package com.example.qhagoapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import android.widget.TextView
import com.example.qhagoapp.databinding.ActivityMainBinding
import com.example.qhagoapp.network.security.TokenManager
import com.example.qhagoapp.ui.login.LoginActivity
import com.example.qhagoapp.utils.ThemeManager
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

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
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow, R.id.nav_settings, R.id.nav_webview
            ),
            binding.drawerLayout
        )

        // 4. Connect ActionBar to NavController
        setupActionBarWithNavController(navController, appBarConfiguration)

        // 5. Setup Side Navigation Drawer
        binding.navView?.let { navView ->
            navView.setupWithNavController(navController)
            
            // Update Header with user email
            val headerView = navView.getHeaderView(0)
            val userEmailTv = headerView.findViewById<TextView>(R.id.textView)
            TokenManager.getUserEmail()?.let { 
                userEmailTv.text = it
            }

            navView.setNavigationItemSelectedListener { item ->
                if (item.itemId == R.id.nav_logout) {
                    logout()
                    true
                } else {
                    // Use onNavDestinationSelected for standard navigation items
                    val handled = NavigationUI.onNavDestinationSelected(item, navController)
                    if (handled) {
                        binding.drawerLayout?.closeDrawer(GravityCompat.START)
                    }
                    handled
                }
            }
        }

        // 6. Setup Bottom Navigation
        binding.appBarMain.contentMain.bottomNavView?.setupWithNavController(navController)
    }

    private fun logout() {
        TokenManager.clearTokens()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.overflow, menu)

        val themeItem = menu.findItem(R.id.action_theme_switch)
        val themeSwitch = themeItem.actionView?.findViewById<SwitchMaterial>(R.id.theme_switch)

        themeSwitch?.apply {
            setOnCheckedChangeListener(null)
            isChecked = ThemeManager.isDarkMode()
            setOnCheckedChangeListener { _, isChecked ->
                ThemeManager.setDarkMode(isChecked)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Handle logout manually, let NavigationUI handle the rest
        return when (item.itemId) {
            R.id.nav_logout -> {
                logout()
                true
            }
            else -> item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean
    {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}