package us.rin.utils;

import java.io.Serial;
import java.io.Serializable;

public record BypassListUtil(String moduleName, String bypassFor, String bypassName, String configString) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
