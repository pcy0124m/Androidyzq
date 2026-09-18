package com.example.totp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.totp.databinding.ItemAccountBinding

class AccountAdapter(
    var items: MutableList<Account>,
    private val onDelete: (Int) -> Unit,
    private val onCopy: (String) -> Unit
) : RecyclerView.Adapter<AccountAdapter.VH>() {

    inner class VH(val b: ItemAccountBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val acc = items[position]
        val code = TotpUtil.generate(acc.secret)
        val remain = TotpUtil.secondsRemaining()
        holder.b.tvName.text = acc.name
        holder.b.tvCode.text = code
        holder.b.progress.max = 30
        holder.b.progress.progress = remain
        holder.b.btnCopy.setOnClickListener { onCopy(code) }
        holder.b.btnDelete.setOnClickListener { onDelete(position) }
    }
}
