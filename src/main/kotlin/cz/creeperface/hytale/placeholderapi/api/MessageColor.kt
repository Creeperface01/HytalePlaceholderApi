package cz.creeperface.hytale.placeholderapi.api

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

data class MessageColor(val hex: String) {

    companion object {
        private val HEX6 = Regex("[0-9a-f]{6}")
        private val HEX3 = Regex("[0-9a-f]{3}")

        fun rgb(r: Int, g: Int, b: Int) = MessageColor(
            "%02x%02x%02x".format(
                r.coerceIn(0, 255),
                g.coerceIn(0, 255),
                b.coerceIn(0, 255)
            )
        )

        fun hex(input: String): MessageColor? {
            val normalized = input.trim().removePrefix("#").lowercase()
            return when {
                HEX6.matches(normalized) -> MessageColor(normalized)
                HEX3.matches(normalized) -> MessageColor(
                    buildString { normalized.forEach { append(it).append(it) } }
                )

                else -> null
            }
        }

        fun tailwind(name: String, shade: Int = 500): MessageColor? =
            TailwindColors.get(name, shade)?.let(::MessageColor)

        // OKLCH → sRGB hex. L in [0,1], C in ~[0,0.4], H in degrees.
        // Conversion per Björn Ottosson's OKLab; out-of-gamut values are clipped to sRGB.
        fun oklch(lightness: Double, chroma: Double, hueDeg: Double): MessageColor {
            val hueRad = Math.toRadians(hueDeg)
            val a = chroma * cos(hueRad)
            val b = chroma * sin(hueRad)

            val lPrime = lightness + 0.3963377774 * a + 0.2158037573 * b
            val mPrime = lightness - 0.1055613458 * a - 0.0638541728 * b
            val sPrime = lightness - 0.0894841775 * a - 1.2914855480 * b

            val l3 = lPrime * lPrime * lPrime
            val m3 = mPrime * mPrime * mPrime
            val s3 = sPrime * sPrime * sPrime

            val rLin = 4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3
            val gLin = -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3
            val bLin = -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3

            return rgb(linearToSrgb(rLin), linearToSrgb(gLin), linearToSrgb(bLin))
        }

        private fun linearToSrgb(linear: Double): Int {
            val clamped = linear.coerceIn(0.0, 1.0)
            val gamma = if (clamped <= 0.0031308) 12.92 * clamped
            else 1.055 * clamped.pow(1.0 / 2.4) - 0.055
            return (gamma * 255).roundToInt().coerceIn(0, 255)
        }
    }
}
