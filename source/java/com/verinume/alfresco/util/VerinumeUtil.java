package com.verinume.alfresco.util;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @Author Tapan D. Thakkar on 3/22/2015.
 */
public interface VerinumeUtil {

    String getPathFromSpaceRef(final NodeRef ref, boolean children);
}
