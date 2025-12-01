package main.back.coordinates.controller;

import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import main.back.coordinates.dto.CoordinateDtoRequestEdit;
import main.back.coordinates.dto.CoordinatesDtoRequest;
import main.back.coordinates.dto.CoordinatesDtoResponse;
import main.back.coordinates.model.Coordinates;
import main.back.person.controller.PersonWebSocketEndpoint;
import main.back.person.model.Person;
import main.back.user.model.User;


import java.util.ArrayList;
import java.util.List;


//@Singleton
@ApplicationScoped
@Path("/coordinates")
public class CoordinatesController {
    private final EntityManagerFactory emf = Persistence.createEntityManagerFactory("myDb");
    private final EntityManager em = emf.createEntityManager();
    private final EntityTransaction transaction = em.getTransaction();
    CoordinateWebSocketEndpoint coordinateWebSocket = new CoordinateWebSocketEndpoint();
    PersonWebSocketEndpoint personWebSocketEndpoint = new PersonWebSocketEndpoint();

    @PreDestroy
    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addCoordinates(@Valid CoordinatesDtoRequest coordinatesDto) {
        try {
            transaction.begin();
            String login = coordinatesDto.getLogin();
            User user = em.createQuery("SELECT u FROM User u WHERE u.login = :login", User.class)
                    .setParameter("login", login)
                    .getSingleResult();
            List<Person> personList = new ArrayList<>();
            Coordinates coordinates = new Coordinates(coordinatesDto.getX(),coordinatesDto.getY(), user, personList);
            em.persist(coordinates);
            transaction.commit();
            coordinateWebSocket.onMessage("");
            return Response.ok("Координаты успешно добавлены").build();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при добавлении координат").build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCoordinates() {
        try {
            transaction.begin();
            List<Coordinates> coordinates = em.createQuery("SELECT c FROM Coordinates c", Coordinates.class)
                    .getResultList();
            List<CoordinatesDtoResponse> coordinatesResponse = new ArrayList<>();
            for (Coordinates c : coordinates) {
                CoordinatesDtoResponse coordinatesDtoResponse = new CoordinatesDtoResponse(c.getId(), c.getX(), c.getY(), c.getUser().getLogin());
                coordinatesResponse.add(coordinatesDtoResponse);
            }
            transaction.commit();
            return Response.ok(coordinatesResponse).build();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при получении координат").build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCoordinates(@PathParam("id") Integer id, @Valid CoordinateDtoRequestEdit coordinatesDto) {
        try {
            transaction.begin();
            Coordinates coordinate = em.find(Coordinates.class, id);
            if (coordinate != null) {
                if (!coordinate.getPeople().isEmpty()) {
                    Integer idForRep = coordinatesDto.getId();
                    String updateQuery = "UPDATE person SET coordinates_id = :idForRep WHERE coordinates_id = :id";
                    Query query = em.createNativeQuery(updateQuery);
                    query.setParameter("idForRep", idForRep);
                    query.setParameter("id", id);
                    int updatedRows = query.executeUpdate();
                    em.remove(coordinate);
                    transaction.commit();
                    coordinateWebSocket.onMessage("");
                    personWebSocketEndpoint.onMessage("");
                    return Response.ok("Обновлено " + updatedRows + " строк").build();
                }
                em.remove(coordinate);
            }
            transaction.commit();
            coordinateWebSocket.onMessage("");
            personWebSocketEndpoint.onMessage("");
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
    public Response updateCoordinates(@Valid CoordinateDtoRequestEdit coordinateDtoRequest) {
        transaction.begin();
        String login = coordinateDtoRequest.getOwner();
        User user = em.createQuery("SELECT u FROM User u WHERE u.login = :login", User.class)
                .setParameter("login", login)
                .getSingleResult();
        Coordinates existingCoordanate = em.find(Coordinates.class, coordinateDtoRequest.getId());

        if (existingCoordanate == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Flat not found").build();
        }

        existingCoordanate.setX(coordinateDtoRequest.getX());
        existingCoordanate.setY(coordinateDtoRequest.getY());


        em.merge(existingCoordanate);
        transaction.commit();
        coordinateWebSocket.onMessage("");
        return Response.ok("Координаты изменены").build();
    }

    @GET
    @Path("/getId")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlatId(@QueryParam("id") Integer id) {
        try {
            transaction.begin();
            Coordinates coordinate = em.find(Coordinates.class, id);
            CoordinatesDtoResponse coordinateDtoResponse = new CoordinatesDtoResponse(coordinate.getId(), coordinate.getX(), coordinate.getY(), coordinate.getUser().getLogin());
            transaction.commit();
            return Response.ok(coordinateDtoResponse).build();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.serverError().build();
        }
    }
}
