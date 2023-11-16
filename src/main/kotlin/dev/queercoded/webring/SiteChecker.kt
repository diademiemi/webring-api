package dev.queercoded.webring

import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@jakarta.enterprise.context.RequestScoped
class SiteChecker() {

    @ConfigProperty(name="webring.host")
    lateinit var webring_host: String

    @ConfigProperty(name="webring.path")
    lateinit var webring_path: String

    @ConfigProperty(name="webring.http.port")
    lateinit var webring_http_port: String

    @ConfigProperty(name="webring.https.port")
    lateinit var webring_https_port: String

    @ConfigProperty(name="webring.https")
    lateinit var webring_https: String

    @ConfigProperty(name="webring.scraper.user-agent")
    lateinit var webring_scraper_user_agent: String

    @Inject
    lateinit var siteRepository: SiteRepository

    /*
     Check every enabled site whether the URL (proto + domain + path) contains a reference to
     the webring. If not, set dead_end to true.
    */
    @Transactional
    fun checkSites() {
        // Allow with or without port, check https if enabled
        // Check whether prev?source= and next?source= are in the page. Check that the domain is correct.
        // If not, set dead_end to true.

        val client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        print("Checking sites... ")

        val sites = siteRepository.listAll().filter { it.enabled }
        sites.forEach { site ->
            val url = if (site.https) {
                "https://${site.domain}${site.path}"
            } else {
                "http://${site.domain}${site.path}"
            }

            val request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("User-Agent", webring_scraper_user_agent)
                .build()

                // Check HTTP and HTTPS if enabled

                println("Checking ${site.domain}...")
                println("URL: ${url}")
                println("Site has checks disabled: ${site.disable_checks}")
                var found = site.disable_checks

                try {

                    val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                    println("Status code: ${response.statusCode()}")

                    if (webring_https.toBoolean()) {
                        if (response.body()
                                .contains("https://${webring_host}${webring_path}next?source=${site.domain}") || response.body()
                                .contains("https://${webring_host}:${webring_https_port}${webring_path}next?source=${site.domain}") && response.body()
                                .contains("https://${webring_host}${webring_path}prev?source=${site.domain}") || response.body()
                                .contains("https://${webring_host}:${webring_https_port}${webring_path}prev?source=${site.domain}")
                        ) {
                            println("Found HTTPS")
                            found = true
                        }
                    }
                    if (response.body()
                            .contains("http://${webring_host}${webring_path}next?source=${site.domain}") || response.body()
                            .contains("http://${webring_host}:${webring_http_port}${webring_path}next?source=${site.domain}") && response.body()
                            .contains("http://${webring_host}${webring_path}prev?source=${site.domain}") || response.body()
                            .contains("http://${webring_host}:${webring_http_port}${webring_path}prev?source=${site.domain}")
                    ) {
                        println("Found HTTP")
                        found = true
                    }
                } catch (e: Exception) {
                    println("Error checking ${site.domain}")
                    println(e)
                }

                println("Found: ${found}")
                site.dead_end = !found

                println("Saving site...")
                siteRepository.persist(site)


        }

    }

}
