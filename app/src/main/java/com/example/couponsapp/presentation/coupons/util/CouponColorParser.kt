package com.example.couponsapp.presentation.coupons.util

import com.example.couponsapp.R

fun Int.toColorRes(): Int {
    return when {
        this <= 20 -> R.color.green
        this <= 30 -> R.color.yellow
        else -> R.color.red
    }
}

fun Int.toDarkColorRes(): Int {
    return when {
        this <= 20 -> R.color.green_dark
        this <= 30 -> R.color.yellow_dark
        else -> R.color.red_dark
    }
}