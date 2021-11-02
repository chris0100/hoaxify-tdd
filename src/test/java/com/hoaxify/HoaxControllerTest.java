package com.hoaxify;

import com.hoaxify.configuration.AppConfiguration;
import com.hoaxify.error.ApiError;
import com.hoaxify.file.FileAttachmentRepository;
import com.hoaxify.file.FileService;
import com.hoaxify.hoax.Hoax;
import com.hoaxify.hoax.HoaxRepository;
import com.hoaxify.hoax.HoaxService;
import com.hoaxify.hoax.vm.HoaxVM;
import com.hoaxify.user.User;
import com.hoaxify.user.UserService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.hoaxify.TestUtil.createValidHoax;
import static com.hoaxify.TestUtil.createValidUser;
import static org.apache.tomcat.util.http.fileupload.FileUtils.cleanDirectory;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class HoaxControllerTest {

    @Autowired
    TestRestTemplate testRestTemplate;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    private static final String API_1_0_HOAXES = "/api/1.0/hoaxes";

    @Autowired
    private HoaxRepository hoaxRepository;

    @Autowired
    private HoaxService hoaxService;

    @Autowired
    FileAttachmentRepository fileAttachmentRepository;

    @Autowired
    FileService fileService;

    @Autowired
    AppConfiguration appConfiguration;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Before
    public void cleanup() throws IOException {
        fileAttachmentRepository.deleteAll();
        hoaxRepository.deleteAll();
        userRepository.deleteAll();
        testRestTemplate.getRestTemplate().getInterceptors().clear();
        cleanDirectory(new File(appConfiguration.getFullAttachmentsPath()));
    }


    @After
    public void cleanupAfter() {
        hoaxRepository.deleteAll();
    }


    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_receiveOk() {
        userService.save(TestUtil.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = createValidHoax();
        final ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    public void postHoax_whenHoaxIsValidAndUserIsUnauthorized_receiveUnauthorized() {
        Hoax hoax = createValidHoax();
        final ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }


    @Test
    public void postHoax_whenHoaxIsValidAndUserIsUnauthorized_receiveApiError() {
        Hoax hoax = createValidHoax();
        final ResponseEntity<ApiError> response = postHoax(hoax, ApiError.class);
        assertThat(Objects.requireNonNull(response.getBody()).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }


    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSavedToDatabase() {
        userService.save(TestUtil.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = createValidHoax();
        postHoax(hoax, Object.class);
        assertThat(hoaxRepository.count()).isEqualTo(1);
    }


    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSavedToDatabaseWithTimestamp() {
        userService.save(TestUtil.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = createValidHoax();
        postHoax(hoax, Object.class);

        Hoax inDB = hoaxRepository.findAll().get(0);
        assertThat(inDB.getTimestamp()).isNotNull();
    }


    @Test
    public void postHoax_whenHoaxContentNullAndUserIsAuthorized_receiveBadRequest() {
        userService.save(TestUtil.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = new Hoax();
        final ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    public void postHoax_whenHoaxContentLessThan10CharactersAndUserIsAuthorized_receiveBadRequest() {
        userService.save(TestUtil.createValidUser("user1"));
        authenticate("user1");

        Hoax hoax = new Hoax();
        hoax.setContent("123456789");

        final ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    public void postHoax_whenHoaxContentIs5000CharactersAndUserIsAuthorized_receiveOk() {
        userService.save(TestUtil.createValidUser("user1"));
        authenticate("user1");

        Hoax hoax = new Hoax();
        final String veryLongString = IntStream.rangeClosed(1, 5000).mapToObj(i -> "x").collect(Collectors.joining());
        hoax.setContent(veryLongString);

        final ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    public void postHoax_whenHoaxContentIsMoreThan5000CharactersAndUserIsAuthorized_receiveBadRequest() {
        userService.save(TestUtil.createValidUser("user1"));
        authenticate("user1");

        Hoax hoax = new Hoax();
        final String veryLongString = IntStream.rangeClosed(1, 5001).mapToObj(i -> "x").collect(Collectors.joining());
        hoax.setContent(veryLongString);

        final ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    public void postHoax_whenHoaxContentNullAndUserIsAuthorized_receiveApiErrorWithValidationErrors() {
        userService.save(TestUtil.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = new Hoax();
        final ResponseEntity<ApiError> response = postHoax(hoax, ApiError.class);

        Map<String, String> validationErrors = Objects.requireNonNull(response.getBody()).getValidationErrors();
        assertThat(validationErrors.get("content")).isNotNull();
    }


    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSavedWithAuthenticatedUserInfo() {
        userService.save(TestUtil.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = createValidHoax();
        postHoax(hoax, Object.class);

        Hoax inDB = hoaxRepository.findAll().get(0);
        assertThat(inDB.getUser().getUsername()).isEqualTo("user1");
    }


    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxCanBeAccessedFromUserEntity() {
        final User user = userService.save(TestUtil.createValidUser("user1"));

        authenticate("user1");
        Hoax hoax = createValidHoax();
        postHoax(hoax, Object.class);

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        User inDBUser = entityManager.find(User.class, user.getId());
        assertThat(inDBUser.getHoaxes().size()).isEqualTo(1);
    }


    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receiveOk() {
        ResponseEntity<Object> response = getHoaxes(new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receivePageWithZeroItems() {
        ResponseEntity<TestPage<Object>> response = getHoaxes(new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isZero();
    }


    @Test
    public void getHoaxes_whenThereAreHoaxes_receivePageWithItems() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        ResponseEntity<TestPage<Object>> response = getHoaxes(new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(3);
    }


    @Test
    public void getHoaxes_whenThereAreHoaxes_receivePageWithHoaxVM() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());

        ResponseEntity<TestPage<HoaxVM>> response = getHoaxes(new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        HoaxVM storedHoax = Objects.requireNonNull(response.getBody()).getContent().get(0);
        assertThat(storedHoax.getUser().getUsername()).isEqualTo("user1");
    }


    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_receiveHoaxVM() {
        userService.save(TestUtil.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = createValidHoax();
        final ResponseEntity<HoaxVM> response = postHoax(hoax, HoaxVM.class);
        assertThat(Objects.requireNonNull(response.getBody()).getUser().getUsername()).isEqualTo("user1");
    }


    @Test
    public void getHoaxesOfUser_whenUserExists_receiveOk() {
        userService.save(createValidUser("user1"));
        final ResponseEntity<Object> response = getHoaxesOfUser("user1", new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    public void getHoaxesOfUser_whenUserExists_receiveNotFound() {
        final ResponseEntity<Object> response = getHoaxesOfUser("unknown-user", new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Test
    public void getHoaxesOfUser_whenUserExists_receivePageWithZeroHoaxes() {
        userService.save(createValidUser("user1"));
        final ResponseEntity<TestPage<Object>> response = getHoaxesOfUser("user1", new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isZero();
    }


    @Test
    public void getHoaxesOfUser_whenUserExistWithHoax_receivePageWithHoaxVM() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());

        ResponseEntity<TestPage<HoaxVM>> response = getHoaxesOfUser("user1", new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        HoaxVM storedHoax = Objects.requireNonNull(response.getBody()).getContent().get(0);
        assertThat(storedHoax.getUser().getUsername()).isEqualTo("user1");
    }


    @Test
    public void getHoaxesOfUser_whenUserExistWithMultipleHoaxes_receivePageWithMatchingHoaxesCount() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());


        ResponseEntity<TestPage<HoaxVM>> response = getHoaxesOfUser("user1", new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(3);
    }


    @Test
    public void getHoaxesOfUser_whenMultipleUserExistWithMultipleHoaxes_receivePageWithMatchingHoaxesCount() {
        User userWithThreeHoaxes = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(userWithThreeHoaxes, createValidHoax()));

        User userWithFiveHoaxes = userService.save(createValidUser("user2"));
        IntStream.rangeClosed(1, 5).forEach(i -> hoaxService.save(userWithFiveHoaxes, createValidHoax()));

        ResponseEntity<TestPage<HoaxVM>> response = getHoaxesOfUser(userWithFiveHoaxes.getUsername(),
                new ParameterizedTypeReference<TestPage<HoaxVM>>() {
                });

        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(5);
    }


    @Test
    public void getOldHoaxes_whenThereAreNoHoaxes_receiveOk() {
        final ResponseEntity<Object> response = getOldHoaxes(5, new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    public void getOldHoaxes_whenThereAreHoaxes_receivePageWithItemsProvidedId() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        final Hoax fourth = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        final ResponseEntity<TestPage<Object>> response = getOldHoaxes(fourth.getId(), new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(3);
    }


    @Test
    public void getOldHoaxes_whenThereAreHoaxes_receivePageWithHoaxVMBeforeProvidedId() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        final Hoax fourth = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        final ResponseEntity<TestPage<HoaxVM>> response = getOldHoaxes(fourth.getId(), new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getContent().get(0).getDate()).isPositive();
    }


    @Test
    public void getOldHoaxesOfUser_whenUserExistThereAreNoHoaxes_receiveOk() {
        userService.save(createValidUser("user1"));
        final ResponseEntity<Object> response = getOldHoaxesOfUser(5, "user1", new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    public void getOldHoaxesOfUser_whenUserExistAndThereAreHoaxes_receivePageWithItemsProvidedId() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        final Hoax fourth = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        final ResponseEntity<TestPage<Object>> response = getOldHoaxesOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(3);
    }



    @Test
    public void getOldHoaxesOfUser_whenUserExistAndThereAreHoaxes_receivePageWithHoaxVMBeforeProvidedId() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        final Hoax fourth = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        final ResponseEntity<TestPage<HoaxVM>> response = getOldHoaxesOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getContent().get(0).getDate()).isPositive();
    }


    @Test
    public void getOldHoaxesOfUser_whenUserDoesNotExistThereAreNoHoaxes_receiveNotFound() {
        final ResponseEntity<Object> response = getOldHoaxesOfUser(5, "user1", new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Test
    public void getOldHoaxesOfUser_whenUserExistAndThereAreNoHoaxes_receivePageWithZeroItemsBeforeProvidedId() {
        final User user2 = userService.save(createValidUser("user2"));

        final ResponseEntity<TestPage<HoaxVM>> response = getOldHoaxesOfUser(user2.getId(), "user2", new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isZero();
    }


    @Test
    public void getNewHoaxes_whenThereAreHoaxes_receiveListOfItemsAfterProvidedId() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        final Hoax fourth = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        final ResponseEntity<List<Object>> response = getNewHoaxes(fourth.getId(), new ParameterizedTypeReference<List<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).size()).isEqualTo(1);
    }



    @Test
    public void getNewHoaxes_whenThereAreHoaxes_receiveListOfHoaxVMAfterProvidedId() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        final Hoax fourth = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        final ResponseEntity<List<HoaxVM>> response = getNewHoaxes(fourth.getId(), new ParameterizedTypeReference<List<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).get(0).getDate()).isPositive();
    }



    @Test
    public void getNewHoaxesOfUser_whenUserExistThereAreNoHoaxes_receiveOk() {
        userService.save(createValidUser("user1"));
        final ResponseEntity<Object> response = getNewHoaxesOfUser(5, "user1", new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    public void getNewHoaxesOfUser_whenUserExistAndThereAreHoaxes_receiveListWithItemsAfterProvidedId() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        final Hoax fourth = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        final ResponseEntity<List<Object>> response = getNewHoaxesOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<List<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).size()).isEqualTo(1);
    }



    @Test
    public void getNewHoaxesOfUser_whenUserExistAndThereAreHoaxes_receiveListWithHoaxVMAfterProvidedId() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        final Hoax fourth = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        final ResponseEntity<List<HoaxVM>> response = getNewHoaxesOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<List<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).get(0).getDate()).isPositive();
    }


    @Test
    public void getNewHoaxesOfUser_whenUserDoesNotExistThereAreNoHoaxes_receiveNotFound() {
        final ResponseEntity<Object> response = getNewHoaxesOfUser(5, "user1", new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Test
    public void getNewHoaxesOfUser_whenUserExistAndThereAreNoHoaxes_receivePageWithZeroItemsAfterProvidedId() {
        final User user2 = userService.save(createValidUser("user2"));

        final ResponseEntity<List<HoaxVM>> response = getNewHoaxesOfUser(user2.getId(), "user2", new ParameterizedTypeReference<List<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).size()).isZero();
    }


    @Test
    public void getNewHoaxCount_whenThereAreHoaxes_receiveCountAfterProvidedId() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        final Hoax fourth = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        final ResponseEntity<Map<String, Long>> response = getNewHoaxCount(fourth.getId(), new ParameterizedTypeReference<Map<String, Long>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).get("count")).isEqualTo(1);
    }


    @Test
    public void getNewHoaxCountOfUser_whenThereAreHoaxes_receiveCountAfterProvidedId() {
        User user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());
        final Hoax fourth = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        final ResponseEntity<Map<String, Long>> response = getNewHoaxCountOfUser(fourth.getId(), "user1",
                new ParameterizedTypeReference<Map<String, Long>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).get("count")).isEqualTo(1);
    }





    //************************************************************************************
    //************************ METHODS ***************************************************
    //************************************************************************************
    public <T> ResponseEntity<T> getNewHoaxCountOfUser(long hoaxId, String username, ParameterizedTypeReference<T> responseType) {
        String path = "/api/1.0/users/" + username + "/hoaxes/" + hoaxId + "?direction=after&count=true";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    public <T> ResponseEntity<T> getNewHoaxCount(long hoaxId, ParameterizedTypeReference<T> responseType) {
        String path = API_1_0_HOAXES + "/" + hoaxId + "?direction=after&count=true";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }


    public <T> ResponseEntity<T> getNewHoaxesOfUser(long hoaxId, String username, ParameterizedTypeReference<T> responseType) {
        String path = "/api/1.0/users/" + username + "/hoaxes/" + hoaxId + "?direction=after&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }


    public <T> ResponseEntity<T> getNewHoaxes(long hoaxId, ParameterizedTypeReference<T> responseType) {
        String path = API_1_0_HOAXES + "/" + hoaxId + "?direction=after&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    public <T> ResponseEntity<T> getOldHoaxesOfUser(long hoaxId, String username, ParameterizedTypeReference<T> responseType) {
        String path = "/api/1.0/users/" + username + "/hoaxes/" + hoaxId + "?direction=before&page=0&size=5&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }


    public <T> ResponseEntity<T> getOldHoaxes(long hoaxId, ParameterizedTypeReference<T> responseType) {
        String path = API_1_0_HOAXES + "/" + hoaxId + "?direction=before&page=0&size=5&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }


    public <T> ResponseEntity<T> getHoaxesOfUser(String username, ParameterizedTypeReference<T> responseType) {
        String path = "/api/1.0/users/" + username + "/hoaxes";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    public <T> ResponseEntity<T> getHoaxes(ParameterizedTypeReference<T> responseType) {
        return testRestTemplate.exchange(API_1_0_HOAXES, HttpMethod.GET, null, responseType);
    }


    private <T> ResponseEntity<T> postHoax(Hoax hoax, Class<T> responseType) {
        return testRestTemplate.postForEntity(API_1_0_HOAXES, hoax, responseType);
    }


    //Authenticate
    private void authenticate(String username) {
        testRestTemplate.getRestTemplate()
                .getInterceptors().add(new BasicAuthenticationInterceptor(username, "P4ssword"));
    }


    //************************************************************************************
    //************************ METHODS ***************************************************
    //************************************************************************************

}
