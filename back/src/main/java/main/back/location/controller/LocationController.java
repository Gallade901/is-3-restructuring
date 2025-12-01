package main.back.location.controller;

import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import main.back.location.dto.LocationDtoRequest;
import main.back.location.dto.LocationDtoRequestEdit;
import main.back.location.dto.LocationDtoResponse;
import main.back.location.model.Location;
import main.back.person.controller.PersonWebSocketEndpoint;
import main.back.person.model.Person;
import main.back.user.model.User;

import java.util.ArrayList;
import java.util.List;

//@Singleton
@ApplicationScoped
@Path("/location")
public class LocationController {
    private final EntityManagerFactory emf = Persistence.createEntityManagerFactory("myDb");
    private final EntityManager em = emf.createEntityManager();
    private final EntityTransaction transaction = em.getTransaction();
    LocationWebSocketEndpoint locationWebSocket = new LocationWebSocketEndpoint();
    PersonWebSocketEndpoint personWebSocket = new PersonWebSocketEndpoint();

    @PreDestroy
    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addLocation(@Valid LocationDtoRequest locationDto) {
        try {
            transaction.begin();
            String login = locationDto.getLogin();
            User user = em.createQuery("SELECT u FROM User u WHERE u.login = :login", User.class)
                    .setParameter("login", login)
                    .getSingleResult();
            List<Person> personList = new ArrayList<>();
            Location location = new Location(locationDto.getX(), locationDto.getY(), locationDto.getZ(), user, personList);
            em.persist(location);
            transaction.commit();
            locationWebSocket.onMessage("");
            return Response.ok("Location успешно добавлен").build();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при добавлении location").build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLocations() {
        try {
            transaction.begin();
            List<Location> locations = em.createQuery("SELECT l FROM Location l", Location.class)
                    .getResultList();
            List<LocationDtoResponse> locationsResponse = new ArrayList<>();
            for (Location location : locations) {
                LocationDtoResponse locationDtoResponse = new LocationDtoResponse(
                        location.getId(),
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        location.getUser().getLogin()
                );
                locationsResponse.add(locationDtoResponse);
            }
            transaction.commit();
            return Response.ok(locationsResponse).build();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при получении locations").build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLocation(@PathParam("id") Integer id, @Valid LocationDtoRequestEdit locationDto) {
        try {
            transaction.begin();
            Location location = em.find(Location.class, id);
            if (location != null) {
                // Проверяем, есть ли связанные Person
                if (!location.getPeople().isEmpty()) {

                    Integer idForRep = locationDto.getId();
                    // Создаем и выполняем UPDATE запрос
                    String updateQuery = "UPDATE person SET location_id = :idForRep WHERE location_id = :id";
                    Query query = em.createNativeQuery(updateQuery);
                    query.setParameter("idForRep", idForRep);
                    query.setParameter("id", id);
                    int updatedRows = query.executeUpdate();
                    em.remove(location);
                    transaction.commit();
                    personWebSocket.onMessage("");
                    locationWebSocket.onMessage("");
                    return Response.ok("Обновлено " + updatedRows + " строк").build();
                }
                em.remove(location);
            }
            transaction.commit();
            locationWebSocket.onMessage("");
            return Response.ok().build();

        }  catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.serverError().build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLocation(@Valid LocationDtoRequestEdit locationDtoRequest) {
        try {
            transaction.begin();
            String login = locationDtoRequest.getOwner();
            User user = em.createQuery("SELECT u FROM User u WHERE u.login = :login", User.class)
                    .setParameter("login", login)
                    .getSingleResult();
            Location existingLocation = em.find(Location.class, locationDtoRequest.getId());

            if (existingLocation == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Location not found").build();
            }

            existingLocation.setX(locationDtoRequest.getX());
            existingLocation.setY(locationDtoRequest.getY());
            existingLocation.setZ(locationDtoRequest.getZ());

            em.merge(existingLocation);
            transaction.commit();
            locationWebSocket.onMessage("");
            personWebSocket.onMessage(""); // Обновляем также persons, так как они связаны с location
            return Response.ok("Location изменен").build();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при обновлении location").build();
        }
    }

    @GET
    @Path("/getId")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLocationId(@QueryParam("id") Integer id) {
        try {
            transaction.begin();
            Location location = em.find(Location.class, id);
            if (location == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Location not found").build();
            }
            LocationDtoResponse locationDtoResponse = new LocationDtoResponse(
                    location.getId(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getUser().getLogin()
            );
            transaction.commit();
            return Response.ok(locationDtoResponse).build();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при получении location").build();
        }
    }

    @GET
    @Path("/persons/{locationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPersonsByLocation(@PathParam("locationId") Integer locationId) {
        try {
            transaction.begin();
            Location location = em.find(Location.class, locationId);
            if (location == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Location not found").build();
            }

            List<Person> persons = location.getPeople();
            transaction.commit();
            return Response.ok(persons).build();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при получении persons").build();
        }
    }
}