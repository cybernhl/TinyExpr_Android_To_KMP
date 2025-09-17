package org.adman.kmp.tiny.expr.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.adman.kmp.tiny.expr.getPlatform
import org.adman.kmp.tiny.expr.getTinyExprResult

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val platform = getPlatform()
    Text(
        text = "Hello $name! , ${platform.name}!",
        modifier = modifier
    )
    Text(
        text = "Native TinyExpr result : ${getTinyExprResult()}",
        modifier = modifier
    )
}