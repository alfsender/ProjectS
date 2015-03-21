package com.verinume.alfresco.export;

import com.verinume.alfresco.constant.AlfrescoConstants;
import com.verinume.alfresco.util.VerinumeUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @Author Tapan D. Thakkar on 3/20/2015.
 */
public class VerinumeExportToExcel extends AbstractWebScript implements WebScript {

    private static final Log LOGGER = LogFactory.getLog(VerinumeExportToExcel.class);

    private SiteService siteService;
    private NodeService nodeService;
    private SearchService searchService;
    private VerinumeUtil verinumeUtil;
    private ContentService contentService;

    @Override
    public void execute(WebScriptRequest request, WebScriptResponse response) throws IOException {
        String site = request.getParameter(AlfrescoConstants.SITE);
        String path = request.getParameter(AlfrescoConstants.PATH);

        if (null != site) {
            try{
                this.exportSiteContent(site, path, response);
            }catch(Exception e){
                this.generateErrorResponse(e.getMessage(), response);
            }
        } else {
            this.generateErrorResponse("Site is not passed in request", response);
        }
    }


    private void exportSiteContent(String site, String path, WebScriptResponse response) {
        NodeRef folder = this.getFolderRef(site, path);
        if(null != folder) {
            String searchQuery = AlfrescoConstants.PATH_QUERY + this.verinumeUtil.getPathFromSpaceRef(folder, true) + "\""
                    + AlfrescoConstants.PDF_QUERY;
            LOGGER.info("Alfresco search query : " + searchQuery);
            ResultSet resultSet = this.searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, searchQuery);
            List<NodeRef> searchResult = resultSet.getNodeRefs();
            LOGGER.info("No of records found : " + searchResult.size());
            if (searchResult.size() > 0) {
                this.generateExcel(searchResult, response);
            }
        }else{
            this.generateErrorResponse("Invalid Site Passed", response);
        }
    }

    /**
     * @param site
     * @param path
     * @return
     */
    private NodeRef getFolderRef(String site, String path) {
        NodeRef child = null;
        SiteInfo siteInfo = this.siteService.getSite(site);
        if (null != siteInfo) {
            NodeRef documentLibrary = this.siteService.getContainer(siteInfo.getShortName(), SiteService.DOCUMENT_LIBRARY);
            child = documentLibrary;
            if (null != path && !path.isEmpty()) {
                final StringTokenizer t = new StringTokenizer(path, "/");
                while (t.hasMoreTokens() && child != null) {
                    String name = t.nextToken();
                    NodeRef folder = this.nodeService.getChildByName(child, ContentModel.ASSOC_CONTAINS, name);
                    if (null != folder) {
                        child = folder;
                    } else {
                        break;
                    }
                }
            }
        }
        return child;
    }


    private void generateExcel(List<NodeRef> result, WebScriptResponse response) {

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(AlfrescoConstants.SHEET);
        sheet.setDefaultColumnWidth(30);

        CellStyle cs = workbook.createCellStyle();
        HSSFFont workbookFont = workbook.createFont();
        workbookFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        workbookFont.setFontHeightInPoints((short) 10);
        cs.setFont(workbookFont);

        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());

        DateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");

        // Create header
        Row headerRow = sheet.createRow(0);
        // File Name
        HSSFCell fileNameHeaderCell = (HSSFCell) headerRow.createCell(0);
        HSSFRichTextString fileNameHeader = new HSSFRichTextString(AlfrescoConstants.FILENAME);
        fileNameHeader.applyFont(workbookFont);
        fileNameHeaderCell.setCellValue(fileNameHeader);
        fileNameHeaderCell.setCellStyle(style);

        // Comments Name
        HSSFCell commentHeaderCell = (HSSFCell) headerRow.createCell(1);
        HSSFRichTextString commentHeader = new HSSFRichTextString(AlfrescoConstants.COMMENT);
        commentHeader.applyFont(workbookFont);
        commentHeaderCell.setCellValue(commentHeader);
        commentHeaderCell.setCellStyle(style);

        // Comment Author
        HSSFCell commentAuthorHeaderCell = (HSSFCell) headerRow.createCell(2);
        HSSFRichTextString commentAuthorHeader = new HSSFRichTextString(AlfrescoConstants.AUTHOR);
        commentAuthorHeader.applyFont(workbookFont);
        commentAuthorHeaderCell.setCellValue(commentAuthorHeader);
        commentAuthorHeaderCell.setCellStyle(style);

        // Comment Created Date
        HSSFCell commentDateHeaderCell = (HSSFCell) headerRow.createCell(3);
        HSSFRichTextString commentDateHeader = new HSSFRichTextString(AlfrescoConstants.CREATED_DATE);
        commentDateHeader.applyFont(workbookFont);
        commentDateHeaderCell.setCellValue(commentDateHeader);
        commentDateHeaderCell.setCellStyle(style);

        int rowNum = 0;
        for (NodeRef node : result) {
            rowNum = rowNum + 1;
            Row row = sheet.createRow(rowNum);
            HSSFCell fileNameCell = (HSSFCell) row.createCell(0);
            HSSFRichTextString fileName = new HSSFRichTextString((String) this.nodeService.getProperty(node, ContentModel.PROP_NAME));
            fileNameCell.setCellValue(fileName);
            fileNameCell.setCellStyle(style);

            if (this.nodeService.hasAspect(node, ForumModel.ASPECT_DISCUSSABLE)) {
                List<ChildAssociationRef> childAssoc = this.nodeService.getChildAssocs(node, ForumModel.ASSOC_DISCUSSION, RegexQNamePattern.MATCH_ALL);
                if (childAssoc.size() != 0) {
                    ChildAssociationRef discussionAssoc = childAssoc.get(0);
                    NodeRef topicNode = nodeService.getChildAssocs(discussionAssoc.getChildRef(), ContentModel.ASSOC_CONTAINS,
                            QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Comments")).get(0).getChildRef();

                    List<ChildAssociationRef> allComments = this.nodeService.getChildAssocs(topicNode, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
                    boolean newRow = false;
                    for (ChildAssociationRef commentRef : allComments) {
                        NodeRef commentNode = commentRef.getChildRef();
                        if (newRow) {
                            rowNum = rowNum + 1;
                            row = sheet.createRow(rowNum++);
                        }
                        HSSFCell commentCell = (HSSFCell) row.createCell(1);
                        ContentReader reader = contentService.getReader(commentNode, ContentModel.PROP_CONTENT);
                        HSSFRichTextString comment = new HSSFRichTextString(reader.getContentString());
                        commentCell.setCellValue(comment);
                        commentCell.setCellStyle(style);

                        HSSFCell commentAuthor = (HSSFCell) row.createCell(2);
                        HSSFRichTextString author = new HSSFRichTextString((String) this.nodeService.getProperty(commentNode, ContentModel.PROP_CREATOR));
                        commentAuthor.setCellValue(author);
                        commentAuthor.setCellStyle(style);

                        HSSFCell commentDate = (HSSFCell) row.createCell(3);
                        Date createdDate = (Date) this.nodeService.getProperty(commentNode, ContentModel.PROP_CREATED);
                        HSSFRichTextString date = new HSSFRichTextString(formatter.format(createdDate));
                        commentDate.setCellValue(date);
                        commentDate.setCellStyle(style);

                        newRow = true;
                    }
                }
            }
        }
        OutputStream os = null;
        try {
            os = response.getOutputStream();
            workbook.write(response.getOutputStream());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=exportToExcel.xls");
        } catch (Exception e) {
            generateErrorResponse(e.getMessage(), response);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }


    private void generateErrorResponse(String msg, WebScriptResponse response) {
        try {
            JSONObject object = new JSONObject();
            object.put("Success", false);
            object.put("Message", msg);
            response.getWriter().write(object.toString());
            response.getWriter().flush();
            response.getWriter().close();
        } catch (Exception e) {
        }


    }

    // GETTERS AND SETTERS
    public SiteService getSiteService() {
        return siteService;
    }

    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public NodeService getNodeService() {
        return nodeService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public VerinumeUtil getVerinumeUtil() {
        return verinumeUtil;
    }

    public void setVerinumeUtil(VerinumeUtil verinumeUtil) {
        this.verinumeUtil = verinumeUtil;
    }

    public ContentService getContentService() {
        return contentService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }
}
