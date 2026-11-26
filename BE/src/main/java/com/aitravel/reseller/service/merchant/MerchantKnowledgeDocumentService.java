package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.respone.merchant.KnowledgeDocumentDtos;
import com.aitravel.reseller.entity.KnowledgeDocument;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.Product;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.KnowledgeDocumentRepository;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.ProductRepository;
import com.aitravel.reseller.repository.merchant.KnowledgeDocumentSearchRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipFile;

@Service
@RequiredArgsConstructor
public class MerchantKnowledgeDocumentService {
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final String FILE_PREFIX = "KBFILE_V1";

    private static final String PDF_MEDIA_TYPE = "application/pdf";
    private static final String DOCX_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final Set<String> STATUSES =
            Set.of("ACTIVE", "PROCESSING", "FAILED", "ARCHIVED");

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeDocumentSearchRepository searchRepository;
    private final MerchantRepository merchantRepository;
    private final ProductRepository productRepository;

    @Transactional
    public KnowledgeDocumentDtos.PageResponse list(
            int page,
            int size,
            String search,
            String status,
            UUID productId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validatePage(page, size);

        String normalizedSearch = search == null ? "" : search.trim();
        if (normalizedSearch.length() > 200) {
            throw new IllegalArgumentException(
                    "search must not exceed 200 characters"
            );
        }

        String normalizedStatus = normalizeStatus(status);
        if (!normalizedStatus.isEmpty()
                && !STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Invalid knowledge document status"
            );
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<KnowledgeDocumentDtos.ListItem> result =
                searchRepository.searchForMerchant(
                        merchant.getId(),
                        normalizedSearch,
                        normalizedStatus,
                        productId,
                        pageable
                );

        return new KnowledgeDocumentDtos.PageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentDtos.Detail get(
            UUID documentId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        KnowledgeDocument document =
                findOwnedDocument(merchant.getId(), documentId);

        return toDetail(document);
    }

    @Transactional
    public KnowledgeDocumentDtos.Detail upload(
            MultipartFile file,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        UploadedFile uploadedFile = readAndValidateFile(file);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setMerchant(merchant);
        document.setTitle(uploadedFile.originalFileName());
        document.setContent(encodeFile(uploadedFile));
        document.setStatus("ACTIVE");

        return toDetail(documentRepository.save(document));
    }

    @Transactional
    public KnowledgeDocumentDtos.Detail replaceFile(
            UUID documentId,
            MultipartFile file,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        KnowledgeDocument document =
                findOwnedDocument(merchant.getId(), documentId);

        ensureNotArchived(document);

        UploadedFile uploadedFile = readAndValidateFile(file);
        document.setContent(encodeFile(uploadedFile));
        document.setStatus("ACTIVE");

        return toDetail(documentRepository.save(document));
    }

    @Transactional
    public KnowledgeDocumentDtos.Detail update(
            UUID documentId,
            KnowledgeDocumentDtos.UpdateRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        KnowledgeDocument document =
                findOwnedDocument(merchant.getId(), documentId);

        ensureNotArchived(document);

        if (request == null
                || request.title() == null
                || request.title().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }

        String title = request.title().trim();
        if (title.length() > 255) {
            throw new IllegalArgumentException(
                    "title must not exceed 255 characters"
            );
        }

        Product product = null;
        if (request.productId() != null) {
            product = productRepository
                    .findByMerchantIdAndId(
                            merchant.getId(),
                            request.productId()
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found"
                    ));
        }

        document.setTitle(title);
        document.setProduct(product);

        return toDetail(documentRepository.save(document));
    }

    @Transactional
    public KnowledgeDocumentDtos.Detail archive(
            UUID documentId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        KnowledgeDocument document =
                findOwnedDocument(merchant.getId(), documentId);

        document.setStatus("ARCHIVED");
        return toDetail(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public DownloadedFile download(
            UUID documentId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        KnowledgeDocument document =
                findOwnedDocument(merchant.getId(), documentId);

        StoredFile storedFile = decodeStoredFile(document.getContent());
        if (storedFile == null) {
            throw new ResourceNotFoundException(
                    "This knowledge document has no uploaded file"
            );
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(storedFile.base64Data());
            return new DownloadedFile(
                    storedFile.originalFileName(),
                    storedFile.mediaType(),
                    bytes
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Stored knowledge document file is invalid",
                    e
            );
        }
    }

    private UploadedFile readAndValidateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File size must not exceed 10 MB"
            );
        }

        String originalFileName = sanitizeFileName(
                file.getOriginalFilename()
        );
        String lowerName = originalFileName.toLowerCase(Locale.ROOT);

        try {
            byte[] bytes = file.getBytes();

            if (bytes.length == 0) {
                throw new IllegalArgumentException("file is empty");
            }

            if (bytes.length > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException(
                        "File size must not exceed 10 MB"
                );
            }

            if (lowerName.endsWith(".pdf") && isPdf(bytes)) {
                return new UploadedFile(
                        originalFileName,
                        PDF_MEDIA_TYPE,
                        bytes
                );
            }

            if (lowerName.endsWith(".docx") && isDocx(bytes)) {
                return new UploadedFile(
                        originalFileName,
                        DOCX_MEDIA_TYPE,
                        bytes
                );
            }

            throw new IllegalArgumentException(
                    "Only valid PDF and Word .docx files are supported"
            );
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Could not read uploaded file",
                    e
            );
        }
    }

    private boolean isPdf(byte[] bytes) {
        if (bytes.length < 5) {
            return false;
        }

        return bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F'
                && bytes[4] == '-';
    }

    private boolean isDocx(byte[] bytes) {
        if (bytes.length < 4
                || bytes[0] != 'P'
                || bytes[1] != 'K') {
            return false;
        }

        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(
                    "knowledge-document-",
                    ".docx"
            );
            Files.write(temporaryFile, bytes);

            try (ZipFile zipFile = new ZipFile(temporaryFile.toFile())) {
                return zipFile.getEntry("[Content_Types].xml") != null
                        && zipFile.getEntry("word/document.xml") != null;
            }
        } catch (IOException e) {
            return false;
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // The operating system can clean up a leftover temp file.
                }
            }
        }
    }

    private String encodeFile(UploadedFile file) {
        String encodedName = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        file.originalFileName()
                                .getBytes(StandardCharsets.UTF_8)
                );

        String encodedBytes = Base64.getEncoder()
                .encodeToString(file.bytes());

        return FILE_PREFIX
                + "|" + file.mediaType()
                + "|" + encodedName
                + "|" + file.bytes().length
                + "|" + encodedBytes;
    }

    private StoredFile decodeStoredFile(String content) {
        if (content == null || !content.startsWith(FILE_PREFIX + "|")) {
            return null;
        }

        String[] parts = content.split("\\|", 5);
        if (parts.length != 5 || !FILE_PREFIX.equals(parts[0])) {
            throw new IllegalStateException(
                    "Stored knowledge document file format is invalid"
            );
        }

        try {
            String fileName = new String(
                    Base64.getUrlDecoder().decode(parts[2]),
                    StandardCharsets.UTF_8
            );

            long fileSize = Long.parseLong(parts[3]);
            if (fileSize < 0) {
                throw new IllegalStateException(
                        "Stored knowledge document file size is invalid"
                );
            }

            return new StoredFile(
                    parts[1],
                    fileName,
                    fileSize,
                    parts[4]
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Stored knowledge document metadata is invalid",
                    e
            );
        }
    }

    private KnowledgeDocumentDtos.Detail toDetail(
            KnowledgeDocument document
    ) {
        StoredFile storedFile = decodeStoredFile(document.getContent());

        UUID productId = document.getProduct() == null
                ? null
                : document.getProduct().getId();

        return new KnowledgeDocumentDtos.Detail(
                document.getId(),
                productId,
                document.getTitle(),
                document.getStatus(),
                storedFile != null,
                storedFile == null ? null : storedFile.originalFileName(),
                storedFile == null ? null : storedFile.mediaType(),
                storedFile == null ? null : storedFile.fileSize(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(
                    "Uploaded file must have a file name"
            );
        }

        // Handle both Windows and Unix path separators.
        String normalized = fileName.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        String baseName = lastSlash >= 0
                ? normalized.substring(lastSlash + 1)
                : normalized;

        baseName = baseName
                .replaceAll("[\\p{Cntrl}]", "_")
                .trim();

        if (baseName.isBlank() || baseName.length() > 255) {
            throw new IllegalArgumentException(
                    "Invalid file name; maximum length is 255 characters"
            );
        }

        return baseName;
    }

    private KnowledgeDocument findOwnedDocument(
            UUID merchantId,
            UUID documentId
    ) {
        return documentRepository.findByMerchantIdAndId(
                        merchantId,
                        documentId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Knowledge document not found"
                ));
    }

    private Merchant requireActiveMerchant(CustomUserDetails user) {
        if (user == null) {
            throw new AccessDeniedException("Merchant account is required");
        }

        Merchant merchant = merchantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "This user is not linked to a merchant"
                ));

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new AccessDeniedException(
                    "Merchant account is not active"
            );
        }

        return merchant;
    }

    private void ensureNotArchived(KnowledgeDocument document) {
        if ("ARCHIVED".equals(document.getStatus())) {
            throw new IllegalStateException(
                    "Archived knowledge documents cannot be edited"
            );
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be zero or greater"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100"
            );
        }
    }

    private String normalizeStatus(String status) {
        return status == null
                ? ""
                : status.trim().toUpperCase(Locale.ROOT);
    }

    private record UploadedFile(
            String originalFileName,
            String mediaType,
            byte[] bytes
    ) {}

    private record StoredFile(
            String mediaType,
            String originalFileName,
            long fileSize,
            String base64Data
    ) {}

    public record DownloadedFile(
            String originalFileName,
            String mediaType,
            byte[] bytes
    ) {}
}