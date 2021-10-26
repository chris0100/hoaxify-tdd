package com.hoaxify.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "hoaxify")
public class AppConfiguration {

    String uploadPath;
    private String profileImagesFolder = "profile";
    private String attachmentFolder = "attachments";

    public String getFullProfileImagePath() {
        return this.uploadPath + "/" + this.profileImagesFolder;
    }

    public String getFullAttachmentsPath() {
        return this.uploadPath + "/" + this.attachmentFolder;
    }


}
