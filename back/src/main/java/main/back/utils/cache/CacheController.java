package main.back.utils.cache;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/cache")
@Produces(MediaType.APPLICATION_JSON)
public class CacheController {

    @Inject
    private CacheStatisticsService cacheStatisticsService;

    @POST
    @Path("/statistics/enable")
    public Response enableStatistics() {
        cacheStatisticsService.enableStatistics();
        return Response.ok("Cache statistics enabled").build();
    }

    @POST
    @Path("/statistics/disable")
    public Response disableStatistics() {
        cacheStatisticsService.disableStatistics();
        return Response.ok("Cache statistics disabled").build();
    }

    @GET
    @Path("/statistics")
    public Response getStatistics() {
        String stats = cacheStatisticsService.getCacheStatistics();
        return Response.ok(stats).build();
    }

    @POST
    @Path("/clear")
    public Response clearCache() {
        cacheStatisticsService.clearCache();
        return Response.ok("Cache cleared").build();
    }

    @GET
    @Path("/status")
    public Response getStatus() {
        boolean enabled = cacheStatisticsService.isStatisticsEnabled();
        return Response.ok("Statistics enabled: " + enabled).build();
    }
}