package com.PaperPilot.service;

import com.PaperPilot.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ParseDocumentService {

    Logger logger = org.slf4j.LoggerFactory.getLogger(ParseDocumentService.class);


    /**
     * Parses the given MultipartFile and returns a list of Document objects.
     *
     * @param file the MultipartFile to be parsed
     * @return a list of Document objects extracted from the file
     * @throws DocumentProcessingException if an error occurs during parsing
     */
    public List<Document> parse(MultipartFile file) {

        String filename = null != file.getOriginalFilename() ? file.getOriginalFilename() : "unknown";
        String contentType = null != file.getContentType() ? file.getContentType() : "unknown";

        logger.info("Parsing document: {} with content type: {}", filename, contentType);

        try {

            // Create a Resource from the MultipartFile to be used by the document readers
            Resource resource = new ByteArrayResource(file.getBytes()){
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            if (contentType.contains("pdf") || filename.endsWith(".pdf")) {
                return parsePdfDocument(resource);
            } else {
                return parseGenericDocs(resource);
            }


        } catch (Exception e) {
            throw new DocumentProcessingException("Failed to parse document: {} " + filename, e);
        }
    }

    /**
     * Parses a PDF document from the given Resource and returns a list of Document objects.
     *
     * @param resource the Resource representing the PDF document
     * @return a list of Document objects extracted from the PDF
     */
    List<Document> parsePdfDocument(Resource resource) {
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageBottomMargin(0)
                        .build());

        return pdfReader.read();
    }

    /**
     * Parses a generic document (non-PDF) from the given Resource and returns a list of Document objects.
     *
     * @param resource the Resource representing the generic document
     * @return a list of Document objects extracted from the document
     */
    List<Document> parseGenericDocs(Resource resource) {
        TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
        return tikaReader.read();
    }

}
