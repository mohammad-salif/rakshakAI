package com.example

import android.content.ContextWrapper
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.domain.models.Source
import com.example.integrations.notifications.NotificationsHelper
import com.example.presentation.RakshakViewModel
import com.example.presentation.components.MainBottomTab
import com.example.presentation.components.RakshakBottomNavBar
import com.example.presentation.components.RakshakSubTopBar
import com.example.presentation.components.RakshakTopBar
import com.example.presentation.navigation.AppRoute
import com.example.presentation.screens.AboutLimitationsScreen
import com.example.presentation.screens.ConversationTimelineScreen
import com.example.presentation.screens.EmergencyAssistantScreen
import com.example.presentation.screens.FamilyGuardianScreen
import com.example.presentation.screens.HomeDashboardScreen
import com.example.presentation.screens.IsolationBreakerScreen
import com.example.presentation.screens.MoreHubScreen
import com.example.presentation.screens.OnboardingScreen
import com.example.presentation.screens.PreActionCheckScreen
import com.example.presentation.screens.PrivacyCentreScreen
import com.example.presentation.screens.ProtectHubScreen
import com.example.presentation.screens.ResultScreen
import com.example.presentation.screens.SafetyCentreScreen
import com.example.presentation.screens.ScanHubScreen
import com.example.presentation.screens.SettingsScreen
import com.example.presentation.screens.SimulationLabScreen
import com.example.presentation.screens.SplashScreen
import com.example.presentation.screens.VaultTabScreen
import com.example.presentation.theme.RakshakColors
import com.example.presentation.theme.RakshakTheme
import java.util.Locale

class MainActivity : FragmentActivity() {

    private var activeViewModel: RakshakViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as KavachApplication
        val factory = RakshakViewModel.Factory(app)

        setContent {
            val vm: RakshakViewModel = viewModel(factory = factory)
            activeViewModel = vm
            val uiState by vm.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(intent) {
                dispatchExternalIntent(intent, vm)
            }

            LocalizedContextWrapper(languageCode = uiState.settings.appLanguage) {
                RakshakTheme(
                    elderlyMode = uiState.settings.elderlyMode,
                    reducedMotion = uiState.settings.reducedMotion
                ) {
                    RakshakAppShell(
                        viewModel = vm,
                        onUpdateFlagSecure = { secure ->
                            if (secure) {
                                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            } else {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeViewModel?.let { dispatchExternalIntent(intent, it) }
    }

    private fun dispatchExternalIntent(intent: Intent?, vm: RakshakViewModel) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val type = intent.type.orEmpty()
                if (type.startsWith("text/")) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                    if (sharedText.isNotBlank()) {
                        vm.handleIncomingSharedText(sharedText)
                    }
                    intent.action = null
                } else if (type.startsWith("image/")) {
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    if (uri != null) {
                        vm.handleIncomingSharedImage(uri)
                    }
                    intent.action = null
                }
            }
        }

        val notifText = intent.getStringExtra(NotificationsHelper.EXTRA_NAV_SCAN_RESULT_TEXT)
        if (!notifText.isNullOrBlank()) {
            intent.removeExtra(NotificationsHelper.EXTRA_NAV_SCAN_RESULT_TEXT)
            vm.handleIncomingSharedText(notifText)
        }
    }
}

@Composable
private fun LocalizedContextWrapper(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val registryOwner = LocalActivityResultRegistryOwner.current
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val locale = remember(languageCode) { Locale.forLanguageTag(languageCode) }
    val localizedResources = remember(languageCode, baseConfig) {
        val config = Configuration(baseConfig)
        config.setLocale(locale)
        baseContext.createConfigurationContext(config).resources
    }
    val wrappedContext = remember(baseContext, localizedResources) {
        object : ContextWrapper(baseContext) {
            override fun getResources(): Resources = localizedResources
            override fun getAssets(): AssetManager = localizedResources.assets
        }
    }
    val providers = buildList {
        add(LocalContext provides wrappedContext)
        add(LocalConfiguration provides localizedResources.configuration)
        if (registryOwner != null) {
            add(LocalActivityResultRegistryOwner provides registryOwner)
        }
        if (backDispatcherOwner != null) {
            add(LocalOnBackPressedDispatcherOwner provides backDispatcherOwner)
        }
    }.toTypedArray()
    CompositionLocalProvider(*providers) {
        content()
    }
}

@Composable
private fun RakshakAppShell(
    viewModel: RakshakViewModel,
    onUpdateFlagSecure: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(uiState.pendingNavigationEvent) {
        when (uiState.pendingNavigationEvent) {
            "SCAN_MESSAGE_AUTO" -> {
                viewModel.consumePendingNav()
                viewModel.scanMessage(source = Source.SHARED) {
                    navController.navigate(AppRoute.Result) { launchSingleTop = true }
                }
            }
            "SCAN_SCREENSHOT" -> {
                viewModel.consumePendingNav()
                navController.navigate(AppRoute.ScanHub("SCREENSHOT")) { launchSingleTop = true }
            }
        }
    }

    val isSensitiveScreen = currentDest?.hasRoute<AppRoute.Result>() == true ||
        currentDest?.hasRoute<AppRoute.Vault>() == true
    val shouldSecureFlag = uiState.settings.flagSecureEnabled && isSensitiveScreen
    DisposableEffect(shouldSecureFlag) {
        onUpdateFlagSecure(shouldSecureFlag)
        onDispose { }
    }

    val isTopLevelTab = currentDest?.hasRoute<AppRoute.Home>() == true ||
        currentDest?.hasRoute<AppRoute.ScanHub>() == true ||
        currentDest?.hasRoute<AppRoute.Protect>() == true ||
        currentDest?.hasRoute<AppRoute.Vault>() == true ||
        currentDest?.hasRoute<AppRoute.More>() == true

    val activeBottomTab = when {
        currentDest?.hasRoute<AppRoute.Home>() == true -> MainBottomTab.HOME
        currentDest?.hasRoute<AppRoute.ScanHub>() == true -> MainBottomTab.SCAN
        currentDest?.hasRoute<AppRoute.Protect>() == true -> MainBottomTab.PROTECT
        currentDest?.hasRoute<AppRoute.Vault>() == true -> MainBottomTab.VAULT
        currentDest?.hasRoute<AppRoute.More>() == true -> MainBottomTab.MORE
        else -> MainBottomTab.HOME
    }

    val navigateToTab: (MainBottomTab) -> Unit = { tab ->
        val route: AppRoute = when (tab) {
            MainBottomTab.HOME -> AppRoute.Home
            MainBottomTab.SCAN -> AppRoute.ScanHub("MESSAGE")
            MainBottomTab.PROTECT -> AppRoute.Protect
            MainBottomTab.VAULT -> AppRoute.Vault
            MainBottomTab.MORE -> AppRoute.More
        }
        navController.navigate(route) {
            popUpTo(AppRoute.Home) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val subScreenTitle: String? = when {
        currentDest?.hasRoute<AppRoute.Result>() == true -> "Scan Result"
        currentDest?.hasRoute<AppRoute.ConversationTimeline>() == true -> "Conversation Timeline"
        currentDest?.hasRoute<AppRoute.PreActionCheck>() == true -> "Pre-Action Safety Check"
        currentDest?.hasRoute<AppRoute.IsolationBreaker>() == true -> "Isolation Breaker"
        currentDest?.hasRoute<AppRoute.EmergencyAssistant>() == true -> "Emergency Assistant"
        currentDest?.hasRoute<AppRoute.FamilyGuardian>() == true -> "Family Guardian"
        currentDest?.hasRoute<AppRoute.SimulationLab>() == true -> "Simulation Lab (Demo)"
        currentDest?.hasRoute<AppRoute.PrivacyCentre>() == true -> "Privacy Centre"
        currentDest?.hasRoute<AppRoute.SafetyCentre>() == true -> "Safety Centre"
        currentDest?.hasRoute<AppRoute.Settings>() == true -> "Settings"
        currentDest?.hasRoute<AppRoute.AboutLimitations>() == true -> "About & Limitations"
        else -> null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = RakshakColors.CanvasLight,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            when {
                isTopLevelTab -> {
                    val subtitle = when (activeBottomTab) {
                        MainBottomTab.HOME -> "Your Intelligent Digital Guardian"
                        MainBottomTab.SCAN -> "🔍 Detect — Offline Scam Scanners"
                        MainBottomTab.PROTECT -> "Pre-Action & Emergency Defence"
                        MainBottomTab.VAULT -> "Encrypted Vault & History"
                        MainBottomTab.MORE -> "Privacy, Safety & Settings"
                    }
                    RakshakTopBar(
                        subtitle = subtitle,
                        elderlyMode = uiState.settings.elderlyMode,
                        currentLanguage = uiState.settings.appLanguage,
                        onToggleLanguage = {
                            val nextLang = if (uiState.settings.appLanguage.startsWith("hi")) "en" else "hi"
                            viewModel.setLanguage(nextLang)
                        }
                    )
                }
                subScreenTitle != null -> {
                    RakshakSubTopBar(
                        title = subScreenTitle,
                        onBackClick = { navController.popBackStack() },
                        onHomeClick = { navigateToTab(MainBottomTab.HOME) }
                    )
                }
            }
        },
        bottomBar = {
            if (isTopLevelTab) {
                RakshakBottomNavBar(
                    selectedTab = activeBottomTab,
                    onSelectTab = navigateToTab
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = AppRoute.Splash
            ) {
                composable<AppRoute.Splash> {
                    SplashScreen(
                        onFinished = {
                            val dest = if (uiState.settings.onboardingCompleted) AppRoute.Home else AppRoute.Onboarding
                            navController.navigate(dest) {
                                popUpTo(AppRoute.Splash) { inclusive = true }
                            }
                        }
                    )
                }

                composable<AppRoute.Onboarding> {
                    OnboardingScreen(
                        selectedLanguage = uiState.settings.appLanguage,
                        smsAutoScanEnabled = uiState.settings.smsAutoScanEnabled,
                        onSelectLanguage = viewModel::setLanguage,
                        onToggleSmsAutoScan = viewModel::setSmsAutoScan,
                        onFinish = {
                            viewModel.completeOnboarding()
                            navController.navigate(AppRoute.Home) {
                                popUpTo(AppRoute.Onboarding) { inclusive = true }
                            }
                        }
                    )
                }

                composable<AppRoute.Home> {
                    HomeDashboardScreen(
                        uiState = uiState,
                        onOpenScanTab = { tab ->
                            navController.navigate(AppRoute.ScanHub(tab)) { launchSingleTop = true }
                        },
                        onOpenEmergency = { navController.navigate(AppRoute.EmergencyAssistant) },
                        onOpenSafetyCentre = { navController.navigate(AppRoute.SafetyCentre) },
                        onOpenPrivacyCentre = { navController.navigate(AppRoute.PrivacyCentre) },
                        onOpenPreActionCheck = { navController.navigate(AppRoute.PreActionCheck()) },
                        onOpenSimulationLab = { navController.navigate(AppRoute.SimulationLab) },
                        onOpenVaultHistory = { navigateToTab(MainBottomTab.VAULT) }
                    )
                }

                composable<AppRoute.ScanHub> { entry ->
                    val route = entry.toRoute<AppRoute.ScanHub>()
                    ScanHubScreen(
                        initialTab = route.initialTab,
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateToResult = { navController.navigate(AppRoute.Result) },
                        onNavigateToConversationTimeline = { navController.navigate(AppRoute.ConversationTimeline) },
                        onNavigateToSimulationLab = { navController.navigate(AppRoute.SimulationLab) }
                    )
                }

                composable<AppRoute.Protect> {
                    ProtectHubScreen(
                        uiState = uiState,
                        onOpenPreActionCheck = { navController.navigate(AppRoute.PreActionCheck()) },
                        onOpenEmergencyAssistant = { navController.navigate(AppRoute.EmergencyAssistant) },
                        onOpenIsolationBreaker = { navController.navigate(AppRoute.IsolationBreaker) },
                        onOpenFamilyGuardian = { navController.navigate(AppRoute.FamilyGuardian) },
                        onOpenSimulationLab = { navController.navigate(AppRoute.SimulationLab) }
                    )
                }

                composable<AppRoute.Vault> {
                    VaultTabScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onRestoredToScanner = {
                            navController.navigate(AppRoute.ScanHub("MESSAGE")) { launchSingleTop = true }
                        }
                    )
                }

                composable<AppRoute.More> {
                    MoreHubScreen(
                        onOpenPrivacyCentre = { navController.navigate(AppRoute.PrivacyCentre) },
                        onOpenSafetyCentre = { navController.navigate(AppRoute.SafetyCentre) },
                        onOpenSettings = { navController.navigate(AppRoute.Settings) },
                        onOpenAbout = { navController.navigate(AppRoute.AboutLimitations) }
                    )
                }

                composable<AppRoute.Result> {
                    ResultScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenPreActionCheck = { navController.navigate(AppRoute.PreActionCheck()) },
                        onOpenIsolationBreaker = { navController.navigate(AppRoute.IsolationBreaker) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<AppRoute.ConversationTimeline> {
                    ConversationTimelineScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenIsolationBreaker = { navController.navigate(AppRoute.IsolationBreaker) },
                        onOpenPreActionCheck = { navController.navigate(AppRoute.PreActionCheck()) },
                        onOpenFullResult = { navController.navigate(AppRoute.Result) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<AppRoute.PreActionCheck> {
                    PreActionCheckScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onDone = { navController.popBackStack() }
                    )
                }

                composable<AppRoute.IsolationBreaker> {
                    IsolationBreakerScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenPreActionCheck = { navController.navigate(AppRoute.PreActionCheck()) },
                        onAddContact = { navController.navigate(AppRoute.FamilyGuardian) }
                    )
                }

                composable<AppRoute.EmergencyAssistant> {
                    EmergencyAssistantScreen(viewModel = viewModel)
                }

                composable<AppRoute.FamilyGuardian> {
                    FamilyGuardianScreen(uiState = uiState, viewModel = viewModel)
                }

                composable<AppRoute.SimulationLab> {
                    SimulationLabScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onRunSampleToResult = { navController.navigate(AppRoute.Result) },
                        onRunConversationToTimeline = { navController.navigate(AppRoute.ConversationTimeline) }
                    )
                }

                composable<AppRoute.PrivacyCentre> {
                    PrivacyCentreScreen(uiState = uiState, viewModel = viewModel)
                }

                composable<AppRoute.SafetyCentre> {
                    SafetyCentreScreen(
                        bookmarkedTipIds = uiState.settings.bookmarkedTips,
                        onToggleBookmark = viewModel::toggleBookmarkTip
                    )
                }

                composable<AppRoute.Settings> {
                    SettingsScreen(uiState = uiState, viewModel = viewModel)
                }

                composable<AppRoute.AboutLimitations> {
                    AboutLimitationsScreen()
                }
            }
        }
    }
}
