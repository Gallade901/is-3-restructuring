package main.back.user.controller;

import jakarta.annotation.PreDestroy;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import main.back.user.dto.AnswerApplication;
import main.back.user.model.ApplicationAdmin;
import main.back.user.model.SessionUser;
import main.back.user.model.User;
import main.back.utils.PasswordHasher;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//@Singleton
@ApplicationScoped
@Path("/user")
@TransactionManagement(TransactionManagementType.CONTAINER) // Указываем, что транзакциями управляет контейнер
public class UserController {

    @PersistenceContext(unitName = "myDb")
    private EntityManager em;


    @PreDestroy
    public void close() {
    }

    @Schedule(hour = "*/4", persistent = false)
    @Transactional
    public void cleanExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<SessionUser> noActiveSessions = em.createQuery("SELECT s FROM SessionUser s WHERE s.expiresAt <= :currentTime", SessionUser.class)
                .setParameter("currentTime", now)
                .getResultList();
        for (SessionUser session : noActiveSessions) {
            em.remove(session);
        }
    }

    @POST
    @Path("/registration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional // Транзакция управляется контейнером
    public String registration(User user) {
        if (!em.createQuery("SELECT u FROM User u WHERE u.login = :login OR EXISTS (SELECT a FROM ApplicationAdmin a WHERE a.login = :login)")
                .setParameter("login", user.getLogin())
                .getResultList().isEmpty()) {
            return "Такой логин уже существует";
        }

        if (user.getRole().equals("ADMIN")) {
            if (!em.createQuery("SELECT u FROM User u WHERE u.role = :role")
                    .setParameter("role", "ADMIN")
                    .getResultList().isEmpty()) {
                ApplicationAdmin applicationAdmin = new ApplicationAdmin();
                applicationAdmin.setLogin(user.getLogin());
                applicationAdmin.setPassword(PasswordHasher.hashPassword(user.getPassword()));
                applicationAdmin.setRole(user.getRole());
                em.persist(applicationAdmin);
                return "Заявка создана";
            }
        }

        String hashedPassword = PasswordHasher.hashPassword(user.getPassword());
        user.setPassword(hashedPassword);
        System.out.println(hashedPassword);
        em.persist(user);
        return "Регистрация прошла успешно";
    }

    @POST
    @Path("/authorization")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response authorization(User user) {
        try {
            User authenticatedUser = (User) em.createQuery("SELECT u FROM User u WHERE u.login = :login and u.password = :password")
                    .setParameter("login", user.getLogin())
                    .setParameter("password", PasswordHasher.hashPassword(user.getPassword()))
                    .getSingleResult();
            String sessionId = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(4);
            SessionUser session = new SessionUser(sessionId, authenticatedUser, expiresAt);
            em.persist(session);

//            NewCookie sessionCookie = new NewCookie(
//                    "sessionId",
//                    sessionId,
//                    "/",
//                    null,
//                    1,
//                    null,
//                    14400,
//                    null,
//                    true,
//                    true,
//                    NewCookie.SameSite.NONE
//            );
            NewCookie cookie = new NewCookie.Builder("sessionId")
                    .value(sessionId)
                    .path("/")
                    .maxAge(14400)
                    .secure(true)
                    .httpOnly(true)
                    .sameSite(NewCookie.SameSite.NONE)
                    .build(); //
            return Response.ok("Авторизация успешна").cookie(cookie).header("Set-Cookie", "sessionId=" + sessionId + "; Max-Age=14400; Path=/; Secure; HttpOnly; SameSite=None").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/checkAuthorization")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional // Для операций чтения тоже можно использовать @Transactional
    public Response checkAuthorization(@CookieParam("sessionId") String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(false).build();
        }
        try {
            SessionUser session = em.createQuery(
                            "SELECT s FROM SessionUser s WHERE s.id = :sessionId AND s.expiresAt > :now", SessionUser.class)
                    .setParameter("sessionId", sessionId)
                    .setParameter("now", LocalDateTime.now())
                    .getSingleResult();
            Map<String, Object> userData = new HashMap<>();
            userData.put("login", session.getUser().getLogin());
            userData.put("role", session.getUser().getRole());
            return Response.ok(userData).build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(false).build();
        }
    }

    @POST
    @Path("/logout")
    @Transactional
    public Response logOut(@CookieParam("sessionId") String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Сессия не найдена").build();
        }
        try {
            int deletedCount = em.createQuery("DELETE FROM SessionUser s WHERE s.id = :sessionId")
                    .setParameter("sessionId", sessionId)
                    .executeUpdate();

            if (deletedCount > 0) {
                return Response.ok("Вы вышли из системы").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("Сессия не найдена").build();
            }
        } catch (Exception e) {
            // Откат транзакции произойдет автоматически при исключении
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при выходе из системы").build();
        }
    }

    @GET
    @Path("/applications")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response applications() {
        System.out.println("-------");
        try {
            List<ApplicationAdmin> applications = em.createQuery("SELECT a FROM ApplicationAdmin a", ApplicationAdmin.class)
                    .getResultList();
            System.out.println(applications);
            return Response.ok(applications).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при получении заявок").build();
        }
    }

    @POST
    @Path("/answerApplication")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response approveApplication(AnswerApplication request) {
        try {
            boolean flag = request.isFlag();
            ApplicationAdmin app = em.createQuery(
                            "SELECT a FROM ApplicationAdmin a WHERE a.login = :login", ApplicationAdmin.class)
                    .setParameter("login", request.getLogin())
                    .getSingleResult();

            if (flag) {
                User user = new User();
                user.setLogin(app.getLogin());
                user.setPassword(app.getPassword());
                user.setRole(app.getRole());
                em.persist(user);
            }

            em.remove(app);
            return Response.ok(flag ? "Заявка одобрена и пользователь добавлен" : "Заявка отклонена").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Заявка не найдена").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при обработке заявки").build();
        }
    }
}