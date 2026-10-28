package com.tricosoft.booknest

import android.app.Application
import com.tricosoft.booknest.state.Graph
import com.tricosoft.booknest.ui.BnFonts

class BookNestApp : Application() {
    lateinit var graph: Graph
        private set

    override fun onCreate() {
        super.onCreate()
        BnFonts.init(this)
        graph = Graph(this)
    }
}
