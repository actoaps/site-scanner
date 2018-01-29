package dk.acto.web;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageChildren {
    private PageNode parent;
    private List<PageNode> children;

}
