@file:Suppress("DEPRECATION")

package com.example.gokudiyugam

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.ui.screens.*
import com.example.gokudiyugam.ui.theme.GokudiyugamTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferenceManager = remember { PreferenceManager(this) }
            var appLanguage by remember { mutableStateOf(preferenceManager.getLanguage()) }
            var isDarkMode by remember { mutableStateOf(preferenceManager.isDarkMode()) }
            var bgColorInt by remember { mutableIntStateOf(preferenceManager.getBackgroundColor()) }
            
            updateResources(this, appLanguage)

            GokudiyugamTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (bgColorInt == -1) MaterialTheme.colorScheme.background else androidx.compose.ui.graphics.Color(bgColorInt)
                ) {
                    val navController = rememberNavController()
                    val auth = FirebaseAuth.getInstance()
                    val db = FirebaseFirestore.getInstance("mediadata")
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current
                    val credentialManager = CredentialManager.create(context)
                    val lifecycleOwner = LocalLifecycleOwner.current

                    var currentUserRole by remember { mutableStateOf<UserRole?>(null) }

                    // Sync User Role on App Start
                    LaunchedEffect(auth.currentUser) {
                        auth.currentUser?.let { user ->
                            db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    val roleStr = doc.getString("role") ?: "NORMAL"
                                    currentUserRole = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.NORMAL }
                                }
                            }
                        }
                    }

                    // Online/Offline Status Management
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            val user = auth.currentUser
                            if (user != null && !user.isAnonymous) {
                                when (event) {
                                    Lifecycle.Event.ON_RESUME -> {
                                        db.collection("users").document(user.uid).update("isOnline", true, "lastSeen", System.currentTimeMillis())
                                    }
                                    Lifecycle.Event.ON_PAUSE -> {
                                        db.collection("users").document(user.uid).update("isOnline", false, "lastSeen", System.currentTimeMillis())
                                    }
                                    else -> {}
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = if (auth.currentUser != null) "home" else "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                preferenceManager = preferenceManager,
                                onLoginSuccess = { username, role ->
                                    currentUserRole = role
                                    preferenceManager.saveCurrentUsername(username)
                                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                },
                                onRequireVerification = { _, _ -> },
                                onSignUpClick = { navController.navigate("signup") },
                                onEmailSignInClick = { email, password, onLoading, onError ->
                                    onLoading(true)
                                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                        onLoading(false)
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            if (user != null) {
                                                db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                                                    val username = doc.getString("name") ?: "User"
                                                    val roleStr = doc.getString("role") ?: "NORMAL"
                                                    val role = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.NORMAL }
                                                    currentUserRole = role
                                                    preferenceManager.saveCurrentUsername(username)
                                                    
                                                    val loginSync = hashMapOf(
                                                        "lastLoginDevice" to "${Build.MANUFACTURER} ${Build.MODEL}",
                                                        "lastSeen" to System.currentTimeMillis(),
                                                        "isOnline" to true
                                                    )
                                                    db.collection("users").document(user.uid).set(loginSync, SetOptions.merge())
                                                    
                                                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                                }
                                            }
                                        } else onError(task.exception?.message ?: "Login failed")
                                    }
                                },
                                onGuestLoginClick = { onLoading, onError ->
                                    onLoading(true)
                                    val savedGuestUid = preferenceManager.getGuestUid()
                                    auth.signInAnonymously().addOnCompleteListener { task ->
                                        onLoading(false)
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            if (user != null) {
                                                if (savedGuestUid.isEmpty()) preferenceManager.saveGuestUid(user.uid)
                                                currentUserRole = UserRole.GUEST
                                                preferenceManager.saveCurrentUsername("Guest")
                                                navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                            }
                                        } else onError(task.exception?.message ?: "Guest login failed")
                                    }
                                },
                                onGoogleSignInClick = {}
                            )
                        }

                        composable("signup") {
                            SignUpScreen(
                                preferenceManager = preferenceManager,
                                onSignUpSuccess = { username, role ->
                                    currentUserRole = role
                                    preferenceManager.saveCurrentUsername(username)
                                    navController.navigate("home") { popUpTo("signup") { inclusive = true } }
                                },
                                onLoginClick = { navController.navigate("login") },
                                onEmailSignUpClick = { username, email, password, onLoading, onError ->
                                    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                        onLoading(false)
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            if (user != null) {
                                                val userMap = hashMapOf(
                                                    "uid" to user.uid, 
                                                    "name" to username, 
                                                    "email" to email, 
                                                    "role" to "NORMAL",
                                                    "lastLoginDevice" to "${Build.MANUFACTURER} ${Build.MODEL}",
                                                    "lastSeen" to System.currentTimeMillis(),
                                                    "isOnline" to true
                                                )
                                                db.collection("users").document(user.uid).set(userMap)
                                                currentUserRole = UserRole.NORMAL
                                                navController.navigate("home") { popUpTo("signup") { inclusive = true } }
                                            }
                                        } else onError(task.exception?.message ?: "Signup failed")
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                currentUserRole = currentUserRole,
                                onNavigateToDailyDarshan = { navController.navigate("daily_darshan") },
                                onNavigateToKirtan = { navController.navigate("kirtan") },
                                onNavigateToSabhaTimeTable = { navController.navigate("sabha_timetable") },
                                onNavigateToFunctions = { navController.navigate("functions") },
                                onNavigateToSatsangNews = { navController.navigate("coming_soon/Satsang News") },
                                onNavigateToSabhaSaar = { navController.navigate("sabha_saar") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToMediaLibrary = { navController.navigate("media_library") },
                                onProfileClick = { navController.navigate("profile") },
                                onLogout = {
                                    val user = auth.currentUser
                                    if (user != null && !user.isAnonymous) {
                                        db.collection("users").document(user.uid).update("isOnline", false, "lastSeen", System.currentTimeMillis())
                                    }
                                    currentUserRole = null
                                    preferenceManager.saveCurrentUsername("")
                                    auth.signOut()
                                    scope.launch { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                },
                                onNavigateToGoogleDrive = { navController.navigate("google_drive") },
                                onNavigateToAdminPanel = { navController.navigate("admin_panel") }
                            )
                        }

                        composable("admin_panel") {
                            AdminPanelScreen(
                                currentUserRole = currentUserRole,
                                preferenceManager = preferenceManager,
                                onNavigateToMediaLibrary = { navController.navigate("media_library") },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("profile") { ProfileScreen(onBack = { navController.popBackStack() }, onNavigateToEditProfile = { navController.navigate("settings") }, onNavigateToChangePassword = { navController.navigate("settings") }) }
                        composable("media_library") { MediaDataScreen(currentUserRole = currentUserRole, onBack = { navController.popBackStack() }) }
                        composable("google_drive") { DriveScreen(currentUserRole = currentUserRole, onBack = { navController.popBackStack() }) }
                        composable("daily_darshan") { DailyDarshanScreen(onBack = { navController.popBackStack() }, onNavigateToPujaDarshan = { navController.navigate("puja_darshan") }, onNavigateToMandirDarshan = { navController.navigate("mandir_darshan") }, onNavigateToGuruhariDarshan = { navController.navigate("guruhari_darshan") }, onNavigateToFestivals = { navController.navigate("festivals") }) }
                        composable("mandir_darshan") { MandirDarshanPhotoScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                        
                        composable("puja_darshan") { 
                            PujaDarshanScreen(
                                preferenceManager = preferenceManager, 
                                currentUserRole = currentUserRole, 
                                onBack = { navController.popBackStack() }
                            ) 
                        }
                        
                        composable("guruhari_darshan") { 
                            GuruhariDarshanScreen(
                                preferenceManager = preferenceManager, 
                                currentUserRole = currentUserRole, 
                                onBack = { navController.popBackStack() }
                            ) 
                        }

                        composable("festivals") { 
                            FestivalsScreen(
                                preferenceManager = preferenceManager, 
                                currentUserRole = currentUserRole, 
                                onBack = { navController.popBackStack() },
                                onNavigateToVideoPlayer = { t, u -> navController.navigate("video_player/${URLEncoder.encode(t, "UTF-8")}/${URLEncoder.encode(u, "UTF-8")}") },
                                onNavigateToAudioPlayer = { c -> navController.navigate("kirtan_player/${URLEncoder.encode(c, "UTF-8")}") }
                            ) 
                        }
                        
                        composable("kirtan") { KirtanScreen(onBack = { navController.popBackStack() }, onNavigateToPlayer = { c -> navController.navigate("kirtan_player/${URLEncoder.encode(c, "UTF-8")}") }) }
                        composable("kirtan_player/{category}") { backStackEntry -> val category = backStackEntry.arguments?.getString("category") ?: ""; KirtanPlayerScreen(category = URLDecoder.decode(category, "UTF-8"), onBack = { navController.popBackStack() }) }
                        composable("sabha_timetable") { SabhaTimeTableScreen(preferenceManager = preferenceManager, currentUserRole = currentUserRole, onBack = { navController.popBackStack() }, onSabhaClick = { s -> navController.navigate("sabha_detail/$s") }) }
                        composable("sabha_detail/{sabhaName}") { backStackEntry -> val sabhaName = backStackEntry.arguments?.getString("sabhaName") ?: ""; SabhaDetailScreen(currentUserRole = currentUserRole, sabhaName = sabhaName, onBack = { navController.popBackStack() }) }
                        composable("functions") { FunctionsScreen(preferenceManager, currentUserRole, { navController.popBackStack() }, { t, u -> navController.navigate("video_player/${URLEncoder.encode(t, "UTF-8")}/${URLEncoder.encode(u, "UTF-8")}") }) }
                        composable("video_player/{title}/{url}") { backStackEntry -> val title = backStackEntry.arguments?.getString("title") ?: ""; val url = backStackEntry.arguments?.getString("url") ?: ""; VideoPlayerScreen(title = URLDecoder.decode(title, "UTF-8"), url = URLDecoder.decode(url, "UTF-8"), onBack = { navController.popBackStack() }) }
                        composable("satsang_news") { SatsangNewsScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                        composable("sabha_saar") { SabhaSaarScreen(onBack = { navController.popBackStack() }) }
                        composable("settings") { SettingsScreen(preferenceManager, onBack = { navController.popBackStack() }, onRestartApp = { appLanguage = preferenceManager.getLanguage(); isDarkMode = preferenceManager.isDarkMode(); bgColorInt = preferenceManager.getBackgroundColor(); updateResources(context, appLanguage) }, onNavigateToHelpFeedback = { navController.navigate("help_feedback") }) }
                        composable("help_feedback") { HelpFeedbackScreen(onBack = { navController.popBackStack() }) }
                        composable("coming_soon/{title}") { backStackEntry -> val title = backStackEntry.arguments?.getString("title") ?: "Coming Soon"; ComingSoonScreen(title = title, onBack = { navController.popBackStack() }) }
                    }
                }
            }
        }
    }

    private fun updateResources(context: Context, language: String) {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
