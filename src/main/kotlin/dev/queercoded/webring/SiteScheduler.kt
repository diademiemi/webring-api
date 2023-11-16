package dev.queercoded.webring

import io.quarkus.scheduler.Scheduled
import jakarta.inject.Inject

internal class SiteScheduler {

    @Inject
    lateinit var siteChecker: SiteChecker

    @Scheduled(cron = "{webring.scrape_cron}")
    fun scrapeSites() {
        siteChecker.checkSites()
    }

}
