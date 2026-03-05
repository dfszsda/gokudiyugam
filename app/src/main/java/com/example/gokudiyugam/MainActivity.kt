@file:Suppress("DEPRECATION")

package com.example.gokudiyugam

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gokudiyugam.model.User
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.ui.screens.*
import com.example.gokudiyugam.ui.theme.GokudiyugamTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.initialize
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private val WEB_CLIENT_ID = "728177722635-u2gatdgb305ga6gbt6viabml4d8hv3gc.apps.googleusercontent.com"
    private val ADMIN_EMAIL = "bssbadalpur@gmail.com"
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Firebase.initialize(context = this)
        
        if (com.example.gokudiyugam.BuildConfig.DEBUG) {
            Firebase.appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance(),
            )
        } else {
            Firebase.appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance(),
            )
        }
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Ensure Firestore works well with offline/cache
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings

        setContent {
            val context = LocalContext.current
            val preferenceManager = remember { PreferenceManager(context) }
            val scope = rememberCoroutineScope()
            val credentialManager = remember { CredentialManager.create(context) }
            
            var appLanguage by remember { mutableStateOf(preferenceManager.getLanguage()) }
            var isDarkMode by remember { mutableStateOf(preferenceManager.isDarkMode()) }
            var bgColorInt by remember { mutableStateOf(preferenceManager.getBackgroundColor()) }

            val locale = Locale(appLanguage)
            val configuration = LocalConfiguration.current
            configuration.setLocale(locale)
            
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                val customBgColor = if (bgColorInt == -1) null else Color(bgColorInt)

                GokudiyugamTheme(
                    darkTheme = isDarkMode,
                    customBackgroundColor = customBgColor
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val currentUsername = preferenceManager.getCurrentUsername() ?: ""
                        var currentUserRole by remember { 
                            mutableStateOf<UserRole?>(
                                if (currentUsername.isNotEmpty()) preferenceManager.getUserRoleForAccount(currentUsername) else null
                            ) 
                        }

                        // Updated: Removed automatic profile saving to database.
                        // Roles are now handled locally or via hardcoded Admin check.
                        LaunchedEffect(auth.currentUser) {
                            val user = auth.currentUser
                            if (user != null) {
                                val role = if (user.email == ADMIN_EMAIL) UserRole.HOST else UserRole.NORMAL
                                currentUserRole = role
                                val name = user.displayName ?: user.email?.substringBefore("@") ?: "User"
                                preferenceManager.saveUserRoleForAccount(name, role)
                            }
                        }

                        val startDestination = if (auth.currentUser != null && currentUsername.isNotEmpty()) "home" else "login"

                        NavHost(
                            navController = navController,
                            startDestination = startDestination
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
                                    onGoogleSignInClick = {
                                        scope.launch {
                                            handleGoogleSignIn(context, credentialManager, preferenceManager, { u, r ->
                                                currentUserRole = r
                                                preferenceManager.saveCurrentUsername(u)
                                                navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                            }, { err -> 
                                                Log.e("MainActivity", "Google Error: $err")
                                                Toast.makeText(context, "Google Login Failed.", Toast.LENGTH_LONG).show()
                                            })
                                        }
                                    },
                                    onEmailSignInClick = { email, password, onLoading, onError ->
                                        handleEmailSignIn(email, password, preferenceManager, { u, r ->
                                            onLoading(false)
                                            currentUserRole = r
                                            preferenceManager.saveCurrentUsername(u)
                                            navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                        }, { err -> 
                                            onLoading(false)
                                            onError(err)
                                            Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show() 
                                        })
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
                                    onGoogleSignInClick = {
                                        scope.launch {
                                            handleGoogleSignIn(context, credentialManager, preferenceManager, { u, r ->
                                                currentUserRole = r
                                                preferenceManager.saveCurrentUsername(u)
                                                navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                            }, { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() })
                                        }
                                    },
                                    onEmailSignUpClick = { username, email, password, onLoading, onError ->
                                        handleEmailSignUp(username, email, password, preferenceManager, { u, r ->
                                            onLoading(false)
                                            currentUserRole = r
                                            preferenceManager.saveCurrentUsername(u)
                                            navController.navigate("home") { popUpTo("signup") { inclusive = true } }
                                        }, { err -> 
                                            onLoading(false)
                                            onError(err)
                                            Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show() 
                                        })
                                    }
                                )
                            }

                            composable("home") {
                                HomeScreen(
                                    currentUserRole = currentUserRole,
                                    onNavigateToDailyDarshan = { navController.navigate("daily_darshan") },
                                    onNavigateToKirtan = { navController.navigate("kirtan") },
                                    onNavigateToSabhaTimeTable = { navController.navigate("bridge_to_darshan") }, // Just a temporary check
                                    onNavigateToFunctions = { navController.navigate("functions") },
                                    onNavigateToSatsangNews = { navController.navigate("satsang_news") },
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onNavigateToAdminPanel = { navController.navigate("admin_panel") },
                                    onNavigateToMediaLibrary = { navController.navigate("media_library") },
                                    onNavigateToGoogleDrive = { navController.navigate("google_drive") },
                                    onProfileClick = { },
                                    onLogout = {
                                        currentUserRole = null
                                        preferenceManager.saveCurrentUsername("")
                                        auth.signOut()
                                        scope.launch { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                    }
                                )
                            }

                            composable("media_library") {
                                MediaDataScreen(currentUserRole = currentUserRole, onBack = { navController.popBackStack() })
                            }

                            composable("google_drive") {
                                DriveScreen(currentUserRole = currentUserRole, onBack = { navController.popBackStack() })
                            }

                            composable("daily_darshan") {
                                DailyDarshanScreen(
                                    onBack = { navController.popBackStack() },
                                    onNavigateToPujaDarshan = { navController.navigate("puja_darshan") },
                                    onNavigateToMandirDarshan = { navController.navigate("mandir_darshan") },
                                    onNavigateToGuruhariDarshan = { navController.navigate("guruhari_darshan") },
                                    onNavigateToFestivals = { navController.navigate("festivals") }
                                )
                            }
                            
                            composable("mandir_darshan") { MandirDarshanPhotoScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                            composable("puja_darshan") { PujaDarshanScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                            composable("guruhari_darshan") { GuruhariDarshanScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }

                            composable("festivals") {
                                FestivalsScreen(preferenceManager, currentUserRole, { navController.popBackStack() }, { t, u ->
                                    navController.navigate("video_player/${URLEncoder.encode(t, "UTF-8")}/${URLEncoder.encode(u, "UTF-8")}")
                                })
                            }

                            @Suppress("DEPRECATION")
                            composable("kirtan") {
                                KirtanScreen(onBack = { navController.popBackStack() }, onNavigateToPlayer = { c ->
                                    navController.navigate("kirtan_player/${URLEncoder.encode(c, "UTF-8")}")
                                })
                            }

                            composable("kirtan_player/{category}") { backStackEntry ->
                                val category = backStackEntry.arguments?.getString("category") ?: ""
                                KirtanPlayerScreen(category = URLDecoder.decode(category, "UTF-8"), onBack = { navController.popBackStack() })
                            }

                            composable("sabha_timetable") {
                                SabhaTimeTableScreen(onBack = { navController.popBackStack() }, onSabhaClick = { s ->
                                    navController.navigate("sabha_detail/$s")
                                })
                            }

                            composable("sabha_detail/{sabhaName}") { backStackEntry ->
                                val sabhaName = backStackEntry.arguments?.getString("sabhaName") ?: ""
                                SabhaDetailScreen(currentUserRole = currentUserRole, sabhaName = sabhaName, onBack = { navController.popBackStack() })
                            }

                            composable("functions") {
                                FunctionsScreen(preferenceManager, currentUserRole, { navController.popBackStack() }, { t, u ->
                                    navController.navigate("video_player/${URLEncoder.encode(t, "UTF-8")}/${URLEncoder.encode(u, "UTF-8")}")
                                })
                            }

                            composable("video_player/{title}/{url}") { backStackEntry ->
                                val title = backStackEntry.arguments?.getString("title") ?: ""
                                val url = backStackEntry.arguments?.getString("url") ?: ""
                                VideoPlayerScreen(title = URLDecoder.decode(title, "UTF-8"), url = URLDecoder.decode(url, "UTF-8"), onBack = { navController.popBackStack() })
                            }

                            composable("satsang_news") { SatsangNewsScreen(preferenceManager, currentUserRole, { navController.popBackStack() }) }
                            composable("settings") { SettingsScreen(preferenceManager, { navController.popBackStack() }, {
                                appLanguage = preferenceManager.getLanguage()
                                isDarkMode = preferenceManager.isDarkMode()
                                bgColorInt = preferenceManager.getBackgroundColor()
                                updateResources(context, appLanguage)
                            }) }
                            composable("admin_panel") { 
                                AdminPanelScreen(
                                    preferenceManager = preferenceManager, 
                                    onNavigateToMediaLibrary = { navController.navigate("media_library") }, 
                                    onBack = { navController.popBackStack() }
                                ) 
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun handleEmailSignIn(email: String, password: String, pm: PreferenceManager, onSuccess: (String, UserRole) -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    // Removed Firestore profile fetching. Role determined by admin email check.
                    val role = if (email == ADMIN_EMAIL) UserRole.HOST else UserRole.NORMAL
                    val name = email.substringBefore("@")
                    pm.saveUserCredentials(name, password, role, email)
                    onSuccess(name, role)
                } else {
                    onError("User not found after sign in")
                }
            } else onError(task.exception?.message ?: "Login failed")
        }
    }

    private fun handleEmailSignUp(username: String, email: String, pass: String, pm: PreferenceManager, onSuccess: (String, UserRole) -> Unit, onError: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    // Removed Firestore document creation.
                    val role = if (email == ADMIN_EMAIL) UserRole.HOST else UserRole.NORMAL
                    pm.saveUserCredentials(username, pass, role, email)
                    onSuccess(username, role)
                } else {
                    onError("User creation failed")
                }
            } else onError(task.exception?.message ?: "Signup failed")
        }
    }

    private suspend fun handleGoogleSignIn(context: Context, cm: CredentialManager, pm: PreferenceManager, onSuccess: (String, UserRole) -> Unit, onError: (String) -> Unit) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        try {
            val result = cm.getCredential(context, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Removed Firestore profile fetching/saving.
                        val role = if (user.email == ADMIN_EMAIL) UserRole.HOST else UserRole.NORMAL
                        val name = user.displayName ?: user.email?.substringBefore("@") ?: "User"
                        pm.saveUserCredentials(name, "", role, user.email ?: "")
                        onSuccess(name, role)
                    } else {
                        onError("User not found after Google sign in")
                    }
                } else {
                    onError(task.exception?.message ?: "Google sign in failed")
                }
            }
        } catch (e: Exception) { 
            Log.e("MainActivity", "Google Error: ${e.message}")
            onError(e.message ?: "Google sign in failed") 
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

// --- PREVIEWS ---

@Preview(showBackground = true, name = "Login Screen")
@Composable
fun PreviewLogin() {
    val context = LocalContext.current
    GokudiyugamTheme {
        LoginScreen(
            preferenceManager = PreferenceManager(context),
            onLoginSuccess = { _, _ -> },
            onRequireVerification = { _, _ -> },
            onSignUpClick = {},
            onGoogleSignInClick = {},
            onEmailSignInClick = { _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Home Screen")
@Composable
fun PreviewHome() {
    GokudiyugamTheme {
        HomeScreen(
            currentUserRole = UserRole.HOST,
            onNavigateToDailyDarshan = {},
            onNavigateToKirtan = {},
            onNavigateToSabhaTimeTable = {},
            onNavigateToFunctions = {},
            onNavigateToSatsangNews = {},
            onNavigateToSettings = {},
            onNavigateToAdminPanel = {},
            onNavigateToMediaLibrary = {},
            onProfileClick = {},
            onLogout = {},
            onNavigateToGoogleDrive = {}
        )
    }
}
