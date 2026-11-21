package com.example.integrations

import android.content.Context
import com.example.detection.rules.RuleSource

class AssetRuleSource(private val context: Context) : RuleSource {
    override fun readRuleText(fileName: String): String {
        return context.assets.open("rules/$fileName").bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }
    }
}
