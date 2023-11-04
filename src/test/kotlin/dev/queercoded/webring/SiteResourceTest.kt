package dev.queercoded.webring

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.transaction.Transactional
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SiteResourceTest {

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

        println(siteA)

        val siteB = Site()

        siteB.name = "Test Site B"
        siteB.domain = "test-site-b.example.com"
        siteB.author = "Test User B"
        siteB.path = "/path/"
        siteB.https = false

        val siteC = Site()

        siteC.name = "Test Site C"
        siteC.domain = "test-site-c.example.com"
        siteC.author = "Test User C"
        siteC.path = "/"
        siteC.enabled = false

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
            .header("Authorization", "Bearer ${Env.api_token}")
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
            .assertThat().body("size()", org.hamcrest.Matchers.greaterThan(0))
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
            .header("Authorization", "Bearer ${Env.api_token}")
            .post("/sites")
            .then()
            .statusCode(201)
            .assertThat().body("name", org.hamcrest.Matchers.equalTo("Test Site B"))
    }

    @Test
    @Order(5)
    fun testListSitesEndpoint2() {
        given()
            .`when`()
            .get("/sites/all")
            .then()
            .statusCode(200)
            .assertThat().body("size()", org.hamcrest.Matchers.greaterThan(0))
            .assertThat().body("name", org.hamcrest.Matchers.hasItem("Test Site A"))
            .assertThat().body("name", org.hamcrest.Matchers.hasItem("Test Site B"))
    }

}