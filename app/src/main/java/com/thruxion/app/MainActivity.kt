package com.thruxion.app

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import android.widget.TextView
import android.webkit.CookieManager
import android.webkit.WebStorage
import com.thruxion.app.databinding.ActivityMainBinding
import com.thruxion.app.network.security.TokenManager
import com.thruxion.app.ui.chat.ChatDialogFragment
import com.thruxion.app.ui.login.LoginActivity
import com.thruxion.app.utils.ThemeManager
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

        binding.appBarMain.fab?.setOnClickListener {
            ChatDialogFragment.newInstance().show(supportFragmentManager, "ChatDialog")
        }

        // 2. Setup NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        // 3. Setup Unified AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow, R.id.nav_settings, R.id.nav_webview, R.id.nav_healthy
            ),
            binding.drawerLayout
        )

        // 4. Connect ActionBar to NavController
        setupActionBarWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, _, _ ->
            invalidateOptionsMenu()
        }

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

        // 7. Setup Keyboard Visibility Listener
        setupKeyboardListener()
    }

    private fun setupKeyboardListener() {
        val root = binding.root
        root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = root.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            // If keyboard is visible (occupies more than 15% of the screen)
            if (keypadHeight > screenHeight * 0.15) {
                binding.appBarMain.contentMain.bottomNavView?.visibility = View.GONE
            } else {
                binding.appBarMain.contentMain.bottomNavView?.visibility = View.VISIBLE
            }
        }
    }

    private fun logout() {
        TokenManager.clearTokens()
        com.thruxion.app.utils.LocaleManager.init(this)

        // SECURITY: Securely clear WebView sessions to prevent data leakage between users
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies {
            cookieManager.flush()
        }
        WebStorage.getInstance().deleteAllData()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.overflow, menu)

        val themeItem = menu.findItem(R.id.action_theme_switch)
        // Set state immediately on inflation
        val themeSwitch = (themeItem.actionView as? SwitchMaterial) 
            ?: themeItem.actionView?.findViewById(R.id.theme_switch)

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
