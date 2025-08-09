package com.itcareer.media.controller;

import com.itcareer.media.constant.ItcareerMediaConstant;
import com.itcareer.media.dto.ApiMessageDto;
import com.itcareer.media.dto.UploadFileDto;
import com.itcareer.media.form.DeleteListFileForm;
import com.itcareer.media.form.UploadBase64Form;
import com.itcareer.media.form.UploadFileForm;
import com.itcareer.media.jwt.ItcareerJwt;
import com.itcareer.media.service.OrgMediaApiService;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/file")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class FileController extends ABasicController{
    @Autowired
    OrgMediaApiService orgMediaApiService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces= MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<UploadFileDto> upload(@Valid UploadFileForm uploadFileForm, BindingResult bindingResult) {
        ItcareerJwt jwt = getSessionFromToken();
        if(jwt == null || jwt.getUserKind() == null){
            ApiMessageDto<UploadFileDto> result = new ApiMessageDto<>();
            result.setResult(false);
            result.setMessage("Not valid additional data");
            return result;
        }

        Integer userKind = getSessionFromToken().getUserKind();
        if (!userKind.equals(ItcareerMediaConstant.USER_KIND_ADMIN) &&
                !userKind.equals(ItcareerMediaConstant.USER_KIND_STUDENT) &&
                !userKind.equals(ItcareerMediaConstant.USER_KIND_EDUCATOR)) {

            ApiMessageDto<UploadFileDto> result = new ApiMessageDto<>();
            result.setResult(false);
            result.setMessage("Invalid user kind");
            return result;
        }

        return orgMediaApiService.storeFile(uploadFileForm);
    }

    @PostMapping(value = "/upload-base64", produces= MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<UploadFileDto> uploadForBase64(@Valid @RequestBody UploadBase64Form uploadBase64Form, BindingResult bindingResult) {
        ItcareerJwt jwt = getSessionFromToken();
        if(jwt == null || jwt.getUserKind() == null){
            ApiMessageDto<UploadFileDto> result = new ApiMessageDto<>();
            result.setResult(false);
            result.setMessage("Not valid additional data");
            return result;
        }

        Integer userKind = getSessionFromToken().getUserKind();
        if (!userKind.equals(ItcareerMediaConstant.USER_KIND_ADMIN) &&
                !userKind.equals(ItcareerMediaConstant.USER_KIND_STUDENT) &&
                !userKind.equals(ItcareerMediaConstant.USER_KIND_EDUCATOR)) {

            ApiMessageDto<UploadFileDto> result = new ApiMessageDto<>();
            result.setResult(false);
            result.setMessage("Invalid user kind");
            return result;
        }

        return orgMediaApiService.storeFileByBase64(uploadBase64Form);
    }

    @GetMapping("/download/{folder}/{fileName:.+}")
    @Cacheable("images")
    public ResponseEntity<Resource> downloadFile(@PathVariable String folder,@PathVariable String fileName, HttpServletRequest request) throws FileNotFoundException {
        return getResource(folder,fileName,request);
    }

    private ResponseEntity<Resource> getResource(String folder, String fileName, HttpServletRequest request) {
        Resource resource= orgMediaApiService.loadFileAsResource(folder , fileName);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        if(contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(7776000, TimeUnit.SECONDS))
                .contentType(MediaType.parseMediaType(contentType))
                //.header(HttpHeaders.EXPIRES, expires)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping(value = "/delete-list-file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<String> deleteListFile(@Valid @RequestBody DeleteListFileForm deleteListFileForm, BindingResult bindingResult) {
        ApiMessageDto<String> apiMessageDto = new ApiMessageDto<>();
        for (String filePath : deleteListFileForm.getFiles()) {
            orgMediaApiService.deleteFile(filePath);
        }
        apiMessageDto.setMessage("Delete list file success");
        return apiMessageDto;
    }

    @DeleteMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<String> deleteFile(@RequestParam String filePath){
        ApiMessageDto<String> apiMessageDto = new ApiMessageDto<>();
        try {
            Path path = Paths.get(filePath).normalize();
            if (path.getNameCount() < 2) {
                apiMessageDto.setResult(false);
                apiMessageDto.setMessage("Invalid file path");
                return apiMessageDto;
            }

            String rootFolder = path.getName(0).toString();
            String subPath = path.subpath(1, path.getNameCount()).toString();

            orgMediaApiService.deleteByFilePath(rootFolder, subPath);
            apiMessageDto.setMessage("delete success");
            return apiMessageDto;

        } catch (Exception e) {
            log.error("Error occurred while deleting", e);
            apiMessageDto.setResult(false);
            apiMessageDto.setMessage("Error occurred while deleting");
            return apiMessageDto;
        }
    }
}
