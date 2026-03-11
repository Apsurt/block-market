package com.apsurt.blockmarket.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

// The data we actually want to save
data class MarketConfigData(
    var transactionFeePercent: Double = 5.0, // E.g., a 5% tax
    var itemBlacklist: List<String> = listOf("minecraft:bedrock", "minecraft:barrier", "minecraft:command_block")
)

object MarketConfig {
    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile = File(FabricLoader.getInstance().configDir.toFile(), "block-market.json")

    // This is what the rest of your mod will read from
    var data: MarketConfigData = MarketConfigData()
        private set

    fun load() {
        if (configFile.exists()) {
            FileReader(configFile).use { reader ->
                data = GSON.fromJson(reader, MarketConfigData::class.java) ?: MarketConfigData()
            }
        } else {
            // If the file doesn't exist yet, save the default values to generate it
            save()
        }
    }

    private fun save() {
        FileWriter(configFile).use { writer ->
            GSON.toJson(data, writer)
        }
    }
}