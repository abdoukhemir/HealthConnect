package eniso.ia2.healthconnect2
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter

import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import eniso.ia2.healthconnect2.databinding.DialogSignalBinding
import eniso.ia2.healthconnect2.sampledata.Publication
import eniso.ia2.healthconnect2.databinding.ItemPublicationBinding
import eniso.ia2.healthconnect2.sampledata.Signal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class PublicationsAdapter(
    private val publications: List<Publication>,
    private val context: Context, // Add context parameter
    private val onCommentClick: (String) -> Unit
) : RecyclerView.Adapter<PublicationsAdapter.PublicationViewHolder>() {

    inner class PublicationViewHolder(private val binding: ItemPublicationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(publication: Publication) {
            binding.tvName.text = publication.name
            binding.tvContent.text = publication.content
            binding.ivProfileImage.setImageResource(R.drawable.profile)
            binding.tvDate.text = publication.Date

            // Comment button click listener
            binding.ivComment.setOnClickListener {
                onCommentClick(publication.id)
            }

            // Signal button click listener
            binding.ivSignal.setOnClickListener {
                onSignalClick(publication.id)
            }
        }

        private fun onSignalClick(postId: String) {
            val dialogBinding = DialogSignalBinding.inflate(LayoutInflater.from(context))
            val dialog = AlertDialog.Builder(context)
                .setView(dialogBinding.root)
                .setTitle("Signal Post")
                .setPositiveButton("Send") { dialog, _ ->
                    // Get the selected signal type from the Spinner
                    val signalType = dialogBinding.spinnerSignalType.selectedItem as String
                    val signalDescription = dialogBinding.etSignalDescription.text.toString()

                    if (signalType.isNotEmpty() && signalDescription.isNotEmpty()) {
                        saveSignalToFirebase(postId, signalType, signalDescription)
                    } else {
                        Toast.makeText(context, "Please fill all fields!", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            // Set up the Spinner adapter
            val signalTypes = context.resources.getStringArray(R.array.signal_types)
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, signalTypes)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dialogBinding.spinnerSignalType.adapter = adapter

            dialog.show()
        }
    }
    private fun saveSignalToFirebase(postId: String, signalType: String, signalDescription: String) {
        Log.d("SignalDebug", "Saving signal for postId: $postId")
        Log.d("SignalDebug", "Signal type: $signalType, description: $signalDescription")

        if (context == null) {
            Log.e("SignalError", "Context is null")
            return
        }

        if (postId.isNullOrEmpty()) {
            Log.e("SignalError", "Post ID is null or empty")
            return
        }

        val database = FirebaseDatabase.getInstance().reference
        if (database == null) {
            Log.e("SignalError", "Database reference is null")
            return
        }

        val signalKey = database.child("publications/$postId/signals").push().key
        if (signalKey != null) {
            Log.d("SignalDebug", "Signal key generated: $signalKey")

            val signal = Signal(
                id = signalKey,
                type = signalType,
                description = signalDescription,
                Date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
            )

            database.child("publications/$postId/signals/$signalKey")
                .setValue(signal)
                .addOnSuccessListener {
                    Log.d("SignalDebug", "Signal saved successfully")
                    Toast.makeText(context, "Signal sent successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Log.e("FirebaseError", "Error saving signal: ${error.message}")
                    Toast.makeText(context, "Failed to send signal: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("SignalError", "Failed to generate signal key")
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicationViewHolder {
        val binding = ItemPublicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PublicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PublicationViewHolder, position: Int) {
        holder.bind(publications[position])
    }

    override fun getItemCount(): Int = publications.size
}
