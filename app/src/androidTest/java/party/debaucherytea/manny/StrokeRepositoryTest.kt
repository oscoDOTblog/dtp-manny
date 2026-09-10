package party.debaucherytea.manny

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import party.debaucherytea.manny.strokes.AssetStrokeDataRepository

class StrokeRepositoryTest {
    private fun repository() =
        AssetStrokeDataRepository(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test fun loadKnownCharacter() = runBlocking {
        val data = repository().load("狗")
        assertNotNull(data)
        assertEquals("狗", data!!.character)
        assertEquals(8, data.strokes.size)
        assertEquals(8, data.medians.size)
    }

    @Test fun loadCachesResults() = runBlocking {
        val repository = repository()
        assertSame(repository.load("狗"), repository.load("狗"))
    }

    @Test fun loadMissingCharacterReturnsNull() = runBlocking {
        val repository = repository()
        assertNull(repository.load("𠀋"))
        assertNull(repository.load("A"))
        assertNull(repository.load("苹果"))
        assertNull(repository.load(""))
    }
}
