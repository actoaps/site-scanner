package dk.acto.web;

import java.net.URI;

/**
 * Created by Grudge on 29/04/2017.
 */
public class PageNode {
    private final URI uri;
    private final String contentType;
    private final String message;

    public PageNode(URI uri, String contentType, String message) {
        this.uri = uri;
        this.contentType = contentType;
        this.message = message;
    }

    public URI getUri() {
        return uri;
    }

    public String getContentType() {
        return contentType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "PageNode{" +
                "uri=" + uri +
                ", contentType='" + contentType + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
