package jr.brian.home.data.config

import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import jr.brian.jarnfe.backup.JarnBackupItem
import org.junit.Assert.assertEquals
import org.junit.Test

class JarnCollectionsPreviewTest {
    private val chrono = JarnGameKeyConfig("snes", "Chrono.sfc")
    private val mario = JarnGameKeyConfig("snes", "Mario.sfc")

    private fun preview(backup: FeatureConfig, current: FeatureConfig, area: JarnBackupArea): JarnBackupItem =
        buildBackupPreview(JarngreiprConfig(feature = backup), JarngreiprConfig(feature = current))
            .items.first { it.area == area }

    @Test
    fun `an older backup shows collections and favorites as not included`() {
        val now = FeatureConfig(jarnCollections = listOf(JarnCollectionConfig("Mine")), jarnFavorites = listOf(mario))
        assertEquals(JarnBackupChange.NotIncluded, preview(FeatureConfig(), now, JarnBackupArea.Collections).change)
        assertEquals(JarnBackupChange.NotIncluded, preview(FeatureConfig(), now, JarnBackupArea.Favorites).change)
    }

    @Test
    fun `collections count new collections and new members in matching ones`() {
        val now = FeatureConfig(jarnCollections = listOf(JarnCollectionConfig("RPGs", 0, listOf(mario))))
        val backup = FeatureConfig(
            jarnCollections = listOf(
                JarnCollectionConfig("rpgs", 0, listOf(mario, chrono)),
                JarnCollectionConfig("Party", 1, listOf(mario)),
            ),
        )
        assertEquals(JarnBackupChange.Adds(3), preview(backup, now, JarnBackupArea.Collections).change)
    }

    @Test
    fun `a backup with nothing new is the same`() {
        val now = FeatureConfig(
            jarnCollections = listOf(JarnCollectionConfig("RPGs", 0, listOf(mario))),
            jarnFavorites = listOf(mario, chrono),
        )
        val backup = FeatureConfig(jarnCollections = emptyList(), jarnFavorites = listOf(chrono))
        assertEquals(JarnBackupChange.Same, preview(backup, now, JarnBackupArea.Collections).change)
        assertEquals(JarnBackupChange.Same, preview(backup, now, JarnBackupArea.Favorites).change)
    }

    @Test
    fun `favorites count only the ones not already favorite`() {
        val now = FeatureConfig(jarnFavorites = listOf(mario))
        val backup = FeatureConfig(jarnFavorites = listOf(mario, chrono))
        assertEquals(JarnBackupChange.Adds(1), preview(backup, now, JarnBackupArea.Favorites).change)
    }

    @Test
    fun `collections and favorites never count as other settings`() {
        val backup = FeatureConfig(jarnCollections = listOf(JarnCollectionConfig("New")), jarnFavorites = listOf(chrono))
        assertEquals(JarnBackupChange.Same, preview(backup, FeatureConfig(), JarnBackupArea.OtherSettings).change)
    }
}
