package com.thruxion.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.thruxion.app.data.AppDatabase
import com.thruxion.app.data.repository.ChatRepositoryImpl
import com.thruxion.app.network.ApiRegistry
import com.thruxion.app.utils.CryptoManager

class ChatDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_PARTNER_ID = "partner_id"
        private const val ARG_PARTNER_NAME = "partner_name"

        fun newInstance(partnerId: String? = null, partnerName: String? = null): ChatDialogFragment {
            val args = Bundle().apply {
                putString(ARG_PARTNER_ID, partnerId)
                putString(ARG_PARTNER_NAME, partnerName)
            }
            return ChatDialogFragment().apply { arguments = args }
        }
    }

    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val cryptoManager = try { CryptoManager(requireContext()) } catch (e: Exception) { null }
        val repository = ChatRepositoryImpl(
            database.chatMessageDao(),
            database.contactDao(),
            ApiRegistry.communicationsApi,
            cryptoManager
        )
        ChatViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar)
        
        val partnerId = arguments?.getString(ARG_PARTNER_ID)
        val partnerName = arguments?.getString(ARG_PARTNER_NAME)
        
        if (partnerId != null)
            viewModel.navigateToDetail(partnerId, partnerName)
        else
            viewModel.navigateToList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                var isVisible by remember { mutableStateOf(false) }
                val configuration = LocalConfiguration.current
                val isTablet = configuration.screenWidthDp > 600
                
                val closeAction = {
                    isVisible = false
                    postDelayed({ dismiss() }, 300)
                }

                LaunchedEffect(Unit) {
                    isVisible = true
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(enabled = isVisible) { 
                             closeAction()
                        },
                    contentAlignment = if (isTablet) Alignment.Center else Alignment.BottomCenter
                ) {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = if (isTablet) {
                            fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                        } else {
                            slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeIn()
                        },
                        exit = if (isTablet) {
                            fadeOut() + scaleOut(targetScale = 0.9f)
                        } else {
                            slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        }
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = if (isTablet) 32.dp else 12.dp)
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                                .padding(bottom = 12.dp)
                                .then(
                                    if (isTablet) Modifier.widthIn(max = 600.dp).fillMaxHeight(0.8f)
                                    else Modifier.fillMaxWidth().heightIn(max = (configuration.screenHeightDp * 0.85).dp)
                                )
                                .clip(RoundedCornerShape(28.dp))
                                .clickable(enabled = false) { }, // Prevent clicks from going to background
                            tonalElevation = 8.dp,
                            shadowElevation = 12.dp
                        ) {
                            ChatMain(
                                viewModel = viewModel,
                                onClose = { closeAction() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            
            // 1. Enable true edge-to-edge for the Dialog window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // 2. Use ADJUST_RESIZE to let the window resize when keyboard opens
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }
}
