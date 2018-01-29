package dk.acto.web;

import com.google.common.net.InternetDomainName;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Grudge on 29/04/2017.
 */
@Data
@Builder
@Slf4j
public class PageEdge {
    private final PageNode parent;
    private final PageNode child;

    public boolean hasSameHost() {
        if (parent == null)
            return true;

        InternetDomainName left = InternetDomainName.from(parent.getUri().getHost()).topPrivateDomain();
        InternetDomainName right = InternetDomainName.from(child.getUri().getHost()).topPrivateDomain();

        return left.equals(right);
    }
}
