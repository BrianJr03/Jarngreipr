package jr.brian.jarnfe.library

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class JarnGameSortTest {
    private fun game(
        title: String,
        lastPlayed: Long? = null,
        plays: Int? = null,
        released: String? = null,
    ) = JarnLibraryGame(
        id = title,
        systemId = "snes",
        title = title,
        metadata = JarnGameMetadata(lastPlayedAtMs = lastPlayed, playCount = plays, releaseDate = released),
    )

    @Test
    fun `name key drops a leading The or A`() {
        assertEquals("legend of zelda", nameSortKey("The Legend of Zelda"))
        assertEquals("boy and his blob", nameSortKey("A Boy and His Blob"))
        assertEquals("boy and his blob", nameSortKey("  a  Boy and His Blob "))
    }

    @Test
    fun `name key keeps words that only start like an article`() {
        assertEquals("theme hospital", nameSortKey("Theme Hospital"))
        assertEquals("aladdin", nameSortKey("Aladdin"))
        assertEquals("a", nameSortKey("A"))
    }

    @Test
    fun `name key keeps digits, which sort before letters`() {
        assertEquals("1942", nameSortKey("1942"))
        val sorted = listOf(game("Zombies Ate My Neighbors"), game("The 7th Saga"), game("1942"), game("Theme Hospital"))
            .sortedFor(JarnGameSort.Name)
        assertEquals(listOf("1942", "The 7th Saga", "Theme Hospital", "Zombies Ate My Neighbors"), sorted.map { it.title })
    }

    @Test
    fun `name sort files The Legend of Zelda under L and Theme Hospital under T`() {
        val sorted = listOf(game("Theme Hospital"), game("The Legend of Zelda"), game("A Boy and His Blob"), game("Mario"))
            .sortedFor(JarnGameSort.Name)
        assertEquals(listOf("A Boy and His Blob", "The Legend of Zelda", "Mario", "Theme Hospital"), sorted.map { it.title })
        assertEquals(sorted.map { nameSortKey(it.title) }, sorted.map { nameSortKey(it.title) }.sorted())
    }

    @Test
    fun `last played is newest first with never played last by name`() {
        val sorted = listOf(game("B"), game("Old", lastPlayed = 10), game("A"), game("New", lastPlayed = 99))
            .sortedFor(JarnGameSort.LastPlayed)
        assertEquals(listOf("New", "Old", "A", "B"), sorted.map { it.title })
    }

    @Test
    fun `most played is highest first with unplayed last by name`() {
        val sorted = listOf(game("Z"), game("Few", plays = 2), game("Lots", plays = 40), game("A"))
            .sortedFor(JarnGameSort.MostPlayed)
        assertEquals(listOf("Lots", "Few", "A", "Z"), sorted.map { it.title })
    }

    @Test
    fun `release year is oldest first with unknown years last`() {
        val sorted = listOf(
            game("None"),
            game("Iso", released = "1995-03-01"),
            game("EsDe", released = "19910101T000000"),
            game("Same year B", released = "1995"),
        ).sortedFor(JarnGameSort.ReleaseYear)
        assertEquals(listOf("EsDe", "Iso", "Same year B", "None"), sorted.map { it.title })
    }

    @Test
    fun `an unknown saved id falls back to name order`() {
        assertEquals(JarnGameSort.Name, JarnGameSort.fromId("Shuffle"))
        assertEquals(JarnGameSort.Name, JarnGameSort.fromId(null))
        assertEquals(JarnGameSort.MostPlayed, JarnGameSort.fromId("MostPlayed"))
    }

    @Test
    fun `next cycles through every order`() {
        assertEquals(JarnGameSort.LastPlayed, JarnGameSort.Name.next())
        assertEquals(JarnGameSort.Name, JarnGameSort.ReleaseYear.next())
    }

    @Test
    fun `no system manager means name order`() = runBlocking {
        assertEquals(JarnGameSort.Name, gameSort(null, "snes").first())
    }
}
