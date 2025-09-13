package org.adman.kmp.tiny.expr

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val result = TinyExprJNI.eval("3 + 4 * 2")
            Log.e("KMP-TinyExpr", "Show TinyExpr result : $result ")
            App()
        }
    }
}



