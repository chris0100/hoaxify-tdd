package com.hoaxify;

import com.hoaxify.configuration.AppConfiguration;
import com.hoaxify.file.FileAttachment;
import com.hoaxify.file.FileAttachmentRepository;
import com.hoaxify.user.UserService;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static com.hoaxify.TestUtil.createValidUser;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class FileUploadControllerTest {

    private static final String API_1_0_HOAXES_UPLOAD = "/api/1.0/hoaxes/upload";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AppConfiguration appConfiguration;

    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;

    @Before
    public void init() throws IOException {
        userRepository.deleteAll();
        fileAttachmentRepository.deleteAll();
        testRestTemplate.getRestTemplate().getInterceptors().clear();
        FileUtils.cleanDirectory(new File(appConfiguration.getFullAttachmentsPath()));
    }


    @Test
    public void uploadFile_withImageFromAuthorizedUser_receiveOk(){
        userService.save(createValidUser("user1"));
        authenticate("user1");
        ResponseEntity<Object> response = uploadFile(getRequestEntity(), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    public void uploadFile_withImageFromUnauthorizedUser_receiveUnauthorized(){
        ResponseEntity<Object> response = uploadFile(getRequestEntity(), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }


    @Test
    public void uploadFile_withImageFromAuthorizedUser_receiveFileAttachmentWithDate(){
        userService.save(createValidUser("user1"));
        authenticate("user1");
        ResponseEntity<FileAttachment> response = uploadFile(getRequestEntity(), FileAttachment.class);
        assertThat(Objects.requireNonNull(response.getBody()).getDate()).isNotNull();
    }


    @Test
    public void uploadFile_withImageFromAuthorizedUser_receiveFileAttachmentWithRandomName(){
        userService.save(createValidUser("user1"));
        authenticate("user1");
        ResponseEntity<FileAttachment> response = uploadFile(getRequestEntity(), FileAttachment.class);
        assertThat(Objects.requireNonNull(Objects.requireNonNull(response.getBody()).getName())).isNotNull();
        assertThat(Objects.requireNonNull(response.getBody().getName())).isNotEqualTo("profile.png");
    }


    @Test
    public void uploadFile_withImageFromAuthorizedUser_imageSavedToFolder(){
        userService.save(createValidUser("user1"));
        authenticate("user1");
        ResponseEntity<FileAttachment> response = uploadFile(getRequestEntity(), FileAttachment.class);
        String imagePath = appConfiguration.getFullAttachmentsPath() + "/" + Objects.requireNonNull(response.getBody()).getName();
        File storedImage = new File(imagePath);
        assertThat(storedImage).exists();
    }


    @Test
    public void uploadFile_withImageFromAuthorizedUser_fileAttachmentSavedToDatabase(){
        userService.save(createValidUser("user1"));
        authenticate("user1");
        uploadFile(getRequestEntity(), FileAttachment.class);

        assertThat(fileAttachmentRepository.count()).isEqualTo(1);
    }


    @Test
    public void uploadFile_withImageFromAuthorizedUser_fileAttachmentStoredWithFileType(){
        userService.save(createValidUser("user1"));
        authenticate("user1");
        uploadFile(getRequestEntity(), FileAttachment.class);
        FileAttachment storedFile = fileAttachmentRepository.findAll().get(0);
        assertThat(storedFile.getFileType()).isEqualTo("image/png");
    }



    //************************************************************************************
    //************************ METHODS ***************************************************
    //************************************************************************************

    public <T> ResponseEntity<T> uploadFile(HttpEntity<?> requestEntity, Class<T> responseType){
        return testRestTemplate.exchange(API_1_0_HOAXES_UPLOAD, HttpMethod.POST, requestEntity, responseType);
    }


    private HttpEntity<MultiValueMap<String, Object>> getRequestEntity() {
        ClassPathResource imageResource = new ClassPathResource("profile.png");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", imageResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        return requestEntity;
    }


    //Authenticate
    private void authenticate(String username) {
        testRestTemplate.getRestTemplate()
                .getInterceptors().add(new BasicAuthenticationInterceptor(username, "P4ssword"));
    }
}
