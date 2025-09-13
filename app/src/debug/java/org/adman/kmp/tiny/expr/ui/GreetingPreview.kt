package org.adman.kmp.tiny.expr.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.adman.kmp.tiny.expr.component.Greeting

import org.adman.kmp.tiny.expr.ui.theme.AppTheme

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppTheme {
        Greeting("Android")
    }
}