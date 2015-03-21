package com.verinume.alfresco.util;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.ISO9075;

import java.util.Collection;

/**
 * @Author Tapan D. Thakkar on 3/22/2015.
 */

public class VerinumeUtilImpl implements VerinumeUtil {

    private NodeService nodeService;
    private NamespaceService namespaceService;

    @Override
    public String getPathFromSpaceRef(final NodeRef ref, boolean children)
    {
        final Path path = nodeService.getPath(ref);
        final StringBuilder buf = new StringBuilder(64);
        String elementString;
        Path.Element element;
        ChildAssociationRef elementRef;
        Collection<?> prefixes;
        for (int i = 0; i < path.size(); i++)
        {
            elementString = "";
            element = path.get(i);
            if (element instanceof Path.ChildAssocElement)
            {
                elementRef = ((Path.ChildAssocElement) element).getRef();
                if (elementRef.getParentRef() != null)
                {
                    prefixes = namespaceService.getPrefixes(elementRef.getQName().getNamespaceURI());
                    if (prefixes.size() > 0)
                    {
                        elementString = '/' + (String) prefixes.iterator().next() + ':' + ISO9075.encode(elementRef.getQName().getLocalName());
                    }
                }
            }

            buf.append(elementString);
        }
        if (children == true)
        {
            // append syntax to get all children of the path
            buf.append("//*");
        }
        else
        {
            // append syntax to just represent the path, not the children
            buf.append("/*");
        }

        return buf.toString();
    }

    public NamespaceService getNamespaceService() {
        return namespaceService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public NodeService getNodeService() {
        return nodeService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
}
