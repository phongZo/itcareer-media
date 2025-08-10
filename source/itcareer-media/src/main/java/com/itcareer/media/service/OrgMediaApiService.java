package com.itcareer.media.service;

import com.itcareer.media.constant.ItcareerMediaConstant;
import com.itcareer.media.dto.ApiMessageDto;
import com.itcareer.media.dto.ErrorCode;
import com.itcareer.media.dto.UploadFileDto;
import com.itcareer.media.exception.BadRequestException;
import com.itcareer.media.form.UploadBase64Form;
import com.itcareer.media.form.UploadFileForm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@Slf4j
public class OrgMediaApiService {
    protected static final String[] UPLOAD_TYPES = new String[]{"LOGO", "AVATAR", "IMAGE", "VIDEO", "DOCUMENT"};
    protected static final String[] AVATAR_EXTENSION = new String[]{"jpeg", "jpg", "gif", "bmp", "png"};

    @Value("${file.upload-dir}")
    private String rootDirectory;

    /**
     * return file path
     * - General/
     *          Video,Media,..
     * - Tenant/
     *          - TenantId
     *                     Video,Media..
     *
     * @param uploadFileForm
     * @return
     */
    public ApiMessageDto<UploadFileDto> storeFile(UploadFileForm uploadFileForm) {
        // Normalize file name
        ApiMessageDto<UploadFileDto> apiMessageDto = new ApiMessageDto<>();
        try {
            boolean contains = Arrays.stream(UPLOAD_TYPES).anyMatch(uploadFileForm.getType()::equalsIgnoreCase);
            if (!contains) {
                apiMessageDto.setResult(false);
                apiMessageDto.setMessage("Type is required in AVATAR or LOGO or IMAGE or VIDEO or DOCUMENT");
                return apiMessageDto;
            }
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(uploadFileForm.getFile().getOriginalFilename()));
            String ext = FilenameUtils.getExtension(fileName);
            boolean extContains = Arrays.stream(AVATAR_EXTENSION).anyMatch(ext::equalsIgnoreCase);
            if ((Objects.equals(uploadFileForm.getType(), "AVATAR")
                    || Objects.equals(uploadFileForm.getType(), "LOGO")
                    || Objects.equals(uploadFileForm.getType(), "IMAGE"))
                    && !extContains) {
                throw new BadRequestException("File format is invalid", ErrorCode.FILE_ERROR_FORMAT_INVALID);
            }
            //upload to uploadFolder/TYPE/id
            String finalFile = (uploadFileForm.getApp()!=null ? uploadFileForm.getApp() +"_" :"")+ uploadFileForm.getType() + "_" + RandomStringUtils.randomAlphanumeric(10) + "." + ext;
            String typeFolder = File.separator + uploadFileForm.getType();
            Path fileStorageLocation;
            String tenantFolder = "";
            fileStorageLocation = Paths.get(rootDirectory + ItcareerMediaConstant.DIRECTORY_GENERAL + typeFolder).toAbsolutePath().normalize();

            Files.createDirectories(fileStorageLocation);
            Path targetLocation = fileStorageLocation.resolve(finalFile);
            Files.copy(uploadFileForm.getFile().getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            UploadFileDto uploadFileDto = new UploadFileDto();
            uploadFileDto.setFilePath(tenantFolder + typeFolder + File.separator + finalFile);
            apiMessageDto.setData(uploadFileDto);
            apiMessageDto.setMessage("Upload file success");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            apiMessageDto.setResult(false);
            apiMessageDto.setMessage(e.getMessage());
        }
        return apiMessageDto;
    }


    public ApiMessageDto<UploadFileDto> storeFileByBase64(UploadBase64Form uploadBase64Form) {
        // Normalize file name
        ApiMessageDto<UploadFileDto> apiMessageDto = new ApiMessageDto<>();
        try {
            boolean contains = Arrays.stream(UPLOAD_TYPES).anyMatch(uploadBase64Form.getType()::equalsIgnoreCase);
            if (!contains) {
                apiMessageDto.setResult(false);
                apiMessageDto.setMessage("Type is required in AVATAR or LOGO");
                return apiMessageDto;
            }
            String ext = "png";
            String finalFile = (uploadBase64Form.getApp()!=null ? uploadBase64Form.getApp() +"_" :"")+ uploadBase64Form.getType() + "_" + RandomStringUtils.randomAlphanumeric(10) + "." + ext;
            //upload to uploadFolder/TYPE/id
            String typeFolder = File.separator + uploadBase64Form.getType();
            Path fileStorageLocation;
            String tenantFolder = "";

            fileStorageLocation = Paths.get(rootDirectory + ItcareerMediaConstant.DIRECTORY_GENERAL + typeFolder).toAbsolutePath().normalize();

            Files.createDirectories(fileStorageLocation);
            convertBase64ToImage(uploadBase64Form.getBase64Image(),fileStorageLocation.toString() + File.separator + finalFile);
            UploadFileDto uploadFileDto = new UploadFileDto();
            uploadFileDto.setFilePath(tenantFolder + typeFolder + File.separator + finalFile);
            apiMessageDto.setData(uploadFileDto);
            apiMessageDto.setMessage("Upload file success");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            apiMessageDto.setResult(false);
            apiMessageDto.setMessage(e.getMessage());
        }
        return apiMessageDto;
    }

    public void convertBase64ToImage(String base64String, String outputPath) throws IOException {
        try {
            // Decode the Base64 string to bytes
            byte[] imageBytes = Base64.getDecoder().decode(base64String);

            // Create a FileOutputStream to write the image to a file
            try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
                FileCopyUtils.copy(imageBytes,outputStream);
                //outputStream.write(imageBytes);
            }
        } catch (IOException e) {
            throw new IOException("Error converting Base64 to image: " + e.getMessage());
        }
    }

    public void deleteFile(String filePath) {
        File file = new File(rootDirectory + ItcareerMediaConstant.DIRECTORY_GENERAL+ filePath);

        log.info("======> file path: {}", file.getAbsolutePath());
        if(file.exists()){
            file.delete();
        }
    }

    public void deleteByFilePath(String rootFolder, String subPath) {
        try {
            String basePath = rootDirectory + ItcareerMediaConstant.DIRECTORY_GENERAL + "/" + rootFolder;
            Path subPathObj = Paths.get(subPath);

            boolean isFolderKind = !subPathObj.getFileName().toString().contains(".");

            if (isFolderKind) {
                String folderName = subPathObj.getName(0).toString();
                File targetFolder = new File(basePath + "/" + folderName);

                log.info("======> Deleting folder: {}", targetFolder.getAbsolutePath());
                if (targetFolder.exists() && targetFolder.isDirectory()) {
                    deleteDirectory(targetFolder.toPath());
                    log.info("======> Folder '{}' deleted successfully", targetFolder.getAbsolutePath());
                } else {
                    log.warn("======> Folder not found or not a directory: {}", targetFolder.getAbsolutePath());
                }
            } else {
                File targetFile = new File(basePath + "/" + subPath);
                log.info("======> Deleting file: {}", targetFile.getAbsolutePath());
                if (targetFile.exists() && targetFile.isFile()) {
                    if (targetFile.delete()) {
                        log.info("======> File '{}' deleted successfully", targetFile.getAbsolutePath());
                    } else {
                        log.warn("======> Failed to delete file: {}", targetFile.getAbsolutePath());
                    }
                } else {
                    log.warn("======> File not found or is not a file: {}", targetFile.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            log.error("======> Error occurred while deleting file/folder, rootFolder={}, subPath={}", rootFolder, subPath, e);
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    public Resource loadFileAsResource(String folder, String fileName) {
        String directory = rootDirectory + ItcareerMediaConstant.DIRECTORY_GENERAL;
        System.out.println("User.home: "+System.getProperty("spring.config.location"));
        System.out.println("get file: "+folder+"/"+fileName+", path: "+directory);
        try {
            Path fileStorageLocation = Paths.get(directory + File.separator + folder).toAbsolutePath().normalize();
            Path fP = fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(fP.toUri());
            if (resource.exists()) {
                return resource;
            }
        } catch (MalformedURLException ex) {
            //log.error(ex.getMessage(), ex);
            System.out.println("Error get file: "+folder+"/"+fileName+", path: "+directory);

        }
        return null;
    }

    public InputStreamResource loadFileAsResourceExt(String folder, String fileName) {
        String directory = rootDirectory + ItcareerMediaConstant.DIRECTORY_GENERAL;
        try {
            File file = new File(directory + File.separator + folder + File.separator + fileName);
            InputStreamResource inputStreamResource = new InputStreamResource(new FileInputStream(file));
            if (inputStreamResource.exists()) {
                return inputStreamResource;
            }
        } catch (FileNotFoundException ex) {
            log.error(ex.getMessage(), ex);

        }
        return null;
    }
}
