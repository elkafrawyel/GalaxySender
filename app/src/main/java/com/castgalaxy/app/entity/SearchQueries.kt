package com.castgalaxy.app.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class SearchQueries(
    @Id var id: Long = 0,
    var text: String
)