package party.debaucherytea.manny

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import party.debaucherytea.manny.data.FlashcardRepository
import party.debaucherytea.manny.ui.MannyApp
import party.debaucherytea.manny.ui.theme.MannyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = FlashcardRepository(applicationContext.assets)
        setContent { MannyTheme { MannyApp(repository) } }
    }
}
