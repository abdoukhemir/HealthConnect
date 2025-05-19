package eniso.ia2.healthconnect2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eniso.ia2.healthconnect2.sampledata.Comment
import eniso.ia2.healthconnect2.databinding.ItemCommentBinding


class CommentsAdapter(
    private var comments: MutableList<Comment>,
    private val postId: String
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    // ViewHolder class for binding data to the views
    class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment){
            binding.ivProfileImage.setImageResource(R.drawable.profile)
            binding.tvUsername.text=comment.idUser
            binding.tvComment.text=comment.text
            binding.tvDate.text=comment.Date

        }

    }

    // onCreateViewHolder to inflate the layout for each comment item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    // onBindViewHolder to bind data to each view
    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    // Return the total number of comments in the list
    override fun getItemCount(): Int = comments.size


}