package com.selae.signage.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.selae.signage.R
import com.selae.signage.model.GameJackpot
import com.selae.signage.model.JackpotAddon

/**
 * JackpotAdapter.kt
 *
 * Adaptador RecyclerView para mostrar las tarjetas de botes.
 * Usa ListAdapter con DiffUtil para actualizaciones eficientes.
 */
class JackpotAdapter : ListAdapter<GameJackpot, JackpotAdapter.JackpotViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GameJackpot>() {
            override fun areItemsTheSame(old: GameJackpot, new: GameJackpot) =
                old.idGame == new.idGame

            override fun areContentsTheSame(old: GameJackpot, new: GameJackpot) =
                old == new
        }
    }

    // ============================================================
    // VIEWHOLDER
    // ============================================================

    inner class JackpotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgLogo: ImageView = itemView.findViewById(R.id.imgGameLogo)
        private val tvGameName: TextView = itemView.findViewById(R.id.tvGameName)
        private val tvJackpot: TextView = itemView.findViewById(R.id.tvJackpotAmount)
        private val tvDrawDate: TextView = itemView.findViewById(R.id.tvDrawDate)
        private val containerAddons: LinearLayout = itemView.findViewById(R.id.containerAddons)

        fun bind(game: GameJackpot) {
            // Nombre del juego
            tvGameName.text = game.name

            // Importe del bote formateado
            tvJackpot.text = FormatUtils.formatJackpot(game.getJackpotAmount())

            // Fecha del próximo sorteo en español
            tvDrawDate.text = FormatUtils.formatDrawDate(game.getDrawDateString())

            // Logo del juego con Glide (caché activada)
            val logoOptions = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_logo_placeholder)
                .error(R.drawable.ic_logo_placeholder)
                .centerInside()

            Glide.with(itemView.context)
                .load(game.getLogoUrl())
                .apply(logoOptions)
                .into(imgLogo)

            // Addons (premios adicionales)
            containerAddons.removeAllViews()
            val addons = game.getAddonList()
            if (addons.isNotEmpty()) {
                containerAddons.visibility = View.VISIBLE
                addons.forEach { addon -> addAddonView(addon) }
            } else {
                containerAddons.visibility = View.GONE
            }
        }

        /**
         * Infla y agrega una vista de addon al contenedor.
         */
        private fun addAddonView(addon: JackpotAddon) {
            val addonView = LayoutInflater.from(itemView.context)
                .inflate(R.layout.item_addon_row, containerAddons, false)

            addonView.findViewById<TextView>(R.id.tvAddonName).text = addon.getAddonName()
            addonView.findViewById<TextView>(R.id.tvAddonAmount).text =
                FormatUtils.formatJackpot(addon.getAddonAmount())

            containerAddons.addView(addonView)
        }
    }

    // ============================================================
    // LIFECYCLE DE RECYCLERVIEW
    // ============================================================

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JackpotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jackpot_card, parent, false)
        return JackpotViewHolder(view)
    }

    override fun onBindViewHolder(holder: JackpotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
