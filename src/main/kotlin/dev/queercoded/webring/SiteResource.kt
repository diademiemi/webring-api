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

    @Inject
    @Location("nosuchdomain.html")
    lateinit var noSuchDomain: Template

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
        val site = sites.find { it.domain == sourceDomain }
            ?: throw NullPointerException("Source domain not found: $sourceDomain")
        val index = sites.indexOf(site)
        val nextIndex = if (index == sites.lastIndex) 0 else index + 1
        return sites[nextIndex]
    }

    fun getPrevSite(sourceDomain: String): Site {
        val sites = listEnabledSites()
        val site = sites.find { it.domain == sourceDomain }
            ?: throw NullPointerException("Source domain not found: $sourceDomain")
        val index = sites.indexOf(site)
        val prevIndex = if (index == 0) sites.lastIndex else index - 1
        return sites[prevIndex]
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sites/next/{source_domain}")
    fun getNextSiteJson(@PathParam("source_domain") sourceDomain: String): Response {
        return try {
            val site = getNextSite(sourceDomain)
            Response.ok(site).status(200).build()
        } catch (e: NullPointerException) {
            Response.status(Response.Status.NOT_FOUND).entity("Domain not found: $sourceDomain").build()
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sites/prev/{source_domain}")
    fun getPrevSiteJson(@PathParam("source_domain") sourceDomain: String): Response {
        return try {
            val site = getPrevSite(sourceDomain)
            Response.ok(site).status(200).build()
        } catch (e: NullPointerException) {
            Response.status(Response.Status.NOT_FOUND).entity("Domain not found: $sourceDomain").build()
        }
    }

    @GET
    @Path("/next")
    fun gotoNextSite(@QueryParam("source") sourceDomain: String): Response {
        return try {
            val nextSite = getNextSite(sourceDomain)
            var url = "http://${nextSite.domain}${nextSite.path}"
            if (nextSite.https) {
                url = "https://${nextSite.domain}${nextSite.path}"
            }
            Response.status(Response.Status.TEMPORARY_REDIRECT)
                .location(URI(url))
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .build()
        } catch (e: NullPointerException) {
            serveNoSuchDomainTemplate(sourceDomain)
        }
    }

    @GET
    @Path("/prev")
    fun gotoPrevSite(@QueryParam("source") sourceDomain: String): Response {
        return try {
            val prevSite = getPrevSite(sourceDomain)
            var url = "http://${prevSite.domain}${prevSite.path}"
            if (prevSite.https) {
                url = "https://${prevSite.domain}${prevSite.path}"
            }
            Response.status(Response.Status.TEMPORARY_REDIRECT)
                    .location(URI(url))
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .build()
        } catch (e: NullPointerException) {
            serveNoSuchDomainTemplate(sourceDomain)
        }
    }

    private fun serveNoSuchDomainTemplate(sourceDomain: String): Response {
        // Assuming notFoundTemplate is accessible here
        return noSuchDomain.data("domain", sourceDomain).render().let {
            Response.status(Response.Status.NOT_FOUND).entity(it)
                .header("Content-Type", "text/html")
                .build()
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
    fun updateSiteById(@PathParam("id") id: Long, site: HashMap<String, String>): Response {
        // Check if authHeader is set
        if (!checkIfAuthenticated()) {
            return Response.ok().status(401).build()
        }

        // This hashmap may be incomplete, so check whether a value exists before checking it

        // Check if site with new name exists
        site["name"]?.let {
            if (siteRepository.findByName(it) != null) {
                return Response.ok().status(409).build()
            }
        }

        // Check if site with new domain exists
        site["domain"]?.let {
            if (siteRepository.findBydomain(it) != null) {
                return Response.ok().status(409).build()
            }
        }

        val oldSite = siteRepository.findById(id) ?: return Response.ok().status(404).build()

        // Only update changed fields
        site["name"]?.let { if (oldSite.name != it) oldSite.name = it }
        site["domain"]?.let { if (oldSite.domain != it) oldSite.domain = it }
        site["path"]?.let { if (oldSite.path != it) oldSite.path = it }
        site["https"]?.let { it.toBoolean().let { value -> if (oldSite.https != value) oldSite.https = value } }
        site["author"]?.let { if (oldSite.author != it) oldSite.author = it }
        site["enabled"]?.let { it.toBoolean().let { value -> if (oldSite.enabled != value) oldSite.enabled = value } }
        site["disable_checks"]?.let { it.toBoolean().let { value -> if (oldSite.disable_checks != value) oldSite.disable_checks = value } }
        site["dead_end"]?.let { it.toBoolean().let { value -> if (oldSite.dead_end != value) oldSite.dead_end = value } }

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