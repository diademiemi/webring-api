package dev.queercoded.webring

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class SiteRepository : PanacheRepository<Site> {
    fun findByName(name: String) = find("name", name).firstResult()
    fun findBydomain(domain: String) = find("domain", domain).firstResult()
    fun findByAuthor(author: String) = find("author", author).firstResult()

}