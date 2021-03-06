package com.example.couponsapp.data

import android.util.Log
import com.example.couponsapp.data.local.CouponDao
import com.example.couponsapp.data.local.mappers.toDomain
import com.example.couponsapp.data.local.mappers.toEntity
import com.example.couponsapp.data.remote.CouponService
import com.example.couponsapp.data.remote.mappers.toDomain
import com.example.couponsapp.domain.CouponRepository
import com.example.couponsapp.domain.models.Coupon
import com.example.couponsapp.domain.models.State
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import javax.inject.Inject

class CouponRepositoryImplementation @Inject constructor(
    private val couponService: CouponService,
    private val couponDao: CouponDao,
    externalScope: CoroutineScope
) : CouponRepository {

    private val couponsFlow = flow {
        coroutineScope {
            launch { updateCouponList() }
            couponDao.getAll().collect { emit(it) }
        }
    }
        .shareIn(externalScope, Lazily, 1)
        .map { list -> list.map { it.toDomain() } }
        .map { Result.success(it) }
        .catch { emit(Result.failure(it)) }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()

    override fun getAll(): Flow<Result<List<Coupon>>> = couponsFlow
        .catch { emit(Result.failure(it)) }
        .onEach { Log.d(TAG, "emit new result $it") }


    override fun get(couponId: Long): Flow<Coupon?> {
        return couponsFlow
            .map { result -> result.getOrNull()?.find { it.id == couponId } }
            .onEach { Log.d(TAG, "emit coupon $it") }
    }

    override suspend fun changeState(couponId: Long) {
        val localCoupon = get(couponId).firstOrNull()
        val pendingCoupon = localCoupon?.copy(state = State.Pending)
        Log.d(TAG, "insert pending coupon ${pendingCoupon?.id} -- STATE ${pendingCoupon?.state}")
        pendingCoupon?.toEntity()?.let { couponDao.insert(it) }
        delay((Math.random() * 1000).toLong()) // To emulate a network call

        try {
            val coupon = couponService
                .putCouponState(couponId)
                .toDomain()
                .toEntity()
            Log.d(TAG, "insert coupon with the updated state ${coupon.id} -- STATE ${coupon.state}")

            couponDao.insert(coupon)
        } catch (exception: Exception) {
            // if the request failed set the state to the default value
            localCoupon?.toEntity()?.let { couponDao.insert(it) }
            Log.d(TAG, "put coupon state failed with $exception")
        }
    }

    private suspend fun updateCouponList() {
        delay((Math.random() * 1000).toLong()) // To emulate a network call
        val couponList = couponService.getCoupons()
            .coupons
            .map { it.toDomain() }
        storeCouponsLocally(couponList)
    }

    private fun storeCouponsLocally(coupons: List<Coupon>) {
        coupons
            .map { it.toEntity() }
            .apply { couponDao.insertAll(*this.toTypedArray()) }
    }

    companion object {
        private const val TAG = "CouponRepositoryImpl"
    }
}