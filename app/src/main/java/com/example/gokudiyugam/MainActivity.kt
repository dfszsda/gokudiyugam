@file:Suppress("DEPRECATION")

package com.example.gokudiyugam

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.ui.screens.*
import com.example.gokudiyugam.ui.theme.GokudiyugamTheme
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.initialize
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

class MainActivity : androidx.fragment.app.FragmentActivity() {
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
                        val currentUsernamePref = preferenceManager.getCurrentUsername() ?: ""
                        
                        var currentUserRole by remember { 
                            mutableStateOf<UserRole?>(
                                if (currentUsernamePref.isNotEmpty()) preferenceManager.getUserRoleForAccount(currentUsernamePref) else null
                            ) 
                        }

                        LaunchedEffect(auth.currentUser) {
                            val user = auth.currentUser
                            if (user != null) {
                                val email = user.email ?: ""
                                val role = if (email == ADMIN_EMAIL) UserRole.HOST else UserRole.NORMAL
                                currentUserRole = role
                                
                                val name = user.displayName ?: email.substringBefore("@")
                                preferenceManager.saveCurrentUsername(name)
                                preferenceManager.saveUserRoleForAccount(name, role)
                                
                                val userMap = mapOf(
                                    "uid" to user.uid,
                                    "name" to name,
                                    "email" to email,
                                    "role" to role.name,
                                    "lastActive" to System.currentTimeMillis()
                                )
                                db.collection("users").document(user.uid).set(userMap, SetOptions.merge())
                            }
                        }

                        val startDestination = if (auth.currentUser != null) "home" else "login"

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
                                    onNavigateToSabhaTimeTable = { navController.navigate("bridge_to_darshan") }, 
                                    onNavigateToFunctions = { navController.navigate("coming_soon/Functions") },
                                    onNavigateToSatsangNews = { navController.navigate("satsang_news") },
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onNavigateToAdminPanel = { navController.navigate("admin_panel") },
                                    onNavigateToMediaLibrary = { navController.navigate("media_library") },
                                    onProfileClick = { navController.navigate("profile") },
                                    onLogout = {
                                        currentUserRole = null
                                        preferenceManager.saveCurrentUsername("")
                                        auth.signOut()
                                        scope.launch { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                    },
                                    onNavigateToGoogleDrive = { navController.navigate("google_drive") }
                                )
                            }

                            composable("profile") {
                                ProfileScreen(
                                    onBack = { navController.popBackStack() },
                                    onNavigateToEditProfile = { navController.navigate("settings") },
                                    onNavigateToChangePassword = { 
                                        // Since we don't have a separate change password screen yet, 
                                        // we can either redirect to settings or show a message.
                                        // For now, let's assume settings has it or we will add it.
                                        navController.navigate("settings")
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
                                    onNavigateToPujaDarshan = { navController.navigate("coming_soon/Puja Darshan") },
                                    onNavigateToMandirDarshan = { navController.navigate("mandir_darshan") },
                                    onNavigateToGuruhariDarshan = { navController.navigate("coming_soon/Guruhari Darshan") },
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
                                    currentUserRole = currentUserRole,
                                    preferenceManager = preferenceManager, 
                                    onNavigateToMediaLibrary = { navController.navigate("media_library") }, 
                                    onBack = { navController.popBackStack() }
                                ) 
                            }
                            
                            composable("bridge_to_darshan") {
                                ComingSoonScreen(title = "Sabhasar", onBack = { navController.popBackStack() })
                            }

                            composable("coming_soon/{title}") { backStackEntry ->
                                val title = backStackEntry.arguments?.getString("title") ?: "Coming Soon"
                                ComingSoonScreen(title = title, onBack = { navController.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleEmailSignIn(email: String, password: String, pm: PreferenceManager, onSuccess: (String, UserRole) -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    val role = if (email == ADMIN_EMAIL) UserRole.HOST else UserRole.NORMAL
                    val name = user.displayName ?: email.substringBefore("@")
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
                    val role = if (email == ADMIN_EMAIL) UserRole.HOST else UserRole.NORMAL
                    pm.saveUserCredentials(username, pass, role, email)
                    onSuccess(username, role)
                } else {
                    onError("User creation failed")
                }
            } else onError(task.exception?.message ?: "Signup failed")
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
            onEmailSignInClick = { _, _, _, _ -> }
        )
    }
}
