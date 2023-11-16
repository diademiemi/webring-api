package dev.queercoded.webring

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.config.RedirectConfig
import jakarta.transaction.Transactional
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SiteResourceTest {


    @ConfigProperty(name="webring.api_token")
    lateinit var api_token: String

    companion object {

        var initialSites = ArrayList<Site>()

    }

    @Test
    @Order(1)
    fun populateSiteList() {
        val siteA = Site()

        siteA.name = "Test Site A"
        siteA.domain = "test-site-a.example.com"
        siteA.author = "Test User A"
        siteA.path = "/"
        siteA.disable_checks = true

        println(siteA)

        val siteB = Site()

        siteB.name = "Test Site B"
        siteB.domain = "test-site-b.example.com"
        siteB.author = "Test User B"
        siteB.path = "/path/"
        siteB.https = false
        siteB.disable_checks = true

        val siteC = Site()

        siteC.name = "Test Site C"
        siteC.domain = "test-site-c.example.com"
        siteC.author = "Test User C"
        siteC.path = "/"
        siteC.enabled = false
        siteC.disable_checks = true

        initialSites = arrayListOf(siteA, siteB, siteC)

    }

    @Test
    @Order(2)
    @Transactional
    fun testCreateNewSiteEndpoint1() {

        given()
            .`when`()
            .contentType("application/json")
            .body(initialSites[0])
            .header("Authorization", "Bearer ${api_token}")
            .post("/sites")
            .then()
            .statusCode(201)
            .assertThat().body("name", org.hamcrest.Matchers.equalTo("Test Site A"))
    }

    @Test
    @Order(3)
    fun testListSitesEndpoint1() {
        given()
            .`when`()
            .get("/sites/all")
            .then()
            .statusCode(200)
            .assertThat().body("size()", org.hamcrest.Matchers.equalTo(1))
            .assertThat().body("name", org.hamcrest.Matchers.hasItem("Test Site A"))
    }

    @Test
    @Order(4)
    @Transactional
    fun testCreateNewSiteEndpoint2() {

        given()
            .`when`()
            .contentType("application/json")
            .body(initialSites[1])
            .header("Authorization", "Bearer ${api_token}")
            .post("/sites")
            .then()
            .statusCode(201)
            .assertThat().body("name", org.hamcrest.Matchers.equalTo("Test Site B"))
    }

    @Test
    @Order(5)
    @Transactional
    fun testCreateNewSiteEndpoint3() {

        given()
            .`when`()
            .contentType("application/json")
            .body(initialSites[2])
            .header("Authorization", "Bearer ${api_token}")
            .post("/sites")
            .then()
            .statusCode(201)
            .assertThat().body("name", org.hamcrest.Matchers.equalTo("Test Site C"))

    }


        @Test
    @Order(6)
    fun testListSitesEndpoint2() {
        var sites: Array<Site> = given()
            .`when`()
            .get("/sites/all")
            .then()
            .statusCode(200)
            .assertThat().body("size()", org.hamcrest.Matchers.greaterThan(0))
            .assertThat().body("name", org.hamcrest.Matchers.hasItem("Test Site A"))
            .assertThat().body("name", org.hamcrest.Matchers.hasItem("Test Site B"))
            .extract().`as`(Array<Site>::class.java)

        // Check that Site C is NOT in the list
        assert(!sites.any { it.name == "Test Site C" })

    }

    @Test
    @Order(7)
    fun testListAllPlusDisabledSitesEndpoint1() {
        var sites: Array<Site> = given()
            .`when`()
            .header("Authorization", "Bearer ${api_token}")

            .get("/sites/all-plus-disabled")
            .then()
            .statusCode(200)
            .assertThat().body("size()", org.hamcrest.Matchers.greaterThan(0))
            .assertThat().body("name", org.hamcrest.Matchers.hasItem("Test Site A"))
            .assertThat().body("name", org.hamcrest.Matchers.hasItem("Test Site B"))
            .assertThat().body("name", org.hamcrest.Matchers.hasItem("Test Site C"))
            .extract().`as`(Array<Site>::class.java)

    }

    @Test
    @Order(8)
    fun testListDisabledSitesEndpoint1() {
        var sites: Array<Site> = given()
            .`when`()
            .header("Authorization", "Bearer ${api_token}")

            .get("/sites/disabled")
            .then()
            .statusCode(200)
            .assertThat().body("size()", org.hamcrest.Matchers.greaterThan(0))
            .assertThat().body("name", org.hamcrest.Matchers.hasItem("Test Site C"))
            .extract().`as`(Array<Site>::class.java)
    }

    @Test
    @Order(9)
    @Transactional
    fun testEnableSitesEndpoint1() {
        // Get Site C ID
        val siteC = given()
            .`when`()
            .header("Authorization", "Bearer ${api_token}")
            .get("/sites/name/test-site-c.example.com")
            .then()
            .statusCode(200)
            .extract().`as`(Site::class.java)
        // Enable Site C
        given()
            .`when`()
            .header("Authorization", "Bearer ${api_token}")
            .put("/sites/id/${siteC.id}/enable")
            .then()
            .statusCode(200)
            .assertThat().body("name", org.hamcrest.Matchers.equalTo("Test Site C"))
            .assertThat().body("enabled", org.hamcrest.Matchers.equalTo(true))
    }

    @Test
    @Order(10)
    fun testListDisabledSitesEndpoint2() {
        given()
            .`when`()
            .header("Authorization", "Bearer ${api_token}")

            .get("/sites/disabled")
            .then()
            .statusCode(200)
            .assertThat().body("size()", org.hamcrest.Matchers.equalTo(0))

    }

    @Test
    @Order(11)
    fun testGetNextSiteEndpoint1() {
        var site: Site = given()
            .`when`()
            .get("/sites/next/test-site-a.example.com")
            .then()
            .statusCode(200)
            .assertThat().body("name", org.hamcrest.Matchers.equalTo("Test Site B"))
            .extract().`as`(Site::class.java)

    }

    @Test
    @Order(12)
    fun testGetNextSiteEndpoint2() {
        var site: Site = given()
            .`when`()
            .get("/sites/next/test-site-c.example.com")
            .then()
            .statusCode(200)
            .assertThat().body("name", org.hamcrest.Matchers.equalTo("Test Site A"))
            .extract().`as`(Site::class.java)

    }

    @Test
    @Order(13)
    fun testGetPrevSiteEndpoint1() {
        var site: Site = given()
            .`when`()
            .get("/sites/prev/test-site-a.example.com")
            .then()
            .statusCode(200)
            .assertThat().body("name", org.hamcrest.Matchers.equalTo("Test Site C"))
            .extract().`as`(Site::class.java)

    }

    @Test
    @Order(14)
    fun testGetPrevSiteEndpoint2() {
        var site: Site = given()
            .`when`()
            .get("/sites/prev/test-site-c.example.com")
            .then()
            .statusCode(200)
            .assertThat().body("name", org.hamcrest.Matchers.equalTo("Test Site B"))
            .extract().`as`(Site::class.java)

    }

    @Test
    @Order(15)
    fun testGoToNextSiteEndpoint1() {
        given()
            .`when`()
            .config(
                RestAssured.config().redirect(RedirectConfig.redirectConfig().followRedirects(false)))

            .get("/next?source=test-site-c.example.com")
            .then()
            .statusCode(307)
    }

    @Test
    @Order(16)
    fun testGoToPrevSiteEndpoint1() {
        given()
            .`when`()
            .config(
                RestAssured.config().redirect(RedirectConfig.redirectConfig().followRedirects(false)))
            .get("/prev?source=test-site-c.example.com")
            .then()
            .statusCode(307)
    }

    @Test
    @Order(18)
    fun testUpdateSiteEndpoint1() {
        var siteC = given()
            .`when`()
            .header("Authorization", "Bearer ${api_token}")
            .get("/sites/name/test-site-c.example.com")
            .then()
            .statusCode(200)
            .extract().`as`(Site::class.java)

        siteC.name = "Test Site C Updated"
        siteC.domain = "test-site-c-updated.example.com"
        siteC.author = "Test User C Updated"
        siteC.path = "/updated/"
        siteC.https = false

        given()
            .`when`()
            .contentType("application/json")
            .body(siteC)
            .header("Authorization", "Bearer ${api_token}")
            .put("/sites/id/${siteC.id}/update")
            .then()
            .statusCode(200)
            .assertThat().body("name", org.hamcrest.Matchers.equalTo("Test Site C Updated"))
            .assertThat().body("domain", org.hamcrest.Matchers.equalTo("test-site-c-updated.example.com"))
            .assertThat().body("author", org.hamcrest.Matchers.equalTo("Test User C Updated"))
            .assertThat().body("path", org.hamcrest.Matchers.equalTo("/updated/"))
            .assertThat().body("https", org.hamcrest.Matchers.equalTo(false))

    }
}
