package org.fossify.clock.helpers

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.fossify.clock.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages AdMob initialization, anchored adaptive banner ads, and
 * frequency-capped interstitial ads for Chronova Clock.
 *
 * Ads are strictly omitted from time-critical flows (ringing alarms,
 * snooze/dismiss actions, and active stopwatch timing).
 */
object AdManager {
    private const val MIN_INTERSTITIAL_INTERVAL_MS = 90_000L
    private const val ACTIONS_PER_INTERSTITIAL = 2

    private val isInitialized = AtomicBoolean(false)
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var lastInterstitialShownAt = 0L
    private var eligibleActionCount = 0

    fun initialize(context: Context) {
        if (!isInitialized.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        try {
            MobileAds.initialize(appContext) {
                preloadInterstitial(appContext)
            }
        } catch (_: Exception) {
            isInitialized.set(false)
        }
    }

    fun loadAdaptiveBanner(activity: Activity, container: FrameLayout): AdView? {
        return try {
            val adSize = getAnchoredAdaptiveBannerSize(activity, container)
            val adView = AdView(activity).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                setAdSize(adSize)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        container.visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        container.visibility = View.GONE
                    }
                }
            }
            container.removeAllViews()
            container.addView(adView)
            adView.loadAd(AdRequest.Builder().build())
            adView
        } catch (_: Exception) {
            container.visibility = View.GONE
            null
        }
    }

    fun preloadInterstitial(context: Context) {
        if (isLoadingInterstitial || interstitialAd != null) return
        isLoadingInterstitial = true
        val appContext = context.applicationContext
        try {
            InterstitialAd.load(
                appContext,
                BuildConfig.ADMOB_INTERSTITIAL_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isLoadingInterstitial = false
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isLoadingInterstitial = false
                    }
                }
            )
        } catch (_: Exception) {
            interstitialAd = null
            isLoadingInterstitial = false
        }
    }

    /**
     * Records a non-critical user action (e.g., saving an alarm, saving a timer,
     * adding a world clock, or leaving Settings) and displays a preloaded
     * interstitial when both action count and cooldown thresholds are met.
     */
    fun recordActionAndMaybeShowInterstitial(activity: Activity, onDone: (() -> Unit)? = null) {
        eligibleActionCount++
        val now = SystemClock.elapsedRealtime()
        val cooldownMet = lastInterstitialShownAt == 0L ||
            (now - lastInterstitialShownAt) >= MIN_INTERSTITIAL_INTERVAL_MS

        val ad = interstitialAd
        if (ad == null || eligibleActionCount < ACTIONS_PER_INTERSTITIAL || !cooldownMet || activity.isFinishing || activity.isDestroyed) {
            if (ad == null) {
                preloadInterstitial(activity)
            }
            onDone?.invoke()
            return
        }

        eligibleActionCount = 0
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterstitialShownAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                preloadInterstitial(activity)
                onDone?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                preloadInterstitial(activity)
                onDone?.invoke()
            }
        }
        ad.show(activity)
    }

    private fun getAnchoredAdaptiveBannerSize(activity: Activity, container: FrameLayout): AdSize {
        val density = activity.resources.displayMetrics.density
        var adWidthPixels = container.width.toFloat()
        if (adWidthPixels <= 0f) {
            adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.windowManager.currentWindowMetrics.bounds.width().toFloat()
            } else {
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getMetrics(outMetrics)
                outMetrics.widthPixels.toFloat()
            }
        }
        val adWidthDp = (adWidthPixels / density).toInt().coerceAtLeast(320)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp)
    }
}
