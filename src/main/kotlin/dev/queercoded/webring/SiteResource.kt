package dev.queercoded.webring

import io.quarkus.qute.Location
import io.quarkus.qute.Template
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.resteasy.reactive.RestHeader
import java.net.URI

@Path("/")
class SiteResource(val siteRepository: SiteRepository) {

    @ConfigProperty(name="webring.api_token")
    lateinit var api_token: String

    @Inject
    @Location("index.html")
    lateinit var index: Template

    @RestHeader("Authorization")
    lateinit var authHeader: String

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    fun index(): Response {
        return index.data("sites", listEnabledSites()).render().let {
            Response.ok(it).status(200).build()
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sites/all")
    fun getSites(): Response {
        // Don't list disabled sites
        val sites = siteRepository.listAll().filter { it.enabled }
        return Response.ok(sites).status(200).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sites/name/{domain}")
    fun getSiteByDomain(@PathParam("domain") domain: String): Response {
        val site = siteRepository.findBydomain(domain) ?: return Response.ok().status(404).build()
        return Response.ok(site).status(200).build()
    }

    fun listEnabledSites(): List<Site> {
        return siteRepository.listAll().filter { it.enabled && !it.dead_end }
    }

    fun getNextSite(sourceDomain: String): Site {
        val sites = listEnabledSites()
        val site = sites.find { it.domain == sourceDomain } ?: return Site()
        val index = sites.indexOf(site)
        val nextIndex = if (index == sites.lastIndex) 0 else index + 1
        return sites[nextIndex]
    }

    fun getPrevSite(sourceDomain: String): Site {
        val sites = listEnabledSites()
        val site = sites.find { it.domain == sourceDomain } ?: return Site()
        val index = sites.indexOf(site)
        val prevIndex = if (index == 0) sites.lastIndex else index - 1
        return sites[prevIndex]
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sites/next/{source_domain}")
    fun getNextSiteJson(@PathParam("source_domain") sourceDomain: String): Response {
        val site = getNextSite(sourceDomain)
        return Response.ok(site).status(200).build()
    }
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sites/prev/{source_domain}")
    fun getPrevSiteJson(@PathParam("source_domain") sourceDomain: String): Response {
        val site = getPrevSite(sourceDomain)
        return Response.ok(site).status(200).build()
    }

    @GET
    @Path("/next")
    // Get the next site in order of creation. Find the given source_domain and return the next site. If the source_domain is the last site, return the first site.
    fun gotoNextSite(@QueryParam("source") sourceDomain: String): Response {
        val nextSite = getNextSite(sourceDomain)

        // Send redirect to the next
        // Vars are domain, https: Bool, path, build this into URL
        return if (nextSite.https) {
            Response.status(Response.Status.TEMPORARY_REDIRECT)
                .location(URI("https://${nextSite.domain}${nextSite.path}")).build()
        } else {
            Response.status(Response.Status.TEMPORARY_REDIRECT)
                .location(URI("http://${nextSite.domain}${nextSite.path}")).build()
        }
    }

    @GET
    @Path("/prev")
    // Get the previous site in order of creation. Find the given source_domain and return the previous site. If the source_domain is the first site, return the last site.
    fun gotoPrevSite(@QueryParam("source") source_domain: String): Response {
        val prevSite = getPrevSite(source_domain)

        // Send redirect to the prev
        // Vars are domain, https: Bool, path, build this into URL
        return if (prevSite.https) {
            Response.status(Response.Status.TEMPORARY_REDIRECT)
                .location(URI("https://${prevSite.domain}${prevSite.path}")).build()
        } else {
            Response.status(Response.Status.TEMPORARY_REDIRECT)
                .location(URI("http://${prevSite.domain}${prevSite.path}")).build()
        }
    }
    // ADMIN API CALLS

    fun checkIfAuthenticated(): Boolean {
        return authHeader == "Bearer ${api_token}"
    }

    @Inject
    lateinit var siteChecker: SiteChecker

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sites/force-recheck")
    fun forceRecheck(): Response {
        // Check if authHeader is set
        if (!checkIfAuthenticated()) {
            return Response.ok().status(401).build()
        }

        siteChecker.checkSites()

        return Response.ok().status(200).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sites/id/{id}")
    fun getSiteById(@PathParam("id") id: Long): Response {
        // Check if authHeader is set
        if (!checkIfAuthenticated()) {
            return Response.ok().status(401).build()
        }

        val site = siteRepository.findById(id) ?: return Response.ok().status(404).build()
        return Response.ok(site).status(200).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sites/disabled")
    fun getSitesDisabled(): Response {
        // Check if authHeader is set
        if (!checkIfAuthenticated()) {
            return Response.ok().status(401).build()
        }

        val sites = siteRepository.listAll().filter { !it.enabled }
        return Response.ok(sites).status(200).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sites/all-dead-end")
    fun getSitesDeadEnd(): Response {
        // Check if authHeader is set
        if (!checkIfAuthenticated()) {
            return Response.ok().status(401).build()
        }

        val sites = siteRepository.listAll().filter { it.dead_end }
        return Response.ok(sites).status(200).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sites/all-plus-disabled")
    fun getSitesPlusDisabled(): Response {
        // Check if authHeader is set
        if (!checkIfAuthenticated()) {
            return Response.ok().status(401).build()
        }

        val sites = siteRepository.listAll()
        return Response.ok(sites).status(200).build()
    }

    @PUT
    @Transactional
    @Path("/sites/id/{id}/disable")
    fun disableSiteById(@PathParam("id") id: Long): Response {
        // Check if authHeader is set
        if (!checkIfAuthenticated()) {
            return Response.ok().status(401).build()
        }

        val site = siteRepository.findById(id) ?: return Response.ok().status(404).build()
        site.enabled = false
        siteRepository.persist(site)
        return Response.ok(site).status(200).build()
    }

    @PUT
    @Transactional
    @Path("/sites/id/{id}/enable")
    fun enableSiteById(@PathParam("id") id: Long): Response {
        // Check if authHeader is set
        if (!checkIfAuthenticated()) {
            return Response.ok().status(401).build()
        }

        val site = siteRepository.findById(id) ?: return Response.ok().status(404).build()
        site.enabled = true
        siteRepository.persist(site)
        return Response.ok(site).status(200).build()
    }

    @PUT
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/sites/id/{id}/update")
    fun updateSiteById(@PathParam("id") id: Long, site: Site): Response {
        // Check if authHeader is set
        if (!checkIfAuthenticated()) {
            return Response.ok().status(401).build()
        }

        // Check if site with new name exists
        val siteWithNewName = siteRepository.findByName(site.name)
        if (siteWithNewName != null && siteWithNewName.id != id) {
            return Response.ok().status(409).build()
        }

        // Check if site with new domain exists
        val siteWithNewDomain = siteRepository.findBydomain(site.domain)
        if (siteWithNewDomain != null && siteWithNewDomain.id != id) {
            return Response.ok().status(409).build()
        }

        val oldSite = siteRepository.findById(id) ?: return Response.ok().status(404).build()
        oldSite.name = site.name
        oldSite.domain = site.domain
        oldSite.path = site.path
        oldSite.https = site.https
        oldSite.author = site.author
        oldSite.enabled = site.enabled
        oldSite.disable_checks = site.disable_checks
        oldSite.dead_end = site.dead_end


        siteRepository.persist(oldSite)
        return Response.ok(oldSite).status(200).build()
    }

    @DELETE
    @Transactional
    @Path("/sites/id/{id}")
    fun deleteSiteById(@PathParam("id") id: Long): Response {
        // Check if authHeader is set
        if (!checkIfAuthenticated()) {
            return Response.ok().status(401).build()
        }

        val site = siteRepository.findById(id) ?: return Response.ok().status(404).build()
        siteRepository.delete(site)
        return Response.ok().status(204).build()
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/sites")
    fun addSite(site: Site): Response {
        // Check if authHeader is set
        if (!checkIfAuthenticated()) {
            return Response.ok().status(401).build()
        }

        siteRepository.persist(site)
        return Response.ok(site).status(201).build()
    }

}

@Provider
class SiteExceptionManager : ExceptionMapper<NotFoundException> {
    @Inject
    @Location("404.html")
    lateinit var notFoundTemplate: Template

    @Produces(MediaType.TEXT_HTML)
    override fun toResponse(exception: NotFoundException?): Response? {

        return notFoundTemplate.data("exception", exception).render().let {
            Response.status(Response.Status.NOT_FOUND).entity(it)
                .header("Content-Type", "text/html")
                .build()
        }
    }
}