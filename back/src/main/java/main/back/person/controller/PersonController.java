package main.back.person.controller;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import main.back.coordinates.model.Coordinates;
import main.back.location.model.Location;
import main.back.person.dto.*;
import main.back.person.model.ImportHistory;
import main.back.person.model.Person;
import main.back.user.model.User;
import main.back.utils.cache.CacheStatisticsLogging;
import main.back.utils.minio.ImportTransactionService;
import main.back.utils.minio.MinioService;
import org.w3c.dom.ls.LSOutput;

import javax.naming.InitialContext;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

//@Singleton
@RequestScoped
@Path("/person")
@CacheStatisticsLogging
public class PersonController {
    private final EntityManagerFactory emf = Persistence.createEntityManagerFactory("myDb");
    private final EntityManager em = emf.createEntityManager();
    private final EntityTransaction transaction = em.getTransaction();
    PersonWebSocketEndpoint personWebSocket = new PersonWebSocketEndpoint();
    @Inject
    private MinioService minioService;

    @Inject
    private ImportTransactionService transactionService;

    @PreDestroy
    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
    @POST
    @Path("/import-file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importPersonsFromFile(
            @FormDataParam("file") InputStream fileInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("login") String login) {

        if (fileInputStream == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Файл не предоставлен")
                    .build();
        }

        try {
            // Создаем final копии переменных для использования в лямбде
            final String finalLogin = login;
            final InputStream finalFileInputStream = fileInputStream;
            final FormDataContentDisposition finalFileDetail = fileDetail;

            // Создаем запись истории импорта
            User user = em.createQuery("SELECT u FROM User u WHERE u.login = :login", User.class)
                    .setParameter("login", finalLogin)
                    .getSingleResult();

            final ImportHistory importHistory = new ImportHistory(user, "IN_PROGRESS", 0, LocalDateTime.now());

            // Читаем и парсим JSON из файла
            String fileContent = readInputStreamToString(finalFileInputStream);
            final List<PersonDtoImport> personImportDtos = parseJsonToDtoList(fileContent);

            // Сбрасываем поток для повторного использования
            final InputStream resetInputStream = new java.io.ByteArrayInputStream(fileContent.getBytes());

            // Выполняем распределенную транзакцию
            boolean success = transactionService.executeImportTransaction(
                    em,
                    resetInputStream,
                    finalFileDetail.getFileName(),
                    "application/json",
                    fileContent.length(),
                    importHistory,
                    () -> {
                        // Лямбда с операцией импорта в БД
                        performDatabaseImport(personImportDtos, user, importHistory);
                    }
            );

            if (success) {
                personWebSocket.onMessage("");
                return Response.ok("Успешно импортировано " + importHistory.getImportedCount() + " объектов").build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Ошибка при импорте данных")
                        .build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Ошибка при импорте: " + e.getMessage())
                    .build();
        }
    }

    private String readInputStreamToString(InputStream inputStream) throws Exception {
        try (java.util.Scanner scanner = new java.util.Scanner(inputStream).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private List<PersonDtoImport> parseJsonToDtoList(String jsonContent) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.readValue(jsonContent,
                mapper.getTypeFactory().constructCollectionType(List.class, PersonDtoImport.class));
    }

    private void performDatabaseImport(List<PersonDtoImport> personImportDtos, User user, ImportHistory importHistory) {
        List<Person> importedPersons = new ArrayList<>();

        for (PersonDtoImport dto : personImportDtos) {
            String name = dto.getName().trim();

            // Проверка существования персонажа
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(p) FROM Person p WHERE p.name = :name", Long.class);
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
            query.setParameter("name", name.trim());
            Long existingCount = query.getSingleResult();

            if (existingCount > 0) {
                throw new RuntimeException("Персонаж с именем '" + name + "' уже существует");
            }

            // Создание сущностей...
            Coordinates coordinates = new Coordinates(
                    dto.getCoordinateX(),
                    dto.getCoordinateY(),
                    user,
                    new ArrayList<>()
            );
            em.persist(coordinates);

            Location location = new Location(
                    dto.getLocationX(),
                    dto.getLocationY(),
                    dto.getLocationZ(),
                    user,
                    new ArrayList<>()
            );
            em.persist(location);

            Person person = new Person(
                    dto.getName(),
                    coordinates,
                    dto.getEyeColor(),
                    dto.getHairColor(),
                    location,
                    dto.getHeight(),
                    dto.getBirthday(),
                    dto.getWeight(),
                    dto.getNationality(),
                    user
            );
            em.persist(person);

            coordinates.getPeople().add(person);
            em.merge(coordinates);

            location.getPeople().add(person);
            em.merge(location);

            importedPersons.add(person);
        }

        importHistory.setStatus("SUCCESS");
        importHistory.setImportedCount(importedPersons.size());
        em.merge(importHistory);
    }

    @GET
    @Path("/import-history")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportHistory(@QueryParam("login") String login) {
        try {
            String query;
            List<ImportHistory> historyList;

            User currentUser = em.createQuery("SELECT u FROM User u WHERE u.login = :login", User.class)
                    .setParameter("login", login)
                    .getSingleResult();

            if ("ADMIN".equals(currentUser.getRole())) {
                query = "SELECT ih FROM ImportHistory ih ORDER BY ih.importDate DESC";
                historyList = em.createQuery(query, ImportHistory.class).getResultList();
            } else {
                query = "SELECT ih FROM ImportHistory ih WHERE ih.user.login = :login ORDER BY ih.importDate DESC";
                historyList = em.createQuery(query, ImportHistory.class)
                        .setParameter("login", login)
                        .getResultList();
            }

            // Преобразуем в DTO с URL для скачивания
            List<ImportHistoryDto> historyDtos = new ArrayList<>();
            for (ImportHistory history : historyList) {
                ImportHistoryDto dto = new ImportHistoryDto(
                        history.getId(),
                        history.getUser().getLogin(),
                        history.getStatus(),
                        history.getImportedCount(),
                        history.getImportDate(),
                        history.getFileName(),
                        history.getFileStored()
                );

                // Генерируем URL для скачивания, если файл сохранен
                if (history.getFileStored() != null && history.getFileStored() &&
                        history.getFileName() != null) {
                    try {
                        String downloadUrl = minioService.getFileUrl(history.getFileName());
                        dto.setFileDownloadUrl(downloadUrl);
                    } catch (Exception e) {
                        System.err.println("Failed to generate download URL: " + e.getMessage());
                    }
                }

                historyDtos.add(dto);
            }

            return Response.ok(historyDtos).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Ошибка при получении истории импорта")
                    .build();
        }
    }

    @GET
    @Path("/import-history/{id}/file")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadImportFile(@PathParam("id") Long importHistoryId) {
        try {
            ImportHistory history = em.find(ImportHistory.class, importHistoryId);
            if (history == null || !Boolean.TRUE.equals(history.getFileStored())) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String downloadUrl = minioService.getFileUrl(history.getFileName());
            return Response.temporaryRedirect(java.net.URI.create(downloadUrl)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importPersons(@Valid List<PersonDtoImport> personImportDtos,
                                  @QueryParam("login") String login) {
//        EntityTransaction transaction = em.getTransaction();
        ImportHistory importHistory = null;
        try {
            transaction.begin();

            // Получаем пользователя
            User user = em.createQuery("SELECT u FROM User u WHERE u.login = :login", User.class)
                    .setParameter("login", login)
                    .getSingleResult();

            importHistory = new ImportHistory(user, "ERROR", 0, LocalDateTime.now());
            em.persist(importHistory);
            em.flush();
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
        }
        try {
            transaction.begin();
            // Получаем пользователя
            User user = em.createQuery("SELECT u FROM User u WHERE u.login = :login", User.class)
                    .setParameter("login", login)
                    .getSingleResult();

            List<Person> importedPersons = new ArrayList<>();

            for (PersonDtoImport dto : personImportDtos) {
                String name = dto.getName().trim();
                TypedQuery<Long> query = em.createQuery(
                        "SELECT COUNT(p) FROM Person p WHERE p.name = :name", Long.class);
                query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                query.setParameter("name", name.trim());
                Long existingCount = query.getSingleResult();
                if (existingCount > 0) {
                    transaction.rollback();
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Персонаж с именем '" + name + "' уже существует")
                            .build();
                }
                // Создание Coordinates
                Coordinates coordinates = new Coordinates(
                        dto.getCoordinateX(),
                        dto.getCoordinateY(),
                        user,
                        new ArrayList<>()
                );
                em.persist(coordinates);
                // Создание Location
                Location location = new Location(
                        dto.getLocationX(),
                        dto.getLocationY(),
                        dto.getLocationZ(),
                        user,
                        new ArrayList<>()
                );
                em.persist(location);

                // Создание Person
                Person person = new Person(
                        dto.getName(),
                        coordinates,
                        dto.getEyeColor(),
                        dto.getHairColor(),
                        location,
                        dto.getHeight(),
                        dto.getBirthday(),
                        dto.getWeight(),
                        dto.getNationality(),
                        user
                );

                em.persist(person);

                // Обновление связей
                coordinates.getPeople().add(person);
                em.merge(coordinates);

                location.getPeople().add(person);
                em.merge(location);
                importedPersons.add(person);
            }

            importHistory.setStatus("SUCCESS");
            importHistory.setImportedCount(importedPersons.size());
            em.merge(importHistory);
            transaction.commit();
            personWebSocket.onMessage("");

            return Response.ok("Успешно импортировано " + importedPersons.size() + " объектов").build();

        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            if (importHistory != null && importHistory.getId() != null) {
                try {
                    EntityTransaction errorTransaction = em.getTransaction();
                    errorTransaction.begin();
                    ImportHistory errorHistory = em.find(ImportHistory.class, importHistory.getId());
                    if (errorHistory != null) {
                        errorHistory.setStatus("ERROR");
                        em.merge(errorHistory);
                    }
                    errorTransaction.commit();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Ошибка при импорте: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addPerson(@Valid PersonDtoRequest personDtoRequest) {
        try {
            transaction.begin();
            String login = personDtoRequest.getLogin();
            User user = em.createQuery("SELECT u FROM User u WHERE u.login = :login", User.class)
                    .setParameter("login", login)
                    .getSingleResult();

            String name = personDtoRequest.getName().trim();
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(p) FROM Person p WHERE p.name = :name", Long.class);
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
            query.setParameter("name", name.trim());
            Long existingCount = query.getSingleResult();
            if (existingCount > 0) {
                transaction.rollback();
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Персонаж с именем '" + name + "' уже существует")
                        .build();
            }

            Coordinates coordinates;
            Location location;
            if (personDtoRequest.getCoordinatesId().equals(0)) {
                List<Person> personList = new ArrayList<>();
                coordinates = new Coordinates(personDtoRequest.getCoordinateX(), personDtoRequest.getCoordinateY(), user, personList);
                em.persist(coordinates);
                em.flush();
            } else {
                coordinates = em.find(Coordinates.class, personDtoRequest.getCoordinatesId());
            }

            // Обработка локации
            if (personDtoRequest.getLocationId().equals(0)) {
                List<Person> personList = new ArrayList<>();
                location = new Location(personDtoRequest.getLocationX(), personDtoRequest.getLocationY(),
                        personDtoRequest.getLocationZ(), user, personList);
                em.persist(location);
                em.flush();
            } else {
                location = em.find(Location.class, personDtoRequest.getLocationId());
            }

            // Создание Person
            Person person = new Person(
                    personDtoRequest.getName(),
                    coordinates,
                    personDtoRequest.getEyeColor(),
                    personDtoRequest.getHairColor(),
                    location,
                    personDtoRequest.getHeight(),
                    personDtoRequest.getBirthday(),
                    personDtoRequest.getWeight(),
                    personDtoRequest.getNationality(),
                    user
            );

            em.persist(person);
            // Обновление связей
            coordinates.getPeople().add(person);
            em.merge(coordinates);

            location.getPeople().add(person);
            em.merge(location);

            transaction.commit();
            personWebSocket.onMessage("");
            return Response.ok("Person добавлен").build();
        } catch (Exception e) {
            e.printStackTrace();
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage() + "Ошибка при добавлении Person").build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPersons() {
        try {
            transaction.begin();
            List<Person> persons = em.createQuery("SELECT p FROM Person p", Person.class)
                    .getResultList();
            List<PersonDtoResponse> personsResponse = new ArrayList<>();

            for (Person p : persons) {
                Coordinates coordinates = p.getCoordinates();
                Location location = p.getLocation();

                PersonDtoResponse personDtoResponse = new PersonDtoResponse(
                        p.getId(),
                        p.getName(),
                        coordinates.getX(),
                        coordinates.getY(),
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        p.getCreationDate(),
                        p.getEyeColor(),
                        p.getHairColor(),
                        p.getHeight(),
                        p.getBirthday(),
                        p.getWeight(),
                        p.getNationality(),
                        p.getUser().getLogin()
                );
                personsResponse.add(personDtoResponse);
            }
            transaction.commit();
            return Response.ok(personsResponse).build();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при получении persons").build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletePerson(@PathParam("id") Long id) {
        try {
            transaction.begin();
            Person person = em.find(Person.class, id);
            if (person != null) {
                em.remove(person);
            }
            transaction.commit();
            personWebSocket.onMessage("");
            return Response.ok().build();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.serverError().build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePerson(@Valid PersonDtoRequestEdit personDtoRequestEdit) {
        try {
            transaction.begin();
            String login = personDtoRequestEdit.getOwner();
            User user = em.createQuery("SELECT u FROM User u WHERE u.login = :login", User.class)
                    .setParameter("login", login)
                    .getSingleResult();

            Person existingPerson = em.find(Person.class, personDtoRequestEdit.getId());

            if (existingPerson == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Person not found").build();
            }

            // Обновление основных полей
            existingPerson.setName(personDtoRequestEdit.getName());
            existingPerson.setEyeColor(personDtoRequestEdit.getEyeColor());
            existingPerson.setHairColor(personDtoRequestEdit.getHairColor());
            existingPerson.setHeight(personDtoRequestEdit.getHeight());
            existingPerson.setBirthday(personDtoRequestEdit.getBirthday());
            existingPerson.setWeight(personDtoRequestEdit.getWeight());
            existingPerson.setNationality(personDtoRequestEdit.getNationality());

            // Обработка координат
            Coordinates coordinates;
            if (personDtoRequestEdit.getCoordinatesId().equals(0)) {
                List<Person> personList = new ArrayList<>();
                coordinates = new Coordinates(personDtoRequestEdit.getCoordinateX(), personDtoRequestEdit.getCoordinateY(), user, personList);
                em.persist(coordinates);
            } else {
                coordinates = em.find(Coordinates.class, personDtoRequestEdit.getCoordinatesId());
            }

            // Обработка локации
            Location location;
            if (personDtoRequestEdit.getLocationId().equals(0)) {
                List<Person> personList = new ArrayList<>();
                location = new Location(personDtoRequestEdit.getLocationX(), personDtoRequestEdit.getLocationY(),
                        personDtoRequestEdit.getLocationZ(), user, personList);
                em.persist(location);
            } else {
                location = em.find(Location.class, personDtoRequestEdit.getLocationId());
            }

            existingPerson.setCoordinates(coordinates);
            existingPerson.setLocation(location);

            em.merge(existingPerson);
            transaction.commit();
            personWebSocket.onMessage("");
            return Response.ok("Person изменен").build();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Ошибка при обновлении Person").build();
        }
    }

    @GET
    @Path("/getId")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPersonId(@QueryParam("id") Long id) {
        try {
            Person p = em.find(Person.class, id);
            if (p == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Person not found").build();
            }

            Coordinates coordinates = p.getCoordinates();
            Location location = p.getLocation();

            PersonDtoResponse personDtoResponse = new PersonDtoResponse(
                    p.getId(),
                    p.getName(),
                    coordinates.getX(),
                    coordinates.getY(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    p.getCreationDate(),
                    p.getEyeColor(),
                    p.getHairColor(),
                    p.getHeight(),
                    p.getBirthday(),
                    p.getWeight(),
                    p.getNationality(),
                    p.getUser().getLogin()
            );
            return Response.ok(personDtoResponse).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ошибка при получении Person").build();
        }
    }

}