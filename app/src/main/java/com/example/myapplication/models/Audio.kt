package com.example.myapplication.models

import android.os.Parcelable

@kotlinx.parcelize.Parcelize
class Audio(
    var path:String,
    var title:String,
    var duration: String,
    var artist: String
) : Parcelable