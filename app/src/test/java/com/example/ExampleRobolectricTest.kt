package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ScrollSearchStateBus
import com.example.model.OverlayUiVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TikTokLens", appName)
    }

    @Test
    fun `test state bus username updates and query template`() {
        ScrollSearchStateBus.updateDetectedUsername("zendaya")
        assertEquals("zendaya", ScrollSearchStateBus.currentUsername.value)
        assertEquals("@zendaya", ScrollSearchStateBus.searchBarText.value)

        ScrollSearchStateBus.selectTag("model")
        assertEquals("@zendaya model", ScrollSearchStateBus.searchBarText.value)
        val effectiveQuery = ScrollSearchStateBus.getEffectiveSearchQuery()
        assertEquals("zendaya model", effectiveQuery)

        // When user types custom text keeping @
        ScrollSearchStateBus.setSearchBarText("@zendaya photoshoot red carpet")
        assertEquals("zendaya photoshoot red carpet", ScrollSearchStateBus.getEffectiveSearchQuery())

        // When user removes @ from search bar
        ScrollSearchStateBus.setSearchBarText("carro esportivo")
        assertEquals("carro esportivo", ScrollSearchStateBus.getEffectiveSearchQuery())

        ScrollSearchStateBus.setVersion(OverlayUiVersion.F7)
        assertEquals(OverlayUiVersion.F7, ScrollSearchStateBus.activeVersion.value)
        assertTrue(ScrollSearchStateBus.activeVersion.value.isCleanPhotoOnly)
    }

    @Test
    fun `test all F and V versions available`() {
        val fVersions = OverlayUiVersion.entries.filter { it.isCleanPhotoOnly }
        assertEquals(5, fVersions.size) // F1, F2, F5, F6, F7

        val vVersions = OverlayUiVersion.entries.filter { !it.isCleanPhotoOnly }
        assertEquals(5, vVersions.size) // V1, V2, V5, V6, V7
    }

    @Test
    fun `test tag management`() {
        ScrollSearchStateBus.addCustomTag("photoshoot")
        assertTrue(ScrollSearchStateBus.tags.value.contains("photoshoot"))
    }

    @Test
    fun `test accessibility service detects instagram and tiktok usernames`() {
        val service = org.robolectric.Robolectric.buildService(com.example.service.TikTokAccessibilityService::class.java)
            .create()
            .get()

        val root = android.view.accessibility.AccessibilityNodeInfo.obtain()
        root.packageName = "com.instagram.android"

        val child = android.view.accessibility.AccessibilityNodeInfo.obtain()
        child.packageName = "com.instagram.android"
        child.viewIdResourceName = "com.instagram.android:id/row_feed_photo_profile_name"
        child.text = "neymarjr"

        val shadowRoot = org.robolectric.Shadows.shadowOf(root)
        shadowRoot.addChild(child)

        val method = com.example.service.TikTokAccessibilityService::class.java
            .getDeclaredMethod("extractUsernameFast", android.view.accessibility.AccessibilityNodeInfo::class.java, Int::class.javaPrimitiveType)
        method.isAccessible = true
        val detected = method.invoke(service, root, 800) as String?

        assertEquals("neymarjr", detected)
    }

    @Test
    fun `test user plan switching and custom search requires explicit config`() {
        ScrollSearchStateBus.setPlan(com.example.model.UserPlan.BASIC)
        assertEquals(com.example.model.UserPlan.BASIC, ScrollSearchStateBus.currentPlan.value)
        org.junit.Assert.assertFalse(ScrollSearchStateBus.isPremiumActive.value)

        ScrollSearchStateBus.setPlan(com.example.model.UserPlan.PREMIUM)
        assertEquals(com.example.model.UserPlan.PREMIUM, ScrollSearchStateBus.currentPlan.value)
        assertTrue(ScrollSearchStateBus.isPremiumActive.value)

        ScrollSearchStateBus.togglePlan()
        assertEquals(com.example.model.UserPlan.BASIC, ScrollSearchStateBus.currentPlan.value)
        org.junit.Assert.assertFalse(ScrollSearchStateBus.isPremiumActive.value)

        assertEquals("", com.example.util.GoogleSearchHelper.sanitizeSearchApiUrl("http://insecure.example.test"))
        assertEquals(
            "",
            com.example.util.GoogleSearchHelper.sanitizeSearchApiUrl(
                "https://www.googleapis.com/customsearch/v1?key=test-key-not-real"
            )
        )
        assertFalse(com.example.util.GoogleSearchHelper.isSearchApiConfigured(""))
    }
}
