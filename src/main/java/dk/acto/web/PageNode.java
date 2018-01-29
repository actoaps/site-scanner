package dk.acto.web;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.net.URI;
import java.util.List;

/**
 * Created by Grudge on 29/04/2017.
 */
@Data
@Builder
public class PageNode {
    private final URI uri;
    private final int statusCode;
    private final String contentType;
    private final String actualType;
    private final String charset;
    private long loadTimeInMillis;
    @Singular
    private final List<String> messages;
}
