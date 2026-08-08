package com.thruxion.app.ui.settings

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thruxion.app.R
import com.thruxion.app.utils.MetaMaskManager
import com.google.android.material.button.MaterialButton

class WalletDetailsDialogFragment : DialogFragment() {

    private lateinit var tokenAdapter: TokenAdapter
    private val tokens = MetaMaskManager.SEPOLIA_TEST_TOKENS.map { it.copy() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_wallet_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val address = MetaMaskManager.getPublicAddress()
        val tvFullAddress = view.findViewById<TextView>(R.id.tvFullAddress)
        val tvBalance = view.findViewById<TextView>(R.id.tvBalance)
        val btnCopy = view.findViewById<ImageButton>(R.id.btnCopyAddress)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnClose)
        val rootContainer = view.findViewById<View>(R.id.root_container)
        val rvTokens = view.findViewById<RecyclerView>(R.id.rvTokens)

        tvFullAddress.text = address

        btnCopy.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("Wallet Address", address)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        btnClose.setOnClickListener { dismiss() }
        
        rootContainer.setOnClickListener { dismiss() }

        // Setup Tokens RecyclerView
        tokenAdapter = TokenAdapter()
        rvTokens.layoutManager = LinearLayoutManager(context)
        rvTokens.adapter = tokenAdapter
        tokenAdapter.submitList(tokens)

        // Fetch ETH Balance
        MetaMaskManager.getBalance().thenAccept { balance ->
            requireActivity().runOnUiThread {
                tvBalance.text = balance
            }
        }

        // Fetch Token Balances
        tokens.forEachIndexed { index, token ->
            MetaMaskManager.getTokenBalance(token).thenAccept { balance ->
                requireActivity().runOnUiThread {
                    token.balance = balance
                    tokenAdapter.notifyItemChanged(index)
                }
            }
        }
    }
}
