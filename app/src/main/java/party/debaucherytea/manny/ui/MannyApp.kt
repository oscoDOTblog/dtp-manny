package party.debaucherytea.manny.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import party.debaucherytea.manny.R
import party.debaucherytea.manny.data.*
import java.io.IOException
import org.json.JSONException

private sealed interface LibraryState {
    data object Loading : LibraryState
    data object Failed : LibraryState
    data class Ready(val sections: List<FlashcardSection>) : LibraryState
}

@Composable
fun MannyApp(repository: FlashcardRepository) {
    var attempt by remember { mutableIntStateOf(0) }
    val state by produceState<LibraryState>(LibraryState.Loading, repository, attempt) {
        value = LibraryState.Loading
        value = try {
            LibraryState.Ready(repository.loadSections())
        } catch (error: IOException) {
            Log.e("Manny", "Cannot read bundled vocabulary", error)
            LibraryState.Failed
        } catch (error: JSONException) {
            Log.e("Manny", "Invalid vocabulary JSON", error)
            LibraryState.Failed
        } catch (error: IllegalArgumentException) {
            Log.e("Manny", "Invalid vocabulary data", error)
            LibraryState.Failed
        }
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        when (val current = state) {
            LibraryState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            LibraryState.Failed -> Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.load_error))
                Button(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
            }
            is LibraryState.Ready -> LibraryNavigation(current.sections)
        }
    }
}

@Composable
private fun LibraryNavigation(sections: List<FlashcardSection>) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "sections") {
        composable("sections") {
            SectionListScreen(sections) { section ->
                navController.navigate("cards/${android.net.Uri.encode(section.id)}") { launchSingleTop = true }
            }
        }
        composable("cards/{sectionId}") { entry ->
            val section = sections.find { it.id == entry.arguments?.getString("sectionId") }
            FlashcardScreen(section, onBack = { navController.popBackStack() })
        }
    }
}
