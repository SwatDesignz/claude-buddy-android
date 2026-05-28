// Lifecycle helper for PetEntity
package com.example.data.model

import com.example.data.db.PetEntity

val PetEntity.lifecycle: String get() = when (level) {
    in 1..9 -> "Baby"
    in 10..19 -> "Teen"
    else -> "Adult"
}
