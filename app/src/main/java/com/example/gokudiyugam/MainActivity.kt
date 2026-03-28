@file:Suppress("DEPRECATION")

package com.example.gokudiyugam

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.ui.components.MiniPlayer
import com.example.gokudiyugam.ui.screens.*
import com.example.gokudiyugam.ui.theme.GokudiyugamTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import com.example.gokudiyugam.drive.DriveViewModel

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val preferenceManager = remember { PreferenceManager(this) }
            var appLanguage by remember { mutableStateOf(preferenceManager.getLanguage()) }
            var isDarkMode by remember { mutableStateOf(preferenceManager.isDarkMode()) }
            var bgColorInt by remember { mutableIntStateOf(preferenceManager.getBackgroundColor()) }
            
            // Modern Localization: Using AppCompatDelegate for Locale switching
            LaunchedEffect(appLanguage) {
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(appLanguage)
                if (AppCompatDelegate.getApplicationLocales() != appLocale) {
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }

            LaunchedEffect(Unit) {
                try {
                    FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                } catch (e: Exception) {
                    Log.e("Firebase", "Subscription failed: ${e.message}")
                }
            }

            GokudiyugamTheme(darkTheme = isDarkMode) {
                MainAppContent(
                    preferenceManager, 
                    { appLanguage = it }, 
                    { isDarkMode = it }, 
                    { bgColorInt = it }, 
                    bgColorInt
                )
            }
        }
    }

    @Composable
    private fun MainAppContent(
        preferenceManager: PreferenceManager,
        onLanguageChange: (String) -> Unit,
        onDarkModeChange: (Boolean) -> Unit,
        onBgColorChange: (Int) -> Unit,
        bgColorInt: Int
    ) {
        RequestRuntimePermissions()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (bgColorInt == -1) MaterialTheme.colorScheme.background else androidx.compose.ui.graphics.Color(bgColorInt)
        ) {
            val navController = rememberNavController()
            val auth = remember { FirebaseAuth.getInstance() }
            val db = remember { FirebaseFirestore.getInstance("mediadata") }
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            var currentUserRole by remember { mutableStateOf<UserRole?>(null) }
            val kirtanViewModel: KirtanViewModel = viewModel()
            val driveViewModel: DriveViewModel = viewModel()

            LaunchedEffect(Unit) {
                kirtanViewModel.initController(context)
            }

            LaunchedEffect(auth.currentUser) {
                auth.currentUser?.let { user ->
                    db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val roleStr = doc.getString("role")?.uppercase() ?: "NORMAL"
                            currentUserRole = when {
                                roleStr == "HOST" -> UserRole.HOST
                                roleStr == "SUB_HOST" || roleStr == "SUB-HOST" || roleStr == "SUB HOST" -> UserRole.SUB_HOST
                                roleStr == "GUEST" -> UserRole.GUEST
                                else -> UserRole.NORMAL
                            }
                        }
                    }
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    val user = auth.currentUser
                    if (user != null && !user.isAnonymous) {
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> db.collection("users").document(user.uid).update("isOnline", true, "lastSeen", System.currentTimeMillis())
                            Lifecycle.Event.ON_PAUSE -> db.collection("users").document(user.uid).update("isOnline", false, "lastSeen", System.currentTimeMillis())
                            else -> {}
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            
            val isVideoScreen = currentRoute == "guruhari_darshan" || 
                                currentRoute == "puja_darshan" || 
                                currentRoute == "functions" || 
                                currentRoute?.startsWith("video_player") == true

            Scaffold(
                bottomBar = {
                    if (!isLandscape && kirtanViewModel.currentKirtan != null && currentRoute != "kirtan_player") {
                        MiniPlayer(viewModel = kirtanViewModel, onPlayerClick = { navController.navigate("kirtan_player") })
                    }
                },
                contentWindowInsets = if (isLandscape && isVideoScreen) WindowInsets(0, 0, 0, 0) else WindowInsets.systemBars
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = if (auth.currentUser != null) "home" else "login",
                    modifier = if (isLandscape && isVideoScreen) Modifier.fillMaxSize() else Modifier.padding(innerPadding)
                ) {
                    composable("login") { 
                        LoginScreen(
                            preferenceManager = preferenceManager, 
                            onLoginSuccess = { u, r -> currentUserRole = r; preferenceManager.saveCurrentUsername(u); navController.navigate("home") { popUpTo("login") { inclusive = true } } }, 
                            onRequireVerification = { _, _ -> }, 
                            onSignUpClick = { navController.navigate("signup") }, 
                            onEmailSignInClick = { e, p, l, err -> l(true); auth.signInWithEmailAndPassword(e, p).addOnCompleteListener { task -> l(false); if (task.isSuccessful) navController.navigate("home") { popUpTo("login") { inclusive = true } } else err(task.exception?.message ?: "Login failed") } }, 
                            onGuestLoginClick = { l, err -> l(true); auth.signInAnonymously().addOnCompleteListener { task -> l(false); if (task.isSuccessful) navController.navigate("home") { popUpTo("login") { inclusive = true } } else err(task.exception?.message ?: "Guest login failed") } }, 
                            onGoogleSignInClick = { l, err -> /* Handle Google Sign In if needed */ }
                        ) 
                    }
                    composable("signup") { 
                        SignUpScreen(
                            preferenceManager = preferenceManager, 
                            onSignUpSuccess = { u, r -> currentUserRole = r; preferenceManager.saveCurrentUsername(u); navController.navigate("home") { popUpTo("signup") { inclusive = true } } }, 
                            onLoginClick = { navController.navigate("login") }, 
                            onEmailSignUpClick = { u, e, p, l, err -> l(true); auth.createUserWithEmailAndPassword(e, p).addOnCompleteListener { task -> l(false); if (task.isSuccessful) navController.navigate("home") { popUpTo("signup") { inclusive = true } } else err(task.exception?.message ?: "Signup failed") } }
                        ) 
                    }
                    composable("home") { 
                        HomeScreen(
                            currentUserRole = currentUserRole, 
                            onNavigateToDailyDarshan = { navController.navigate("daily_darshan") }, 
                            onNavigateToKirtan = { navController.navigate("kirtan") }, 
                            onNavigateToKirtanLyrics = { navController.navigate("kirtan_lyrics") }, 
                            onNavigateToSabhaTimeTable = { navController.navigate("sabha_timetable") }, 
                            onNavigateToFunctions = { navController.navigate("functions") }, 
                            onNavigateToSatsangNews = { navController.navigate("coming_soon/Satsang News") }, 
                            onNavigateToSabhaSaar = { navController.navigate("sabha_saar") }, 
                            onNavigateToSettings = { navController.navigate("settings") }, 
                            onNavigateToMediaLibrary = { navController.navigate("media_library") }, 
                            onProfileClick = { navController.navigate("profile") }, 
                            onLogout = { auth.signOut(); navController.navigate("login") { popUpTo(0) { inclusive = true } } }, 
                            onNavigateToGoogleDrive = { navController.navigate("google_drive") }, 
                            onNavigateToAdminPanel = { navController.navigate("admin_panel") }
                        ) 
                    }
                    
                    composable("admin_panel") { AdminPanelScreen(currentUserRole, preferenceManager, GoogleSignIn.getLastSignedInAccount(context), { navController.navigate("media_library") }, { navController.popBackStack() }) }
                    composable("profile") { ProfileScreen({ navController.popBackStack() }, { navController.navigate("settings") }, { navController.navigate("settings") }) }
                    composable("media_library") { MediaDataScreen(currentUserRole, { navController.popBackStack() }) }
                    composable("google_drive") { DriveScreen(currentUserRole, { navController.popBackStack() }) }
                    composable("daily_darshan") { DailyDarshanScreen({ navController.popBackStack() }, { navController.navigate("puja_darshan") }, { navController.navigate("mandir_darshan") }, { navController.navigate("guruhari_darshan") }, { navController.navigate("festivals") }) }
                    composable("mandir_darshan") { MandirDarshanPhotoScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                    composable("puja_darshan") { PujaDarshanScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                    composable("guruhari_darshan") { GuruhariDarshanScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                    
                    composable("festivals") { 
                        FestivalsScreen(
                            preferenceManager = preferenceManager, 
                            currentUserRole = currentUserRole, 
                            onBack = { navController.popBackStack() }, 
                            onNavigateToVideoPlayer = { t, u -> navController.navigate("video_player/${URLEncoder.encode(t, "UTF-8")}/${URLEncoder.encode(u, "UTF-8")}") }, 
                            onNavigateToAudioPlayer = { _ -> navController.navigate("kirtan_player") },
                            driveViewModel = driveViewModel,
                            kirtanViewModel = kirtanViewModel
                        ) 
                    }

                    composable("kirtan") { KirtanScreen({ navController.popBackStack() }, { c -> navController.navigate("kirtan_list/${URLEncoder.encode(c, "UTF-8")}") }, { navController.navigate("playlists") }, kirtanViewModel) }
                    composable("kirtan_lyrics") { KirtanLyricsSearchScreen { navController.popBackStack() } }
                    composable("playlists") { PlaylistScreen({ navController.popBackStack() }, { id, _ -> navController.navigate("kirtan_list/playlist:${id}") }) }
                    composable("kirtan_list/{category}") { b -> KirtanListScreen(URLDecoder.decode(b.arguments?.getString("category") ?: "", "UTF-8"), { navController.popBackStack() }, { k -> kirtanViewModel.playKirtan(context, k, kirtanViewModel.sharedKirtans); navController.navigate("kirtan_player") }, kirtanViewModel) }
                    composable("kirtan_player") { KirtanPlayerScreen({ navController.popBackStack() }, kirtanViewModel) }
                    
                    composable("sabha_timetable") { 
                        SabhaTimeTableScreen(
                            onBack = { navController.popBackStack() }, 
                            onSabhaClick = { s -> navController.navigate("sabha_detail/$s") },
                            preferenceManager = preferenceManager,
                            currentUserRole = currentUserRole,
                            driveViewModel = driveViewModel
                        ) 
                    }
                    
                    composable("sabha_detail/{sabhaName}") { b -> SabhaDetailScreen(currentUserRole, b.arguments?.getString("sabhaName") ?: "", { navController.popBackStack() }) }
                    composable("functions") { FunctionsScreen(preferenceManager, currentUserRole, { navController.popBackStack() }, { t, u -> navController.navigate("video_player/${URLEncoder.encode(t, "UTF-8")}/${URLEncoder.encode(u, "UTF-8")}") }) }
                    composable("video_player/{title}/{url}") { b -> VideoPlayerScreen(URLDecoder.decode(b.arguments?.getString("title") ?: "", "UTF-8"), URLDecoder.decode(b.arguments?.getString("url") ?: "", "UTF-8"), { navController.popBackStack() }) }
                    composable("satsang_news") { SatsangNewsScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                    composable("sabha_saar") { SabhaSaarScreen(onBack = { navController.popBackStack() }, driveViewModel = driveViewModel) }
                    
                    composable("settings") { 
                        SettingsScreen(
                            preferenceManager = preferenceManager, 
                            onBack = { navController.popBackStack() }, 
                            onRestartApp = { onLanguageChange(preferenceManager.getLanguage()); onDarkModeChange(preferenceManager.isDarkMode()); onBgColorChange(preferenceManager.getBackgroundColor()) }, 
                            onNavigateToHelpFeedback = { navController.navigate("help_feedback") }
                        ) 
                    }

                    composable("help_feedback") { HelpFeedbackScreen { navController.popBackStack() } }
                    composable("coming_soon/{title}") { b -> ComingSoonScreen(b.arguments?.getString("title") ?: "Coming Soon", { navController.popBackStack() }) }
                }
            }
        }
    }

    @Composable
    private fun RequestRuntimePermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }
        LaunchedEffect(Unit) { launcher.launch(permissions.toTypedArray()) }
    }
}
