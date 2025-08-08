package express.mvp.codegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory representation of the IDs lock.
 * Stored on disk via LockfileManager using a Properties-based format.
 */
public class IdsLock {
    public int version = 1;

    // Canonical name -> ID
    public final Map<String, Integer> services = new HashMap<>();
    public final Map<String, Integer> methods = new HashMap<>(); // key: "Service.Method"
    public final Map<String, Integer> messages = new HashMap<>();

    // Tombstoned IDs that must never be reused
    public final Set<Integer> tombstoneServices = new HashSet<>();
    public final Map<String, Set<Integer>> tombstoneMethods = new HashMap<>(); // by service
    public final Set<Integer> tombstoneMessages = new HashSet<>();

    // Rename support (old -> new)
    public final Map<String, String> aliasServices = new HashMap<>();
    public final Map<String, String> aliasMessages = new HashMap<>();

    public Set<Integer> tombstonesForService(String service) {
        return tombstoneMethods.computeIfAbsent(service, k -> new HashSet<>());
    }
}
