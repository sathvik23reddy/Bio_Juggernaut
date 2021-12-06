package com.example.bio_juggernaut.models

data class Post (
    val text: String = "",
    val createdBy: User = User(),
    val createdAt: Long = 0L,
    val latitude: String = "",
    val longitude: String = "",
    val imgUri: String = "",
    val address: String = "",
    var isResolved : Boolean = false
    )