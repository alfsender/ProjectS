<#macro documentComment node>
<#escape x as jsonUtils.encodeJSONString(x)>
    <#if node.hasAspect("fm:discussable")>
        <#assign forumFolder = node.childAssocs["fm:discussion"][0] />
        <#if forumFolder.childByNamePath['Comments']??>
            <#assign commentsFolder = forumFolder.childByNamePath['Comments'] />
            <#assign allComments = commentsFolder.childAssocs["cm:contains"] />
                <#list allComments as comment>
                {
                    "name": "${comment.properties.name!''}",
                    "title": "${comment.properties.title!''}",
                    "createdOn": "${xmldate(comment.properties.created)}",
                    <#if comment.author??>
                       <@renderPerson person=comment.author fieldName="author" />
                    <#else>
                    "author":
                     {
                        "username": "${comment.properties.creator}"
                     },
                     </#if>
                    "content": "${stringUtils.stripUnsafeHTML(comment.content)}"
                }<#if comment_has_next>,</#if>
                </#list>
        </#if>
    </#if>
</#escape>
</#macro>

<#escape x as jsonUtils.encodeJSONString(x)>
{
    "success": "${success?string}",
    "message": "${message?string}",
    "total": "${documents?size?number}",
   	"data":
    [
        <#if documents?? >
            <#list documents as item>
            {
                "fileName": "${item.name}",
                "comments":
                [
                    <@documentComment node=item/>
                ]
            }<#if item_has_next>,</#if>
            </#list>
        </#if>
   	]
}
</#escape>
