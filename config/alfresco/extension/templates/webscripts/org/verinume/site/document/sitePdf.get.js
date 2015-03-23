/**
* API to get all PDF documents from site (passed as URL parameter).
* NOTE : This API will consider (VALID) path passed in URL request, if path is not passed in URL API will search in
* repository folder of site.
**/

function main(){

    logger.log("API To Get All PDF documents from site");
    var shortName = args["site"];
    logger.log("Execution started site : " + shortName);
    var folderPath = args["path"];
    var success = false;
    var site = siteService.getSite(shortName);
    if(site != null){
        var documentLibraryContainer = site.getContainer("documentLibrary");
        if(folderPath != null && folderPath != ""){
            logger.log("Folder path passed : " + folderPath);
            folderRef = documentLibraryContainer.childByNamePath(folderPath);
        }else{
        	folderRef = documentLibraryContainer;
        }

        // Check if passed path is valid, if not return
        if(folderPath != null && folderPath != "" && folderRef == null){
        	model.message = "Invalid Folder Path passed";
        	model.documents = [];
        	model.size = 0;
        	return success;
        }

        // All arguments passed are valid, now search document.
        var documents = findDocuments(folderRef);
        success = true;
        model.size = documents.length;
        model.documents = documents;
        model.message = "SUCCESS";
        return success;
    }else{
     	model.message = "Invalid Site passed";
     	model.documents = [];
     	model.size = 0;
     	return success;
    }
}

/**
* This function will search all pdf document from folderRef passed.
*/
function findDocuments(folderRef, documents){

    var folderPathQuery = "+PATH:\"" + folderRef.getQnamePath() + "//*\"" ;
    var pdfQuery = "+@cm\\:content.mimetype:\"application/pdf\"";

    var searchQuery = folderPathQuery + " " + pdfQuery;
    logger.log("Folder path query : " + searchQuery);
    var allNodes = [];
    allNodes = search.query(
    {
        query: searchQuery,
        language: "lucene"
    });

    return allNodes;
}

model.success = main();