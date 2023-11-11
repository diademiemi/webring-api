package dev.queercoded.webring

import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.RestHeader
import java.net.URI

@Path("/")
class SiteResource(val siteRepository: SiteRepository) {

    @RestHeader("Authorization")
    lateinit var authHeader: String

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
    @Path("/sites/next?source={source_domain}")
    // Get the next site in order of creation. Find the given source_domain and return the next site. If the source_domain is the last site, return the first site.
    fun gotoNextSite(@PathParam("source_domain") sourceDomain: String): Response {
        val nextSite = getNextSite(sourceDomain)

        // Send redirect to the next
        // Vars are domain, https: Bool, path, build this into URL
        return if (nextSite.https) {
            Response.status(Response.Status.MOVED_PERMANENTLY)
                .location(URI("https://${nextSite.domain}${nextSite.path}")).build()
        } else {
            Response.status(Response.Status.MOVED_PERMANENTLY)
                .location(URI("http://${nextSite.domain}${nextSite.path}")).build()
        }
    }

    @GET
    @Path("/sites/prev?source={source_domain}")
    // Get the previous site in order of creation. Find the given source_domain and return the previous site. If the source_domain is the first site, return the last site.
    fun gotoPrevSite(@PathParam("source_domain") source_domain: String): Response {
        val prevSite = getPrevSite(source_domain)

        // Send redirect to the prev
        // Vars are domain, https: Bool, path, build this into URL
        return if (prevSite.https) {
            Response.status(Response.Status.MOVED_PERMANENTLY)
                .location(URI("https://${prevSite.domain}${prevSite.path}")).build()
        } else {
            Response.status(Response.Status.MOVED_PERMANENTLY)
                .location(URI("http://${prevSite.domain}${prevSite.path}")).build()
        }
    }

    @GET
    @Path("/")
    fun getRandomSite(): Response {
        val sites = listEnabledSites()
        val randomSite = sites.random()
        // Send redirect to the random site
        // Vars are domain, https: Bool, path, build this into URL
        return if (randomSite.https) {
            Response.status(Response.Status.MOVED_PERMANENTLY)
                .location(URI("https://${randomSite.domain}${randomSite.path}")).build()
        } else {
            Response.status(Response.Status.MOVED_PERMANENTLY)
                .location(URI("http://${randomSite.domain}${randomSite.path}")).build()
        }
    }


    // ADMIN API CALLS

    fun checkIfAuthenticated(): Boolean {
        return authHeader == "Bearer ${Env.api_token}"
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

        var oldSite = siteRepository.findById(id) ?: return Response.ok().status(404).build()
        site.id = oldSite.id

        siteRepository.persist(site)
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
