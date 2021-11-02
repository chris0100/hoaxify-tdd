package com.hoaxify.file;

import com.hoaxify.configuration.AppConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class FileService {

    AppConfiguration appConfiguration;

    Tika tika;

    FileAttachmentRepository fileAttachmentRepository;

    public FileService(AppConfiguration appConfiguration, FileAttachmentRepository fileAttachmentRepository) {
        this.appConfiguration = appConfiguration;
        tika = new Tika();
        this.fileAttachmentRepository = fileAttachmentRepository;
    }

    public String saveProfileImage(String base64Image) throws IOException {
        String imageName = getRandomName();
        byte[] decodeBytes = Base64.getDecoder().decode(base64Image);

        File target = new File(appConfiguration.getFullProfileImagePath() + "/" + imageName);
        FileUtils.writeByteArrayToFile(target, decodeBytes);
        return imageName;
    }

    public String detectType(byte[] fileArr) {
        return tika.detect(fileArr);
    }

    public void deleteProfileImage(String image) {
        try {
            Files.deleteIfExists(Paths.get(appConfiguration.getFullProfileImagePath() + "/" + image));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileAttachment saveAttachment(MultipartFile file) {
        FileAttachment fileAttachment = new FileAttachment();
        fileAttachment.setDate(new Date());
        String randomName = getRandomName();
        fileAttachment.setName(randomName);

        File target = new File(appConfiguration.getFullAttachmentsPath() + "/" + randomName);

        try {
            byte[] fileAsByte = file.getBytes();
            FileUtils.writeByteArrayToFile(target, fileAsByte);
            fileAttachment.setFileType(detectType(fileAsByte));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileAttachmentRepository.save(fileAttachment);
    }

    private String getRandomName() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
