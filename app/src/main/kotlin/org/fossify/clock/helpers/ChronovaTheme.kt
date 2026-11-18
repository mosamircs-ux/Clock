package org.fossify.clock.helpers

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import org.fossify.clock.R
import org.fossify.clock.extensions.config
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor

/**
 * Provides the signature Chronova Clock "Midnight Indigo & Warm Amber"
 * theme defaults and adaptive Material 3 card surfaces.
 */
object ChronovaTheme {
    const val MIDNIGHT_BG = 0xFF0B1026.toInt()
    const val SURFACE_CARD = 0xFF141D3D.toInt()
    const val SURFACE_ELEVATED = 0xFF1C2854.toInt()
    const val SURFACE_BOTTOM_BAR = 0xFF0F1633.toInt()
    const val BORDER_SUBTLE = 0xFF253466.toInt()
    const val AMBER_PRIMARY = 0xFFF59E0B.toInt()
    const val AMBER_ACCENT = 0xFFFBBF24.toInt()
    const val TEXT_PRIMARY = 0xFFF8FAFC.toInt()
    const val TEXT_SECONDARY = 0xFF94A3B8.toInt()

    fun applyDefaultsIfNeeded(context: Context) {
        val cfg = context.config
        if (!cfg.wasChronovaThemeInitialized) {
            cfg.wasChronovaThemeInitialized = true
            cfg.isSystemThemeEnabled = false
            cfg.isGlobalThemeEnabled = false
            cfg.backgroundColor = MIDNIGHT_BG
            cfg.customBackgroundColor = MIDNIGHT_BG
            cfg.primaryColor = AMBER_PRIMARY
            cfg.customPrimaryColor = AMBER_PRIMARY
            cfg.accentColor = AMBER_ACCENT
            cfg.customAccentColor = AMBER_ACCENT
            cfg.textColor = TEXT_PRIMARY
            cfg.customTextColor = TEXT_PRIMARY
            cfg.widgetBgColor = ColorUtils.setAlphaComponent(MIDNIGHT_BG, 215)
            cfg.widgetTextColor = TEXT_PRIMARY
        }
    }

    fun isDarkCanvas(context: Context): Boolean {
        val bg = context.getProperBackgroundColor()
        return ColorUtils.calculateLuminance(bg) < 0.35
    }

    fun cardDrawable(context: Context, isActive: Boolean = false, cornerDp: Float = 20f): Drawable {
        val density = context.resources.displayMetrics.density
        val radiusPx = cornerDp * density
        val primary = context.getProperPrimaryColor()

        return if (isDarkCanvas(context)) {
            if (isActive) {
                GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(0xFF1E2C5E.toInt(), 0xFF151F44.toInt())
                ).apply {
                    cornerRadius = radiusPx
                    setStroke((1.25f * density).toInt().coerceAtLeast(1), ColorUtils.setAlphaComponent(primary, 130))
                }
            } else {
                GradientDrawable().apply {
                    setColor(SURFACE_CARD)
                    cornerRadius = radiusPx
                    setStroke((1f * density).toInt().coerceAtLeast(1), BORDER_SUBTLE)
                }
            }
        } else {
            val fill = ContextCompat.getColor(context, R.color.chronova_light_card)
            val stroke = if (isActive) {
                ColorUtils.setAlphaComponent(primary, 140)
            } else {
                ContextCompat.getColor(context, R.color.chronova_light_border)
            }
            GradientDrawable().apply {
                setColor(fill)
                cornerRadius = radiusPx
                setStroke((1.25f * density).toInt().coerceAtLeast(1), stroke)
            }
        }
    }

    fun heroClockDrawable(context: Context): Drawable {
        val density = context.resources.displayMetrics.density
        val primary = context.getProperPrimaryColor()
        return if (isDarkCanvas(context)) {
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF1C2A5A.toInt(), 0xFF121A3B.toInt(), 0xFF0E1531.toInt())
            ).apply {
                cornerRadius = 28f * density
                setStroke((1.2f * density).toInt().coerceAtLeast(1), ColorUtils.setAlphaComponent(primary, 96))
            }
        } else {
            cardDrawable(context, isActive = true, cornerDp = 28f)
        }
    }

    fun pillDrawable(context: Context): Drawable {
        val density = context.resources.displayMetrics.density
        val primary = context.getProperPrimaryColor()
        return GradientDrawable().apply {
            setColor(ColorUtils.setAlphaComponent(primary, if (isDarkCanvas(context)) 38 else 28))
            cornerRadius = 999f * density
            setStroke((1f * density).toInt().coerceAtLeast(1), ColorUtils.setAlphaComponent(primary, 120))
        }
    }

    fun secondaryCircleButtonDrawable(context: Context): Drawable {
        val density = context.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (isDarkCanvas(context)) {
                setColor(SURFACE_ELEVATED)
                setStroke((1.25f * density).toInt().coerceAtLeast(1), BORDER_SUBTLE)
            } else {
                setColor(ContextCompat.getColor(context, R.color.chronova_light_card))
                setStroke((1.25f * density).toInt().coerceAtLeast(1), ContextCompat.getColor(context, R.color.chronova_light_border))
            }
        }
    }

    fun bottomBarBackgroundColor(context: Context): Int {
        return if (isDarkCanvas(context)) SURFACE_BOTTOM_BAR else Color.WHITE
    }

    fun contrastOnPrimary(primaryColor: Int): Int {
        return if (ColorUtils.calculateLuminance(primaryColor) > 0.45) {
            MIDNIGHT_BG
        } else {
            Color.WHITE
        }
    }
}
