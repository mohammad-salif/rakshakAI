package com.example.detection.rules

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WeightedPhrase(
    val phrase: String,
    val weight: Int
)

@Serializable
data class LexiconFile(
    val language: String,
    @SerialName("hinglish_markers") val hinglishMarkers: List<String> = emptyList(),
    val groups: Map<String, List<WeightedPhrase>> = emptyMap(),
    @SerialName("legit_markers") val legitMarkers: List<String> = emptyList()
)

@Serializable
data class CategoryRule(
    val id: String,
    @SerialName("display_name_en") val displayNameEn: String,
    @SerialName("display_name_hi") val displayNameHi: String,
    @SerialName("required_any_groups") val requiredAnyGroups: List<List<String>>,
    val boosters: List<String> = emptyList(),
    @SerialName("base_weight") val baseWeight: Int = 20
)

@Serializable
data class CategoriesFile(
    val categories: List<CategoryRule>
)

@Serializable
data class BrandRule(
    val id: String,
    val name: String,
    val aliases: List<String>,
    @SerialName("official_domains") val officialDomains: List<String>
)

@Serializable
data class BrandsFile(
    val brands: List<BrandRule>
)

@Serializable
data class SuspiciousTldsFile(
    @SerialName("suspicious_tlds") val suspiciousTlds: List<String>,
    val shorteners: List<String>,
    @SerialName("suspicious_path_keywords") val suspiciousPathKeywords: List<String>
)

@Serializable
data class PublicSuffixesFile(
    @SerialName("two_level_suffixes") val twoLevelSuffixes: List<String>
)

@Serializable
data class NextMoveRule(
    val stage: Int,
    @SerialName("title_en") val titleEn: String,
    @SerialName("title_hi") val titleHi: String,
    val likelihood: String,
    @SerialName("advice_en") val adviceEn: String,
    @SerialName("advice_hi") val adviceHi: String
)

@Serializable
data class StageRule(
    val stage: Int,
    val code: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("name_hi") val nameHi: String,
    @SerialName("next_moves") val nextMoves: List<NextMoveRule>
)

@Serializable
data class StageTransitionsFile(
    val stages: List<StageRule>
)

@Serializable
data class ExplanationsFile(
    val verdicts: Map<String, String>,
    @SerialName("elderly_verdicts") val elderlyVerdicts: Map<String, String>,
    @SerialName("intent_labels") val intentLabels: Map<String, String>,
    @SerialName("manipulation_descriptions") val manipulationDescriptions: Map<String, String>,
    @SerialName("category_consequences") val categoryConsequences: Map<String, String>
)

data class RuleBundle(
    val lexiconEn: LexiconFile,
    val lexiconHi: LexiconFile,
    val lexiconHinglish: LexiconFile,
    val categories: CategoriesFile,
    val brands: BrandsFile,
    val suspiciousTlds: SuspiciousTldsFile,
    val publicSuffixes: PublicSuffixesFile,
    val stageTransitions: StageTransitionsFile,
    val explanationsEn: ExplanationsFile,
    val explanationsHi: ExplanationsFile
)

sealed interface RuleLoadResult {
    data class Loaded(val bundle: RuleBundle) : RuleLoadResult
    data class Unavailable(val reason: String) : RuleLoadResult
}

interface RuleSource {
    fun readRuleText(fileName: String): String
}

class RuleLoader(private val source: RuleSource) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun load(): RuleLoadResult {
        return try {
            val lexiconEn = json.decodeFromString<LexiconFile>(source.readRuleText("lexicon_en.json"))
            val lexiconHi = json.decodeFromString<LexiconFile>(source.readRuleText("lexicon_hi.json"))
            val lexiconHinglish = json.decodeFromString<LexiconFile>(source.readRuleText("lexicon_hinglish.json"))
            val categories = json.decodeFromString<CategoriesFile>(source.readRuleText("categories.json"))
            val brands = json.decodeFromString<BrandsFile>(source.readRuleText("brands.json"))
            val suspiciousTlds = json.decodeFromString<SuspiciousTldsFile>(source.readRuleText("suspicious_tlds.json"))
            val publicSuffixes = json.decodeFromString<PublicSuffixesFile>(source.readRuleText("public_suffixes.json"))
            val stageTransitions = json.decodeFromString<StageTransitionsFile>(source.readRuleText("stage_transitions.json"))
            val explanationsEn = json.decodeFromString<ExplanationsFile>(source.readRuleText("explanations_en.json"))
            val explanationsHi = json.decodeFromString<ExplanationsFile>(source.readRuleText("explanations_hi.json"))

            if (lexiconEn.groups.isEmpty() || categories.categories.isEmpty() || brands.brands.isEmpty()) {
                return RuleLoadResult.Unavailable("Rule definitions are empty or incomplete.")
            }

            RuleLoadResult.Loaded(
                RuleBundle(
                    lexiconEn = lexiconEn,
                    lexiconHi = lexiconHi,
                    lexiconHinglish = lexiconHinglish,
                    categories = categories,
                    brands = brands,
                    suspiciousTlds = suspiciousTlds,
                    publicSuffixes = publicSuffixes,
                    stageTransitions = stageTransitions,
                    explanationsEn = explanationsEn,
                    explanationsHi = explanationsHi
                )
            )
        } catch (e: Exception) {
            RuleLoadResult.Unavailable("Detector unavailable: unable to load rule definitions (${e.message ?: "invalid rules"}). No analysis performed.")
        }
    }
}
