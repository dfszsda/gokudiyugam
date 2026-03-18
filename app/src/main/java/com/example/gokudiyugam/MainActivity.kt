@file:Suppress("DEPRECATION")

package com.example.gokudiyugam

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gokudiyugam.drive.DriveHelper
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.network.GoogleSheetsUploader
import com.example.gokudiyugam.ui.screens.*
import com.example.gokudiyugam.ui.theme.GokudiyugamTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.initialize
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*

class MainActivity : androidx.activity.ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.initialize(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance("mediadata")
        preferenceManager = PreferenceManager(this)
        credentialManager = CredentialManager.create(this)

        setContent {
            var appLanguage by remember { mutableStateOf(preferenceManager.getLanguage()) }
            var isDarkMode by remember { mutableStateOf(preferenceManager.isDarkMode()) }
            var bgColorInt by remember { mutableStateOf(preferenceManager.getBackgroundColor()) }
            
            val context = LocalContext.current
            updateResources(context, appLanguage)

            GokudiyugamTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(bgColorInt)
                ) {
                    val navController = rememberNavController()
                    var currentUserRole by remember { mutableStateOf<UserRole?>(null) }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        val user = auth.currentUser
                        if (user != null) {
                            db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                                val roleStr = doc.getString("role") ?: "NORMAL"
                                currentUserRole = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.NORMAL }
                                db.collection("users").document(user.uid).update("isOnline", true)
                            }
                        }
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
                                onRequireVerification = { _, _ -> /* Handle if needed */ },
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
                                                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                                }
                                            }
                                        } else onError(task.exception?.message ?: "Login failed")
                                    }
                                },
                                onGuestLoginClick = { onLoading, onError ->
                                    onLoading(true)
                                    auth.signInAnonymously().addOnCompleteListener { task ->
                                        onLoading(false)
                                        if (task.isSuccessful) {
                                            currentUserRole = UserRole.NORMAL
                                            preferenceManager.saveCurrentUsername("Guest")
                                            navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                        } else onError(task.exception?.message ?: "Guest login failed")
                                    }
                                },
                                onGoogleSignInClick = {
                                    // Handle Google Sign In
                                }
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
                                                val userMap = hashMapOf("uid" to user.uid, "name" to username, "email" to email, "role" to "NORMAL")
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
                                onNavigateToFunctions = { navController.navigate("coming_soon/Functions") },
                                onNavigateToSatsangNews = { navController.navigate("satsang_news") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToMediaLibrary = { navController.navigate("media_library") },
                                onProfileClick = { navController.navigate("profile") },
                                onLogout = {
                                    val user = auth.currentUser
                                    if (user != null && !user.isAnonymous) {
                                        db.collection("users").document(user.uid).update("isOnline", false)
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
                        composable("daily_darshan") { DailyDarshanScreen(onBack = { navController.popBackStack() }, onNavigateToPujaDarshan = { navController.navigate("coming_soon/Puja Darshan") }, onNavigateToMandirDarshan = { navController.navigate("mandir_darshan") }, onNavigateToGuruhariDarshan = { navController.navigate("coming_soon/Guruhari Darshan") }, onNavigateToFestivals = { navController.navigate("festivals") }) }
                        composable("mandir_darshan") { MandirDarshanPhotoScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                        composable("puja_darshan") { PujaDarshanScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                        composable("guruhari_darshan") { GuruhariDarshanScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                        composable("festivals") { FestivalsScreen(preferenceManager, currentUserRole, { navController.popBackStack() }, { t, u -> navController.navigate("video_player/${URLEncoder.encode(t, "UTF-8")}/${URLEncoder.encode(u, "UTF-8")}") }) }
                        @Suppress("DEPRECATION")
                        composable("kirtan") { KirtanScreen(onBack = { navController.popBackStack() }, onNavigateToPlayer = { c -> navController.navigate("kirtan_player/${URLEncoder.encode(c, "UTF-8")}") }) }
                        composable("kirtan_player/{category}") { backStackEntry -> val category = backStackEntry.arguments?.getString("category") ?: ""; KirtanPlayerScreen(category = URLDecoder.decode(category, "UTF-8"), onBack = { navController.popBackStack() }) }
                        composable("sabha_timetable") { SabhaTimeTableScreen(preferenceManager = preferenceManager, currentUserRole = currentUserRole, onBack = { navController.popBackStack() }, onSabhaClick = { s -> navController.navigate("sabha_detail/$s") }) }
                        composable("sabha_detail/{sabhaName}") { backStackEntry -> val sabhaName = backStackEntry.arguments?.getString("sabhaName") ?: ""; SabhaDetailScreen(currentUserRole = currentUserRole, sabhaName = sabhaName, onBack = { navController.popBackStack() }) }
                        composable("functions") { FunctionsScreen(preferenceManager, currentUserRole, { navController.popBackStack() }, { t, u -> navController.navigate("video_player/${URLEncoder.encode(t, "UTF-8")}/${URLEncoder.encode(u, "UTF-8")}") }) }
                        composable("video_player/{title}/{url}") { backStackEntry -> val title = backStackEntry.arguments?.getString("title") ?: ""; val url = backStackEntry.arguments?.getString("url") ?: ""; VideoPlayerScreen(title = URLDecoder.decode(title, "UTF-8"), url = URLDecoder.decode(url, "UTF-8"), onBack = { navController.popBackStack() }) }
                        composable("satsang_news") { SatsangNewsScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                        composable("settings") { SettingsScreen(preferenceManager, { navController.popBackStack() }, { appLanguage = preferenceManager.getLanguage(); isDarkMode = preferenceManager.isDarkMode(); bgColorInt = preferenceManager.getBackgroundColor(); updateResources(context, appLanguage) }) }
                        composable("bridge_to_darshan") { ComingSoonScreen(title = "Sabhasar", onBack = { navController.popBackStack() }) }
                        composable("coming_soon/{title}") { backStackEntry -> val title = backStackEntry.arguments?.getString("title") ?: "Coming Soon"; ComingSoonScreen(title = title, onBack = { navController.popBackStack() }) }
                    }
                }
            }
        }
    }

    private fun updateResources(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
