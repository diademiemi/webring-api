package dev.queercoded

import io.quarkus.scheduler.Scheduled
import jakarta.transaction.Transactional
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class SiteChecker(val siteRepository: SiteRepository) {

    @Scheduled(every = "{check_interval}")
    @Transactional
            /*
             Check every enabled site whether the URL (proto + domain + path) contains a reference to
             the webring. If not, set dead_end to true.
            */
    fun checkSites() {
        // Allow with or without port, check https if enabled
        // Check whether prev?source= and next?source= are in the page. Check that the domain is correct.
        // If not, set dead_end to true.

        val client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        val sites = siteRepository.listAll().filter { it.enabled }
        sites.forEach { site ->
            val url = if (site.https) {
                "https://${site.domain}${site.path}"
            } else {
                "http://${site.domain}${site.path}"
            }

            val request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("User-Agent", Env.webring_scraper_user_agent)
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                site.dead_end = true
                siteRepository.persist(site)
            } else {
                // Check HTTP and HTTPS if enabled

                var found = false

                if (Env.webring_https.toBoolean()) {
                    if (response.body().contains("https://${Env.webring_host}${Env.webring_path}/next?source=${site.domain}") && response.body().contains("${Env.webring_host}${Env.webring_path}/prev?source=${site.domain}")) {
                        found = true
                        siteRepository.persist(site)
                    }
                }
                if (response.body().contains("http://${Env.webring_host}${Env.webring_path}/next?source=${site.domain}") && response.body().contains("${Env.webring_host}${Env.webring_path}/prev?source=${site.domain}")) {
                    found = true
                    siteRepository.persist(site)
                }

                if (!found) {
                    site.dead_end = true
                    siteRepository.persist(site)
                }

            }
        }

    }

}