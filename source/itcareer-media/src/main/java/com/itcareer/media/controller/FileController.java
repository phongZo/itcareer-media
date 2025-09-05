package com.itcareer.media.controller;

import com.itcareer.media.constant.ItcareerMediaConstant;
import com.itcareer.media.dto.ApiMessageDto;
import com.itcareer.media.dto.UploadFileDto;
import com.itcareer.media.form.DeleteListFileForm;
import com.itcareer.media.form.UploadBase64Form;
import com.itcareer.media.form.UploadCertificateForm;
import com.itcareer.media.form.UploadFileForm;
import com.itcareer.media.jwt.ItcareerJwt;
import com.itcareer.media.service.CertificateService;
import com.itcareer.media.service.OrgMediaApiService;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.ExternalDigest;
import com.itextpdf.text.pdf.security.ExternalSignature;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.itextpdf.text.pdf.security.PdfPKCS7;
import com.itextpdf.text.pdf.security.PrivateKeySignature;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/file")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class FileController extends ABasicController{
    @Autowired
    OrgMediaApiService orgMediaApiService;
    @Autowired
    CertificateService certificateService;

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
        uploadFileForm.setAccountId(getSessionFromToken().getAccountId());
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

    @GetMapping("/download/{folder}/{subFolder}/{fileName:.+}")
    @Cacheable("images")
    public ResponseEntity<Resource> downloadFile(@PathVariable String folder, @PathVariable String subFolder, @PathVariable String fileName, HttpServletRequest request) throws FileNotFoundException {
        return getResource(folder,subFolder,fileName,request);
    }

    private ResponseEntity<Resource> getResource(String folder, String subFolder, String fileName, HttpServletRequest request) {
        Resource resource= orgMediaApiService.loadFileAsResource(folder, subFolder , fileName);
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
        Path path = Paths.get(filePath).normalize();
        if (path.getNameCount() < 2) {
            apiMessageDto.setResult(false);
            apiMessageDto.setMessage("Invalid file path");
            return apiMessageDto;
        }

        String rootFolder = path.getName(0).toString();
        String subPath = path.subpath(1, path.getNameCount()).toString();

        orgMediaApiService.deleteByFilePath(rootFolder, subPath);
        apiMessageDto.setMessage("Delete success");
        return apiMessageDto;
    }

    @PostMapping(value = "/upload-certificate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<UploadFileDto> uploadCertificate(@Valid @RequestBody UploadCertificateForm form, BindingResult bindingResult) {
        ApiMessageDto<UploadFileDto> result = new ApiMessageDto<>();
        ItcareerJwt jwt = getSessionFromToken();
        if(jwt == null || jwt.getUserKind() == null){
            result.setResult(false);
            result.setMessage("Not valid additional data");
            return result;
        }

        Integer userKind = getSessionFromToken().getUserKind();
        if (!userKind.equals(ItcareerMediaConstant.USER_KIND_ADMIN) &&
            !userKind.equals(ItcareerMediaConstant.USER_KIND_STUDENT) &&
            !userKind.equals(ItcareerMediaConstant.USER_KIND_EDUCATOR)) {

            result.setResult(false);
            result.setMessage("Invalid user kind");
            return result;
        }

        PdfReader reader = null;
        PdfStamper stamper = null;
        Path tmpFont = null;
        ByteArrayOutputStream outputArray = null;

        try {
            // Load PDF template từ resources/files/
            ClassPathResource pdfRes = new ClassPathResource("files/certificate.pdf");
            InputStream pdfIn = pdfRes.getInputStream();
            reader = new PdfReader(pdfIn);

            // Chuẩn bị output stream
            outputArray = new ByteArrayOutputStream();

            // Tạo PdfStamper (có thể ném DocumentException)
            stamper = new PdfStamper(reader, outputArray);

            // Lấy canvas page 1 (nếu template ở trang khác, đổi chỉ số)
            PdfContentByte canvas = stamper.getOverContent(1);

            // Copy font Unicode (TTF) từ resources/fonts -> file tạm (BaseFont.createFont cần đường dẫn file)
            // Đảm bảo bạn có file TTF unicode (ví dụ Quintessential-Regular.ttf) tại src/main/resources/fonts/
            ClassPathResource fontRes = new ClassPathResource("fonts/Quintessential-Regular.ttf");
            tmpFont = Files.createTempFile("tmp-font-", ".ttf");
            try (InputStream fis = fontRes.getInputStream()) {
                Files.copy(fis, tmpFont, StandardCopyOption.REPLACE_EXISTING);
            }

            // Load font với IDENTITY_H để hỗ trợ Unicode (tiếng Việt)
            BaseFont bf = BaseFont.createFont(tmpFont.toAbsolutePath().toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

            // Che placeholder Username
            canvas.setColorFill(BaseColor.WHITE);
            canvas.rectangle(880, 570, 200, 49); // toạ độ và kích thước khớp chỗ [ Username ]
            canvas.fill();

            // Viết text Username
            canvas.beginText();
            canvas.setFontAndSize(bf, 32);
            canvas.setColorFill(BaseColor.BLACK);
            canvas.showTextAligned(PdfContentByte.ALIGN_CENTER, form.getUsername(), 970, 580, 0);
            canvas.endText();

            // Che placeholder Simulation Name
            canvas.setColorFill(BaseColor.WHITE);
            canvas.rectangle(880, 482, 170, 31); // toạ độ khớp chỗ [ Simulation Name ]
            canvas.fill();

            // Viết text Simulation Name
            canvas.beginText();
            canvas.setFontAndSize(bf, 20);
            canvas.setColorFill(BaseColor.BLACK);
            canvas.showTextAligned(PdfContentByte.ALIGN_CENTER, form.getSimulationName(), 970, 485, 0);
            canvas.endText();

            // Đóng stamper để ghi nội dung vào outputArray
            // (gọi close() ở đây an toàn, và set stamper = null để tránh đóng lại trong finally)
            stamper.close();
            stamper = null;

            // 10) Tạo MultipartFile từ bytes và gọi service upload
            // Ký PDF
            byte[] unsignedPdf = outputArray.toByteArray();
            ByteArrayOutputStream signedOut = new ByteArrayOutputStream();

            PrivateKey pk = certificateService.getPrivateKey();
            Certificate[] chain = certificateService.getCertificateChain();

            // Đăng ký provider BouncyCastle (hỗ trợ thuật toán ký số)
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            // Tạo PDF để ký, thêm một số thông tin để ký
            PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(unsignedPdf));
            PdfStamper signer = PdfStamper.createSignature(pdfReader, signedOut, '\0');
            PdfSignatureAppearance appearance = signer.getSignatureAppearance();
            appearance.setReason("Issued by ITDream");
            appearance.setLocation("ITDream");

            // Thực hiện dùng thuật toán SHA256 và private key để ký
            ExternalDigest digest = new BouncyCastleDigest();
            ExternalSignature signature = new PrivateKeySignature(pk, DigestAlgorithms.SHA256, "BC");
            MakeSignature.signDetached(appearance, digest, signature, chain, null, null, null, 0, MakeSignature.CryptoStandard.CMS);

            MultipartFile multipartFile = new ByteArrayMultipartFile(signedOut.toByteArray(), "certificate.pdf", "application/pdf");

            UploadFileForm uploadFileForm = new UploadFileForm();
            uploadFileForm.setType("DOCUMENT");
            uploadFileForm.setFile(multipartFile);
            uploadFileForm.setAccountId(getSessionFromToken().getAccountId());

            result = orgMediaApiService.storeFile(uploadFileForm);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stamper != null) {
                try { stamper.close(); } catch (Exception ignore) {}
            }
            if (reader != null) {
                try { reader.close(); } catch (Exception ignore) {}
            }
            if (outputArray != null) {
                try { outputArray.close(); } catch (Exception ignore) {}
            }
            if (tmpFont != null) {
                try { Files.deleteIfExists(tmpFont); } catch (IOException ignore) {}
            }
        }
        return result;
    }

    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<Boolean> verifyCertificate(@RequestParam("file") MultipartFile file) {
        ApiMessageDto<Boolean> result = new ApiMessageDto<>();
        ItcareerJwt jwt = getSessionFromToken();
        if(jwt == null || jwt.getUserKind() == null){
            result.setResult(false);
            result.setMessage("Not valid additional data");
            return result;
        }

        Integer userKind = getSessionFromToken().getUserKind();
        if (!userKind.equals(ItcareerMediaConstant.USER_KIND_ADMIN) &&
            !userKind.equals(ItcareerMediaConstant.USER_KIND_STUDENT) &&
            !userKind.equals(ItcareerMediaConstant.USER_KIND_EDUCATOR)) {

            result.setResult(false);
            result.setMessage("Invalid user kind");
            return result;
        }

        try {
            // Đọc chữ ký trong file PDF
            Certificate trustedCert = certificateService.getCertificate();

            PdfReader reader = new PdfReader(file.getInputStream());
            AcroFields af = reader.getAcroFields();
            List<String> names = af.getSignatureNames();

            // Nếu PDF không có chữ ký
            if (names.isEmpty()) {
                result.setResult(false);
                result.setMessage("No signature found in certificate");
                return result;
            }

            boolean valid = false;
            // Kiểm tra file PDF có chữ ký có hợp lệ hay không
            for (String name : names) {
                PdfPKCS7 pkcs7 = af.verifySignature(name);
                valid = pkcs7.verify();
                if (valid) {
                    X509Certificate signerCert = pkcs7.getSigningCertificate();
                    valid = signerCert.equals(trustedCert);
                    break;
                }
            }

            result.setResult(valid);
            result.setMessage(valid ? "Certificate is valid" : "Certificate is invalid");

        } catch (Exception e) {
            e.printStackTrace();
            result.setResult(false);
            result.setMessage("Error verifying certificate");
            result.setData(false);
        }
        return result;
    }

    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String originalFilename;
        private final String contentType;

        public ByteArrayMultipartFile(byte[] content, String originalFilename, String contentType) {
            this.content = content != null ? content : new byte[0];
            this.originalFilename = originalFilename;
            this.contentType = contentType;
        }

        @Override public String getName() { return originalFilename; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException { Files.write(dest.toPath(), content); }
    }
}
