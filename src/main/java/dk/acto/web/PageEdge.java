package dk.acto.web;

/**
 * Created by Grudge on 29/04/2017.
 */
public class PageEdge {
    private final PageNode parent;
    private final PageNode child;
    private final int StatusCode;

    public PageEdge(PageNode parent, PageNode child, int statusCode) {
        this.parent = parent;
        this.child = child;
        StatusCode = statusCode;
    }

    public PageNode getParent() {
        return parent;
    }

    public PageNode getChild() {
        return child;
    }

    public int getStatusCode() {
        return StatusCode;
    }

    @Override
    public String toString() {
        return "PageEdge{" +
                "parent=" + parent +
                ", child=" + child +
                ", StatusCode=" + StatusCode +
                '}';
    }

    public boolean hasSameHost() {
        return parent == null || parent.getUri().getHost().equals(child.getUri().getHost());
    }
}
