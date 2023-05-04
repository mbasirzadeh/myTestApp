package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemMusicBinding
import com.example.myapplication.models.Audio
import javax.inject.Inject

class MusicAdapter @Inject constructor() : RecyclerView.Adapter<MusicAdapter.ViewHolder>() {

    //item binding
    lateinit var binding: ItemMusicBinding

    //audio list
    var audioList= mutableListOf<Audio>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding= ItemMusicBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(audioList[position])
        holder.setIsRecyclable(false)
    }

    override fun getItemCount()=audioList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        //init Views
        fun bind(audio: Audio){
            binding.apply {
                title.text=audio.title
                duration.text=audio.duration
                binding.root.setOnClickListener {
                    onMusicListener.invoke(audio)
                }
            }
        }
    }

    lateinit var onMusicListener:(Audio)->Unit

    fun onMusicClickListener(listener: (Audio)->Unit){
        onMusicListener=listener
    }

    fun setData(newAudioList: MutableList<Audio>){
        val differ= DiffUtil.calculateDiff(DiffUtilCallBack(audioList,newAudioList))
        audioList=newAudioList
        differ.dispatchUpdatesTo(this)
    }


    inner class DiffUtilCallBack (private val oldList: MutableList<Audio>,
                                  private val newList: MutableList<Audio>): DiffUtil.Callback(){
        override fun getOldListSize()=oldList.size

        override fun getNewListSize()=newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition]==newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition]==newList[newItemPosition]
        }

    }

}