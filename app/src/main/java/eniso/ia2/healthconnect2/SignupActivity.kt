package eniso.ia2.healthconnect2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import eniso.ia2.healthconnect2.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Authentication and Database Reference
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        // Spinner setup for selecting roles
        val roleSpinner: Spinner = binding.roleSpinner
        val roles = listOf("Select Role", "Doctor", "Patient")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            roles
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = adapter

        // Variable to store the selected role
        var selectedRole: String = ""

        // Set up role selection listener
        roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // Ignore "Select Role"
                    selectedRole = roles[position]
                    Toast.makeText(this@SignupActivity, "Selected Role: $selectedRole", Toast.LENGTH_SHORT).show()
                } else {
                    selectedRole = "" // Reset if default is selected
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // No action needed
            }
        }

        // Login TextView click listener
        binding.loginTextView.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        // Sign Up button click listener
        binding.signBtn.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val pwd = binding.passwordInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()

            // Input validation
            if (email.isEmpty() || pwd.isEmpty() || username.isEmpty() || selectedRole.isEmpty() || selectedRole == "Select Role") {
                Toast.makeText(this, "Please enter all fields and select a valid role", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user in Firebase Authentication
            auth.createUserWithEmailAndPassword(email, pwd)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = task.result?.user?.uid ?: ""
                        Log.i("TAG", "User ID: $userId")

                        // Set displayName for the user in Firebase Authentication
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()

                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    Log.i("TAG", "Display name updated successfully.")
                                } else {
                                    Log.e("TAG", "Error updating display name: ${profileTask.exception?.message}")
                                }
                            }

                        // Create a user data map with the selected role
                        val userMap = mapOf(
                            "fullName" to username,
                            "email" to email,
                            "role" to selectedRole
                        )

                        // Save user data in Firebase Realtime Database
                        database.child(userId).setValue(userMap)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, Login::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Database error: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
