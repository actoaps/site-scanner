package dk.acto.web;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class ResponseSummary {
    private Integer statusCode;
    @Singular
    private List<PageNode> nodes;
}
