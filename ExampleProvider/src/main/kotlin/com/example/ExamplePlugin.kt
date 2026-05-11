package com.example

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class ExamplePlugin: Plugin() {
    private var activity: AppCompatActivity? = null

    override fun load(context: Context) {
        activity = context as? AppCompatActivity

        // C'est ici qu'on enregistre ton provider Anime-Sama pour qu'il apparaisse dans l'app
        registerMainAPI(AnimeSama())

        // On garde cette partie pour la gestion des paramètres (BlankFragment)
        openSettings = {
            val frag = BlankFragment(this)
            activity?.let {
                frag.show(it.supportFragmentManager, "Frag")
            }
        }
    }
}