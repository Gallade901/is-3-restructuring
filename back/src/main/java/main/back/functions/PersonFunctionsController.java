package main.back.functions;

import jakarta.ejb.Stateless;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@Stateless
@ApplicationScoped
@Path("/person-functions")
public class PersonFunctionsController {

    @PersistenceContext(unitName = "myDb")
    private EntityManager entityManager;

    @GET
    @Path("/average-height")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAverageHeight() {
        try {
            BigDecimal averageHeight = (BigDecimal) entityManager
                    .createNativeQuery("SELECT get_average_height()")
                    .getSingleResult();

            return Response.ok(averageHeight).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error calculating average height: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/count-by-nationality")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCountByNationality() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<Object[]> queryResult = entityManager
                    .createNativeQuery("SELECT nationality, count FROM get_count_by_nationality()")
                    .getResultList();

            for (Object[] row : queryResult) {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("nationality", row[0]);
                rowMap.put("count", row[1]);
                result.add(rowMap);
            }

            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error getting count by nationality: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/count-height-greater")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCountHeightGreater(@QueryParam("height") long height) {
        try {
            Object result = entityManager
                    .createNativeQuery("SELECT get_count_height_greater(:heightParam)")
                    .setParameter("heightParam", height)
                    .getSingleResult();

            Long count = convertToLong(result);
            return Response.ok(count != null ? count : 0).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error counting height greater: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/count-by-hair-color")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCountByHairColor(@QueryParam("hairColor") String hairColor) {
        try {
            Object result = entityManager
                    .createNativeQuery("SELECT get_count_by_hair_color(:hairColorParam)")
                    .setParameter("hairColorParam", hairColor)
                    .getSingleResult();

            Long count = convertToLong(result);
            return Response.ok(count != null ? count : 0).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error counting by hair color: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/hair-color-percentage")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHairColorPercentage(@QueryParam("hairColor") String hairColor) {
        try {
            BigDecimal percentage = (BigDecimal) entityManager
                    .createNativeQuery("SELECT get_hair_color_percentage(:hairColor)")
                    .setParameter("hairColor", hairColor)
                    .getSingleResult();

            return Response.ok(percentage).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error calculating hair color percentage: " + e.getMessage())
                    .build();
        }
    }

    private Long convertToLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof BigDecimal) return ((BigDecimal) value).longValue();
        if (value instanceof BigInteger) return ((BigInteger) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}