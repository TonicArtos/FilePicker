
package com.tonicartos.component.internal;

import java.io.File;
import java.net.URLConnection;

public class ExtensionMapper {
    public static String getHeader(File file) {
        return URLConnection.getFileNameMap().getContentTypeFor(file.getName());
    }
}
