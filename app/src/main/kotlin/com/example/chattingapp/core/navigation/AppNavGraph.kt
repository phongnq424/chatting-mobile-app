package com.example.chattingapp.core.navigation

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.chattingapp.data.repository.AuthRepository
import com.example.chattingapp.data.repository.ConversationRepository
import com.example.chattingapp.data.repository.MessageRepository
import com.example.chattingapp.data.repository.UserRepository
import com.example.chattingapp.domain.usecase.DeleteMessageUseCase
import com.example.chattingapp.domain.usecase.MarkAsReadUseCase
import com.example.chattingapp.domain.usecase.ObserveConversationsUseCase
import com.example.chattingapp.domain.usecase.ObserveMessagesUseCase
import com.example.chattingapp.domain.usecase.SendMessageUseCase
import com.example.chattingapp.ui.screens.chat.ChatDetailScreen
import com.example.chattingapp.ui.screens.conversation.ConversationListScreen
import com.example.chattingapp.ui.screens.login.LoginScreen
import com.example.chattingapp.ui.screens.profile.ProfileScreen
import com.example.chattingapp.ui.screens.register.RegisterScreen
import com.example.chattingapp.ui.screens.search.UserSearchScreen
import com.example.chattingapp.viewmodel.AuthViewModel
import com.example.chattingapp.viewmodel.ChatDetailViewModel
import com.example.chattingapp.viewmodel.ConversationListViewModel
import com.example.chattingapp.viewmodel.UserSearchViewModel

object NavRoutes {
    const val Login = "login"
    const val Register = "register"
    const val Conversations = "conversations"
    const val Search = "search"
    const val Profile = "profile"
    const val ChatDetail = "chat/{conversationId}"

    fun chatDetail(conversationId: String): String {
        return "chat/$conversationId"
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    authRepository: AuthRepository,
    authViewModel: AuthViewModel,
    userRepository: UserRepository,
    conversationRepository: ConversationRepository,
    messageRepository: MessageRepository
) {
    val observeConversationsUseCase = ObserveConversationsUseCase(conversationRepository)
    val observeMessagesUseCase = ObserveMessagesUseCase(messageRepository)
    val sendMessageUseCase = SendMessageUseCase(messageRepository)
    val markAsReadUseCase = MarkAsReadUseCase(conversationRepository)
    val deleteMessageUseCase = DeleteMessageUseCase(messageRepository)

    val lastNavigationTime = remember { mutableLongStateOf(0L) }

    fun canNavigate(intervalMillis: Long = 700L): Boolean {
        val now = SystemClock.elapsedRealtime()

        return if (now - lastNavigationTime.longValue >= intervalMillis) {
            lastNavigationTime.longValue = now
            true
        } else {
            false
        }
    }

    fun safeNavigate(
        route: String,
        intervalMillis: Long = 700L,
        builder: NavOptionsBuilder.() -> Unit = {}
    ) {
        if (!canNavigate(intervalMillis)) return

        navController.navigate(route) {
            launchSingleTop = true
            builder()
        }
    }

    fun safePopBackStack(intervalMillis: Long = 700L) {
        if (!canNavigate(intervalMillis)) return
        navController.popBackStack()
    }

    fun safeNavigateAndClear(route: String) {
        if (!canNavigate()) return

        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    fun forceNavigateAndClear(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.Login) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    forceNavigateAndClear(NavRoutes.Conversations)
                },
                onNavigateToRegister = {
                    safeNavigate(NavRoutes.Register)
                }
            )
        }

        composable(NavRoutes.Register) {
            BackHandler {
                safePopBackStack()
            }

            RegisterScreen(
                viewModel = authViewModel,
                onBack = {
                    safePopBackStack()
                },
                onRegisterSuccess = {
                    authViewModel.resetLoginSuccess()
                    forceNavigateAndClear(NavRoutes.Conversations)
                }
            )
        }

        composable(NavRoutes.Conversations) {
            val currentUserId = authRepository.getCurrentUser()?.uid

            if (currentUserId == null) {
                forceNavigateAndClear(NavRoutes.Login)
                return@composable
            }

            val conversationListViewModel: ConversationListViewModel = viewModel(
                key = "conversation_list_$currentUserId",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ConversationListViewModel(
                            currentUserId = currentUserId,
                            observeConversationsUseCase = observeConversationsUseCase,
                            userRepository = userRepository
                        ) as T
                    }
                }
            )

            ConversationListScreen(
                viewModel = conversationListViewModel,
                onOpenConversation = { conversationId ->
                    safeNavigate(NavRoutes.chatDetail(conversationId))
                },
                onCreateConversation = {
                    safeNavigate(NavRoutes.Search)
                },
                onNavigateToProfile = {
                    safeNavigate(NavRoutes.Profile)
                },
                onLogout = {
                    if (!canNavigate(intervalMillis = 1000L)) return@ConversationListScreen

                    conversationListViewModel.stopObservingConversations()

                    authViewModel.logout {
                        forceNavigateAndClear(NavRoutes.Login)
                    }
                }
            )
        }

        composable(NavRoutes.Search) {
            BackHandler {
                safePopBackStack()
            }

            val userSearchViewModel: UserSearchViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return UserSearchViewModel(
                            authRepository = authRepository,
                            userRepository = userRepository,
                            conversationRepository = conversationRepository
                        ) as T
                    }
                }
            )

            UserSearchScreen(
                viewModel = userSearchViewModel,
                onBack = {
                    safePopBackStack()
                },
                onConversationCreated = { conversationId ->
                    safeNavigate(
                        route = NavRoutes.chatDetail(conversationId),
                        intervalMillis = 1000L
                    ) {
                        popUpTo(NavRoutes.Search) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.ChatDetail,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            BackHandler {
                safePopBackStack()
            }

            val conversationId = backStackEntry.arguments
                ?.getString("conversationId")
                .orEmpty()

            val chatDetailViewModel: ChatDetailViewModel = viewModel(
                key = "chat_detail_$conversationId",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ChatDetailViewModel(
                            conversationId = conversationId,
                            authRepository = authRepository,
                            observeMessagesUseCase = observeMessagesUseCase,
                            sendMessageUseCase = sendMessageUseCase,
                            markAsReadUseCase = markAsReadUseCase,
                            deleteMessageUseCase = deleteMessageUseCase
                        ) as T
                    }
                }
            )

            ChatDetailScreen(
                viewModel = chatDetailViewModel,
                onBack = {
                    safePopBackStack()
                }
            )
        }

        composable(NavRoutes.Profile) {
            BackHandler {
                safePopBackStack()
            }

            ProfileScreen(
                authViewModel = authViewModel,
                onBack = {
                    safePopBackStack()
                }
            )
        }
    }
}