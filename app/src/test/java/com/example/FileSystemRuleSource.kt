package com.example

import com.example.detection.rules.RuleSource
import java.io.File

class FileSystemRuleSource : RuleSource {
    private val baseDir: File by lazy {
        val candidates = listOf(
            File("src/main/assets/rules"),
            File("app/src/main/assets/rules")
        )
        candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not locate assets/rules directory from ${File(".").absolutePath}")
    }

    override fun readRuleText(fileName: String): String {
        return File(baseDir, fileName).readText(Charsets.UTF_8)
    }
}
