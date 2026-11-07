package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `test search engine urls with safe search disabled`() {
    val duckUrl = com.example.util.GoogleSearchHelper.buildSearchUrl(
        engine = com.example.model.SearchEngine.DUCKDUCKGO,
        query = "zendaya",
        isSafeSearchDisabled = true
    )
    assertTrue(duckUrl.contains("duckduckgo.com"))
    assertTrue(duckUrl.contains("kp=-2"))

    val bingUrl = com.example.util.GoogleSearchHelper.buildSearchUrl(
        engine = com.example.model.SearchEngine.BING,
        query = "zendaya",
        isSafeSearchDisabled = true
    )
    assertTrue(bingUrl.contains("bing.com"))
    assertTrue(bingUrl.contains("adlt=off"))

    val googleUrl = com.example.util.GoogleSearchHelper.buildSearchUrl(
        engine = com.example.model.SearchEngine.GOOGLE,
        query = "zendaya",
        isSafeSearchDisabled = true
    )
    assertTrue(googleUrl.contains("google.com"))
    assertTrue(googleUrl.contains("safe=off"))
  }

  @Test
  fun `test safe search toggle in state bus`() {
    com.example.data.ScrollSearchStateBus.setSafeSearchDisabled(true)
    assertTrue(com.example.data.ScrollSearchStateBus.isSafeSearchDisabled.value)

    com.example.data.ScrollSearchStateBus.toggleSafeSearch()
    assertFalse(com.example.data.ScrollSearchStateBus.isSafeSearchDisabled.value)

    com.example.data.ScrollSearchStateBus.setSafeSearchDisabled(true)
    assertTrue(com.example.data.ScrollSearchStateBus.isSafeSearchDisabled.value)
  }

  @Test
  fun `test search engine selection in state bus`() {
    com.example.data.ScrollSearchStateBus.setSearchEngine(com.example.model.SearchEngine.BING)
    assertEquals(com.example.model.SearchEngine.BING, com.example.data.ScrollSearchStateBus.selectedSearchEngine.value)

    com.example.data.ScrollSearchStateBus.setSearchEngine(com.example.model.SearchEngine.DUCKDUCKGO)
    assertEquals(com.example.model.SearchEngine.DUCKDUCKGO, com.example.data.ScrollSearchStateBus.selectedSearchEngine.value)

    com.example.data.ScrollSearchStateBus.setSearchEngine(com.example.model.SearchEngine.GOOGLE)
    assertEquals(com.example.model.SearchEngine.GOOGLE, com.example.data.ScrollSearchStateBus.selectedSearchEngine.value)
  }

  @Test
  fun `test high quality filter in search urls`() {
    val googleHqUrl = com.example.util.GoogleSearchHelper.buildSearchUrl(
        engine = com.example.model.SearchEngine.GOOGLE,
        query = "khaby",
        isHighQualityFilter = true
    )
    assertTrue(googleHqUrl.contains("isz:l"))

    val bingHqUrl = com.example.util.GoogleSearchHelper.buildSearchUrl(
        engine = com.example.model.SearchEngine.BING,
        query = "khaby",
        isHighQualityFilter = true
    )
    assertTrue(bingHqUrl.contains("imagesize-large"))

    val ddgHqUrl = com.example.util.GoogleSearchHelper.buildSearchUrl(
        engine = com.example.model.SearchEngine.DUCKDUCKGO,
        query = "khaby",
        isHighQualityFilter = true
    )
    assertTrue(ddgHqUrl.contains("size:Large"))
  }

  @Test
  fun `premium request targets the backend and includes search parameters`() {
    val url = com.example.util.GoogleSearchHelper.buildPremiumSearchUrl(
        query = "creator photo",
        apiUrl = "https://lens-search.example.test",
        start = 11,
        num = 100
    )

    val requestUrl = requireNotNull(url)
    assertTrue(requestUrl.startsWith("https://lens-search.example.test/api/search?"))
    assertFalse(requestUrl.contains("key="))
    assertFalse(requestUrl.contains("cx="))
    assertTrue(requestUrl.contains("num=100"))
    assertTrue(requestUrl.contains("start=11"))
    assertTrue(requestUrl.contains("safe=off"))
    assertTrue(requestUrl.contains("highQuality=true"))
    assertTrue(requestUrl.contains("quality=HIGH"))
    assertTrue(requestUrl.contains("q=creator+photo"))
  }

  @Test
  fun `premium request includes selected quality`() {
    val url = com.example.util.GoogleSearchHelper.buildPremiumSearchUrl(
      query = "creator photo",
      apiUrl = "https://lens-search.example.test",
      highQualityOnly = false,
      quality = com.example.model.ImageQuality.ULTRA
    )

    assertTrue(requireNotNull(url).contains("quality=ULTRA"))
    assertTrue(requireNotNull(url).contains("highQuality=false"))
  }

  @Test
  fun `premium request supports any image size without provider filter`() {
    val url = com.example.util.GoogleSearchHelper.buildPremiumSearchUrl(
      query = "creator photo",
      apiUrl = "https://lens-search.example.test",
      quality = com.example.model.ImageQuality.ANY
    )

    val requestUrl = requireNotNull(url)
    assertTrue(requestUrl.contains("quality=HIGH"))
    assertTrue(requestUrl.contains("highQuality=false"))
  }

  @Test
  fun `premium provider request encodes reverse image input and rejects insecure input`() {
    val url = com.example.util.GoogleSearchHelper.buildPremiumProviderSearchUrl(
      provider = com.example.model.PremiumSearchProvider.YANDEX_REVERSE,
      query = "",
      imageUrl = "https://images.example.test/photo.jpg",
      apiUrl = "https://lens-search.example.test"
    )

    assertTrue(requireNotNull(url).contains("provider=yandex_reverse"))
    assertTrue(requireNotNull(url).contains("imageUrl=https%3A%2F%2Fimages.example.test%2Fphoto.jpg"))
    assertTrue(requireNotNull(url).contains("highQuality=true"))
    assertTrue(requireNotNull(url).contains("quality=HIGH"))
    assertNull(
      com.example.util.GoogleSearchHelper.buildPremiumProviderSearchUrl(
        provider = com.example.model.PremiumSearchProvider.YANDEX_REVERSE,
        query = "",
        imageUrl = "http://images.example.test/photo.jpg",
        apiUrl = "https://lens-search.example.test"
      )
    )
    assertTrue(
      requireNotNull(
        com.example.util.GoogleSearchHelper.buildPremiumProviderSearchUrl(
          provider = com.example.model.PremiumSearchProvider.TIKTOK_DISCOVER,
          query = "dance trends",
          apiUrl = "https://lens-search.example.test"
        )
      ).contains("provider=tiktok_discover")
    )
  }

  @Test
  fun `premium request rejects missing backend and invalid page`() {
    assertNull(
        com.example.util.GoogleSearchHelper.buildPremiumSearchUrl(
            query = "creator",
            apiUrl = ""
        )
    )
    assertNull(
        com.example.util.GoogleSearchHelper.buildPremiumSearchUrl(
            query = "creator",
            apiUrl = "https://lens-search.example.test",
            start = 9902
        )
    )
    assertEquals("", com.example.util.GoogleSearchHelper.sanitizeSearchApiUrl("http://insecure.example.test"))
    assertEquals("", com.example.util.GoogleSearchHelper.sanitizeSearchApiUrl("https://example.test?key=secret"))
    assertEquals("", com.example.util.GoogleSearchHelper.sanitizeSearchApiUrl("https://example.test/api/search"))
  }

  @Test
  fun `custom search maps only returned image links and next page`() {
    val response = JSONObject(
        """{
          "items": [
            {
              "title": "Returned result",
              "link": "https://images.example.test/photo.jpg",
              "previewUrl": "https://thumbs.example.test/photo.jpg",
              "displayLink": "images.example.test",
              "image": {"width": 1600, "height": 900}
            },
            {"title": "No dimensions", "link": "https://images.example.test/other.jpg"},
            {"title": "Invalid result", "link": "javascript:alert(1)"},
            {"title": "Missing link"}
          ],
          "queries": {"nextPage": [{"startIndex": 101}]}
        }"""
    )

    val page = com.example.util.GoogleSearchHelper.mapGoogleCustomSearchResponse("creator", response)

    assertEquals(2, page.photos.size)
    assertEquals("https://images.example.test/photo.jpg", page.photos.first().url)
    assertEquals("https://thumbs.example.test/photo.jpg", page.photos.first().previewUrl)
    assertEquals(1600, page.photos.first().width)
    assertEquals(900, page.photos.first().height)
    assertEquals(0, page.photos.last().width)
    assertEquals(0, page.photos.last().height)
    assertEquals(101, page.nextStartIndex)
  }

  @Test
  fun `custom search empty response stays empty`() {
    val page = com.example.util.GoogleSearchHelper.mapGoogleCustomSearchResponse(
        "creator",
        JSONObject("""{"items": []}""")
    )

    assertTrue(page.photos.isEmpty())
    assertNull(page.nextStartIndex)
  }

  @Test
  fun `test social platform selection`() {
    com.example.data.ScrollSearchStateBus.selectSocialPlatform(com.example.model.SocialPlatform.INSTAGRAM)
    assertEquals(com.example.model.SocialPlatform.INSTAGRAM, com.example.data.ScrollSearchStateBus.selectedPlatform.value)

    com.example.data.ScrollSearchStateBus.selectSocialPlatform(com.example.model.SocialPlatform.YOUTUBE_SHORTS)
    assertEquals(com.example.model.SocialPlatform.YOUTUBE_SHORTS, com.example.data.ScrollSearchStateBus.selectedPlatform.value)

    com.example.data.ScrollSearchStateBus.selectSocialPlatform(com.example.model.SocialPlatform.BLUESKY)
    assertEquals(com.example.model.SocialPlatform.BLUESKY, com.example.data.ScrollSearchStateBus.selectedPlatform.value)

    com.example.data.ScrollSearchStateBus.selectSocialPlatform(com.example.model.SocialPlatform.X)
    assertEquals(com.example.model.SocialPlatform.X, com.example.data.ScrollSearchStateBus.selectedPlatform.value)
    assertEquals(com.example.model.SocialPlatform.X, com.example.model.SocialPlatform.fromPackage("com.twitter.android"))

    com.example.data.ScrollSearchStateBus.selectSocialPlatform(com.example.model.SocialPlatform.TIKTOK)
    assertEquals(com.example.model.SocialPlatform.TIKTOK, com.example.data.ScrollSearchStateBus.selectedPlatform.value)
  }

  @Test
  fun `test save search history toggle in state bus`() {
    com.example.data.ScrollSearchStateBus.setSaveSearchHistory(true)
    assertTrue(com.example.data.ScrollSearchStateBus.isSaveSearchHistoryEnabled.value)

    com.example.data.ScrollSearchStateBus.toggleSaveSearchHistory()
    assertFalse(com.example.data.ScrollSearchStateBus.isSaveSearchHistoryEnabled.value)

    com.example.data.ScrollSearchStateBus.toggleSaveSearchHistory()
    assertTrue(com.example.data.ScrollSearchStateBus.isSaveSearchHistoryEnabled.value)
  }

  @Test
  fun `test search history dialog open state`() {
    com.example.data.ScrollSearchStateBus.setHistoryDialogOpen(false)
    assertFalse(com.example.data.ScrollSearchStateBus.isHistoryDialogOpen.value)

    com.example.data.ScrollSearchStateBus.openHistoryDialog()
    assertTrue(com.example.data.ScrollSearchStateBus.isHistoryDialogOpen.value)

    com.example.data.ScrollSearchStateBus.setHistoryDialogOpen(false)
    assertFalse(com.example.data.ScrollSearchStateBus.isHistoryDialogOpen.value)
  }

  @Test
  fun `test search history item creation`() {
    val item = com.example.data.db.SearchHistoryItem(
        id = 1L,
        query = "lorena maria",
        cleanQuery = "lorena maria",
        searchEngine = "Google",
        searchType = "Fotos",
        creatorUsername = "@lorenamaria"
    )
    assertEquals(1L, item.id)
    assertEquals("lorena maria", item.query)
    assertEquals("Google", item.searchEngine)
    assertEquals("Fotos", item.searchType)
    assertEquals("@lorenamaria", item.creatorUsername)
    assertTrue(item.timestamp > 0)
  }
}
