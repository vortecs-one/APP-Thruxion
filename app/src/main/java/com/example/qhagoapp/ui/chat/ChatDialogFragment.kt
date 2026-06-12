package com.example.qhagoapp.ui.chat

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.qhagoapp.data.AppDatabase
import com.example.qhagoapp.data.repository.ChatRepositoryImpl

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
        val repository = ChatRepositoryImpl(database.chatMessageDao())
        ChatViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar)
        
        val partnerId = arguments?.getString(ARG_PARTNER_ID)
        val partnerName = arguments?.getString(ARG_PARTNER_NAME)
        
        if (partnerId != null) {
            viewModel.navigateToDetail(partnerId, partnerName)
        } else {
            viewModel.navigateToList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                var isVisible by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    isVisible = true
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 120.dp, top = 40.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn() + scaleIn(
                            initialScale = 0.5f,
                            transformOrigin = TransformOrigin(0.9f, 0.9f)
                        ),
                        exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut() + scaleOut(targetScale = 0.5f)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.75f)
                                .clip(RoundedCornerShape(24.dp)),
                            tonalElevation = 12.dp,
                            shadowElevation = 8.dp
                        ) {
                            ChatMain(
                                viewModel = viewModel,
                                onClose = { 
                                    isVisible = false
                                    view?.postDelayed({ dismiss() }, 300)
                                }
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
        }
    }
}
