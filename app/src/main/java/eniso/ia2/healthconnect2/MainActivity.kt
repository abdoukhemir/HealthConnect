package eniso.ia2.healthconnect2

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity

import androidx.fragment.app.Fragment
import eniso.ia2.healthconnect2.SettingsFragment

import eniso.ia2.healthconnect2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
       val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up BottomNavigationView item selection listener
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null

            when (item.itemId) {
                R.id.item_home -> {
                    selectedFragment = HomeFragment()
                }
                R.id.item_AI -> {
                    selectedFragment = AIFragment()
                }
                R.id.item_Settings -> {
                    selectedFragment = SettingsFragment()
                }
            }

            // Replace the fragment when an item is selected
            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .addToBackStack(null) // Optional
                    .commit()
            }
            true // Return true to indicate the item was selected
        }

        // Set the initial fragment if not already set (for first time)
        if (savedInstanceState == null) {
            val initialFragment = HomeFragment() // Default fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, initialFragment)
                .commit()
        }
    }
}
