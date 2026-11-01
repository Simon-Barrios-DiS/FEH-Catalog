package com.simon.fehcatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.simon.fehcatalog.ui.FehCatalogApp
import com.simon.fehcatalog.ui.theme.FEHCatalogTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FEHCatalogTheme {
                FehCatalogApp()
            }
        }
    }
}
