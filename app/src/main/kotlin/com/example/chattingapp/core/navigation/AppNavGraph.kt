package com.example.chattingapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.chattingapp.data.repository.AuthRepository
import com.example.chattingapp.data.repository.ConversationRepository
import com.example.chattingapp.data.repository.MessageRepository
import com.example.chattingapp.data.repository.UserRepository
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

    fun navigateAndClear(route: String) {
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
                    navigateAndClear(NavRoutes.Conversations)
                },
                onNavigateToRegister = {
                    navController.navigate(NavRoutes.Register)
                }
            )
        }

        composable(NavRoutes.Register) {
            RegisterScreen(
                viewModel = authViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    authViewModel.resetLoginSuccess()
                    navigateAndClear(NavRoutes.Conversations)
                }
            )
        }

        composable(NavRoutes.Conversations) {
            val currentUserId = authRepository.getCurrentUser()?.uid

            if (currentUserId == null) {
                navController.navigate(NavRoutes.Login) {
                    popUpTo(NavRoutes.Conversations) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
                return@composable
            }

            val conversationListViewModel: ConversationListViewModel = viewModel(
                key = "conversation_list_$currentUserId",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ConversationListViewModel(
                            currentUserId = currentUserId,
                            observeConversationsUseCase = observeConversationsUseCase
                        ) as T
                    }
                }
            )

            ConversationListScreen(
                viewModel = conversationListViewModel,
                onOpenConversation = { conversationId ->
                    navController.navigate(NavRoutes.chatDetail(conversationId))
                },
                onCreateConversation = {
                    navController.navigate(NavRoutes.Search)
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.Profile)
                },
                onLogout = {
                    conversationListViewModel.stopObservingConversations()

                    authViewModel.logout {
                        navigateAndClear(NavRoutes.Login)
                    }
                }
            )
        }

        composable(NavRoutes.Search) {
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
                    navController.popBackStack()
                },
                onConversationCreated = { conversationId ->
                    navController.navigate(NavRoutes.chatDetail(conversationId)) {
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
                            markAsReadUseCase = markAsReadUseCase
                        ) as T
                    }
                }
            )

            ChatDetailScreen(
                viewModel = chatDetailViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.Profile) {
            ProfileScreen(
                authViewModel = authViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}