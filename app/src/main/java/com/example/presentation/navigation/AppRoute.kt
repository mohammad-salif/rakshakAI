package com.example.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable data object Splash : AppRoute
    @Serializable data object Onboarding : AppRoute

    // 5 Bottom Navigation Tabs
    @Serializable data object Home : AppRoute
    @Serializable data class ScanHub(val initialTab: String = "MESSAGE") : AppRoute
    @Serializable data object Protect : AppRoute
    @Serializable data object Vault : AppRoute
    @Serializable data object More : AppRoute

    // Shared & Intervention Screens
    @Serializable data object Result : AppRoute
    @Serializable data object ConversationTimeline : AppRoute
    @Serializable data class PreActionCheck(val preselectedAction: String = "PAY_UPI") : AppRoute
    @Serializable data object IsolationBreaker : AppRoute
    @Serializable data object EmergencyAssistant : AppRoute
    @Serializable data object FamilyGuardian : AppRoute
    @Serializable data object SimulationLab : AppRoute
    @Serializable data object PrivacyCentre : AppRoute
    @Serializable data object SafetyCentre : AppRoute
    @Serializable data object Settings : AppRoute
    @Serializable data object AboutLimitations : AppRoute
}
