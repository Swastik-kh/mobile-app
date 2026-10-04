package com.example.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    var id: String = "",
    var username: String = "",
    var password: String = "",
    var role: String = "",
    var allowedMenus: List<String> = emptyList(),
    var isFrozen: Boolean = false,
    var parentId: String? = null,
    var fullName: String = "",
    var email: String = "",
    var organizationName: String = "",
    var designation: String = ""
)
