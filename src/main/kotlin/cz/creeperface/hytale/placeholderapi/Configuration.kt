package cz.creeperface.hytale.placeholderapi

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec

/**
 * @author CreeperFace
 */
class Configuration {

    var version = 0.toDouble()

    var updateInterval = 10

    var dateFormat = "yyyy-MM-dd"

    var timeFormat = "HH:mm:ss"

    var coordsAccuracy = 2

    var booleanFalseFormat = "no"

    var booleanTrueFormat = "yes"

    var arraySeparator = ", "

    companion object {
        val CODEC: BuilderCodec<Configuration> = BuilderCodec.builder(Configuration::class.java, ::Configuration)
            .append(KeyedCodec("Version", Codec.DOUBLE),
                { simpleClaimsConfig, value, _ -> simpleClaimsConfig.version = value },
                { simpleClaimsConfig, _ -> simpleClaimsConfig.version }).add()
            .append(KeyedCodec("UpdateInterval", Codec.INTEGER),
                { simpleClaimsConfig, value, _ -> simpleClaimsConfig.updateInterval = value },
                { simpleClaimsConfig, _ -> simpleClaimsConfig.updateInterval }).add()
            .append(KeyedCodec("DateFormat", Codec.STRING),
                { simpleClaimsConfig, value, _ -> simpleClaimsConfig.dateFormat = value },
                { simpleClaimsConfig, _ -> simpleClaimsConfig.dateFormat }).add()
            .append(KeyedCodec("TimeFormat", Codec.STRING),
                { simpleClaimsConfig, value, _ -> simpleClaimsConfig.timeFormat = value },
                { simpleClaimsConfig, _ -> simpleClaimsConfig.timeFormat }).add()
            .append(KeyedCodec("CoordsAccuracy", Codec.INTEGER),
                { simpleClaimsConfig, value, _ -> simpleClaimsConfig.coordsAccuracy = value },
                { simpleClaimsConfig, _ -> simpleClaimsConfig.coordsAccuracy }).add()
            .append(KeyedCodec("BooleanFalseFormat", Codec.STRING),
                { simpleClaimsConfig, value, _ -> simpleClaimsConfig.booleanFalseFormat = value },
                { simpleClaimsConfig, _ -> simpleClaimsConfig.booleanFalseFormat }).add()
            .append(KeyedCodec("BooleanTrueFormat", Codec.STRING),
                { simpleClaimsConfig, value, _ -> simpleClaimsConfig.booleanTrueFormat = value },
                { simpleClaimsConfig, _ -> simpleClaimsConfig.booleanTrueFormat }).add()
            .append(KeyedCodec("ArraySeparator", Codec.STRING),
                { simpleClaimsConfig, value, _ -> simpleClaimsConfig.arraySeparator = value },
                { simpleClaimsConfig, _ -> simpleClaimsConfig.arraySeparator }).add()
            .build()
    }
}