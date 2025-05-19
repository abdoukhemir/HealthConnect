package eniso.ia2.healthconnect2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.AlertDialog
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import eniso.ia2.healthconnect2.sampledata.Comment
import eniso.ia2.healthconnect2.sampledata.Publication
import eniso.ia2.healthconnect2.databinding.DialogCommentsBinding
import eniso.ia2.healthconnect2.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var publicationsAdapter: PublicationsAdapter
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    val publications = mutableListOf<Publication>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().getReference("")
        auth = FirebaseAuth.getInstance()

        database.child("publications").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                publications.clear() // Clear existing data

                for (postSnapshot in snapshot.children) {
                    val id = postSnapshot.key ?: ""
                    val author = postSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val content = postSnapshot.child("content").getValue(String::class.java) ?: "No content"

                    publications.add(Publication(id, author, content))
                }

                if (publications.isEmpty()) {
                    // Show a message if there are no publications
                    Toast.makeText(context, "No publications found!", Toast.LENGTH_SHORT).show()
                }

                publicationsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error here
                Log.e("FirebaseError", "Error fetching publications: ${error.message}")
                Toast.makeText(context, "Failed to fetch publications. Please try again later.", Toast.LENGTH_SHORT).show()
            }
        })

        // Update your RecyclerView or UI with the fetched publications
        publicationsAdapter = PublicationsAdapter(publications ,requireContext()) { postId ->
            showCommentsDialog(postId)
        }
        publicationsAdapter.notifyItemInserted(publications.size - 1)

        binding.rvPublications.adapter = publicationsAdapter
        binding.rvPublications.layoutManager = LinearLayoutManager(requireContext())
        binding.fabAddPublication.setOnClickListener {
            showAddPublicationDialog()
        }
        return binding.root
    }

    private fun showCommentsDialog(postId: String) {
        val dialogBinding = DialogCommentsBinding.inflate(layoutInflater)
        val comments = mutableListOf<Comment>()

        // Load comments specific to this postId
        database.child("publications/$postId/comments")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    comments.clear()

                    // Retrieve comments
                    for (postSnapshot in snapshot.children) {
                        val id = postSnapshot.key ?: ""
                        val text = postSnapshot.child("text").getValue(String::class.java) ?: "No content"
                        val idUser = postSnapshot.child("idUser").getValue(String::class.java) ?: "Unknown"

                        val userIdsToFetch = mutableSetOf<String>()
                        if (idUser != "Unknown") {
                            userIdsToFetch.add(idUser)
                        }

                        var comment = Comment(id, text, idUser)
                        // Fetch usernames for the collected user IDs
                        fetchUsernames(userIdsToFetch) { usernamesMap ->
                            // Update the username of the comment based on the user ID
                            val username = usernamesMap[comment.idUser] ?: "Unknown"
                            comment.idUser = username // Update the username for this comment
                            comments.add(comment)
                            commentsAdapter.notifyItemInserted(comments.size - 1)
                        }
                    }

                    // Update the adapter with the retrieved comments
                    commentsAdapter = CommentsAdapter(comments, postId)
                    dialogBinding.rvComments.adapter = commentsAdapter
                    dialogBinding.rvComments.layoutManager = LinearLayoutManager(requireContext())

                    // Display the comments
                    commentsAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error fetching comments: ${error.message}")
                }
            })

        // Check the user's role and adjust visibility of EditText and Send button
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            database.child("users/$userId/role").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val role = snapshot.getValue(String::class.java)
                    if (role == "Patient") {
                        // Hide EditText and Send button for patients
                        dialogBinding.etComment.visibility = View.GONE
                        dialogBinding.btnSend.visibility = View.GONE
                    } else {
                        // Show EditText and Send button for doctors
                        dialogBinding.etComment.visibility = View.VISIBLE
                        dialogBinding.btnSend.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error fetching user role: ${error.message}")
                }
            })
        } else {
            // If the user is not logged in, hide EditText and Send button
            dialogBinding.etComment.visibility = View.GONE
            dialogBinding.btnSend.visibility = View.GONE
        }

        // Handle sending new comments
        dialogBinding.btnSend.setOnClickListener {
            val commentText = dialogBinding.etComment.text.toString()
            if (commentText.isNotEmpty()) {
                // Check if the user is a doctor
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userId = currentUser.uid
                    database.child("users/$userId/role").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val role = snapshot.getValue(String::class.java)
                            if (role == "Doctor") {
                                val commentKey = database.child("publications/$postId/comments").push().key

                                if (commentKey != null) {
                                    val comment = Comment(
                                        id = commentKey,
                                        text = commentText,
                                        idUser = userId
                                    )

                                    // Add the comment to Firebase
                                    database.child("publications/$postId/comments/$commentKey")
                                        .setValue(comment)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Comment added successfully!", Toast.LENGTH_SHORT).show()
                                            dialogBinding.etComment.text.clear()
                                            val userIdsToFetch = mutableSetOf<String>()
                                            userIdsToFetch.add(comment.idUser)
                                            fetchUsernames(userIdsToFetch) { usernamesMap ->
                                                // Update the username of the comment based on the user ID
                                                val username = usernamesMap[comment.idUser] ?: "Unknown"
                                                comment.idUser = username // Update the username for this comment
                                                comments.add(comment)
                                                commentsAdapter.notifyItemInserted(comments.size - 1)
                                            }
                                        }
                                        .addOnFailureListener { error ->
                                            Toast.makeText(context, "Failed to add comment: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            } else {
                                Toast.makeText(context, "Only doctors can post comments!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("FirebaseError", "Error fetching user role: ${error.message}")
                        }
                    })
                } else {
                    Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Comment cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        // Display the dialog
        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setTitle("Comments for Post")
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    // Fetch usernames based on user IDs
    private fun fetchUsernames(userIds: Set<String>, callback: (Map<String, String>) -> Unit) {
        val usernamesMap = mutableMapOf<String, String>()
        val usersRef = database.child("users")

        val remainingUsers = userIds.size
        for (userId in userIds) {
            usersRef.child(userId).child("fullName").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.getValue(String::class.java) ?: "Unknown"
                    usernamesMap[userId] = username

                    if (usernamesMap.size == remainingUsers) {
                        callback(usernamesMap) // All usernames fetched, invoke callback
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error fetching username for user $userId: ${error.message}")
                    if (usernamesMap.size == remainingUsers) {
                        callback(usernamesMap) // Return partial map if error occurs
                    }
                }
            })
        }
    }

    private fun showAddPublicationDialog() {
        // Example dialog to add publication
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.hint = "Write your publication content here"
        builder.setView(input)
        builder.setTitle("Add Publication")
        builder.setPositiveButton("Add") { dialog, _ ->
            val content = input.text.toString()
            if (content.isNotEmpty()) {
                addPublicationToFirebase(content)
            } else {
                Toast.makeText(requireContext(), "Content cannot be empty!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun addPublicationToFirebase(content: String) {
        val newPublicationKey = database.child("publications").push().key
        if (newPublicationKey != null) {
            val publication = Publication(
                id = newPublicationKey,
                name = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous",
                content = content,
                visible = true // By default, it is visible
            )

            database.child("publications/$newPublicationKey")
                .setValue(publication)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Publication added!", Toast.LENGTH_SHORT).show()
                    publications.add(publication)
                    publicationsAdapter.notifyItemInserted(publications.size - 1)
                }
                .addOnFailureListener { error ->
                    Toast.makeText(requireContext(), "Failed to add publication: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}