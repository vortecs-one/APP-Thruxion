package com.thruxion.app.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thruxion.app.R
import com.thruxion.app.utils.MetaMaskManager

class TokenAdapter : ListAdapter<MetaMaskManager.Token, TokenAdapter.TokenViewHolder>(TokenDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_token, parent, false)
        return TokenViewHolder(view)
    }

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TokenViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSymbol = view.findViewById<TextView>(R.id.tvTokenSymbol)
        private val tvName = view.findViewById<TextView>(R.id.tvTokenName)
        private val tvContract = view.findViewById<TextView>(R.id.tvTokenContract)
        private val tvBalance = view.findViewById<TextView>(R.id.tvTokenBalance)

        fun bind(token: MetaMaskManager.Token) {
            tvSymbol.text = token.symbol
            tvName.text = token.name
            val shortAddr = if (token.contractAddress.length > 10) 
                "${token.contractAddress.take(6)}...${token.contractAddress.takeLast(4)}" 
                else token.contractAddress
            tvContract.text = shortAddr
            tvBalance.text = token.balance
        }
    }

    class TokenDiffCallback : DiffUtil.ItemCallback<MetaMaskManager.Token>() {
        override fun areItemsTheSame(oldItem: MetaMaskManager.Token, newItem: MetaMaskManager.Token): Boolean {
            return oldItem.contractAddress == newItem.contractAddress
        }
        override fun areContentsTheSame(oldItem: MetaMaskManager.Token, newItem: MetaMaskManager.Token): Boolean {
            return oldItem == newItem
        }
    }
}
