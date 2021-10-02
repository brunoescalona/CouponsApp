package com.example.couponsapp.domain.use_cases

import com.example.couponsapp.domain.CouponRepository

class ChangeCouponState(
    private val repository: CouponRepository
) {
    operator fun invoke(couponId: Long) {
        return repository.changeState(couponId)
    }
}