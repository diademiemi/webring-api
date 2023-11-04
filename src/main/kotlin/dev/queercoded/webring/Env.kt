package dev.queercoded.webring

class Env {
    companion object {
        var api_token: String = System.getenv("WEBRING_API_TOKEN") ?: "ADMIN_TOKEN_CHANGE_ME"

        var webring_host: String = System.getenv("WEBRING_HOST")?.toString() ?: "localhost"

        var webring_path: String = System.getenv("WEBRINGPATH")?.toString() ?: "/"

        var webring_http_port: String = System.getenv("WEBRING_HTTP_PORT")?.toString() ?: "8080"
        var webring_https_port: String = System.getenv("WEBRING_HTTPS_PORT")?.toString() ?: "8443"  // Only checked if webring_https is true

        var webring_https: String = System.getenv("WEBRING_HTTPS")?.toString() ?: "false"

        var webring_scraper_user_agent: String = System.getenv("WEBRING_SCRAPER_USER_AGENT")?.toString() ?: "Mozilla/5.0 (compatible; Googlebot/2.1; Website Webring Scraper; +https://$webring_host$webring_path"

    }


}