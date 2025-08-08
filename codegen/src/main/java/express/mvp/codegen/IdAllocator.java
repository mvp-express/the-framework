 package express.mvp.codegen;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic ID allocator using FNV-1a 32-bit hashing and deterministic probing.
 * Ranges (inclusive):
 *  - Services: [32..64999]
 *  - Methods:  [16..239] within a service
 *  - Messages: [32..64000]
 *
 * Reserved bands (not directly encoded here):
 *  - Caller should pass tombstones/reserved sets to avoid reuse.
 */
public final class IdAllocator {

    public static final int SERVICE_MIN = 32;
    public static final int SERVICE_MAX = 64999;

    public static final int METHOD_MIN = 16;
    public static final int METHOD_MAX = 239;

    public static final int MESSAGE_MIN = 32;
    public static final int MESSAGE_MAX = 64000;

    private IdAllocator() {}

    public static int assignServiceId(String serviceName,
                                      Map<String, Integer> existingByName,
                                      Set<Integer> tombstonesOrReserved) {
        Integer existing = existingByName.get(serviceName);
        if (existing != null) return existing;

        return assignGlobalDeterministic(
                "Service:" + serviceName,
                existingByName.values(),
                tombstonesOrReserved,
                SERVICE_MIN, SERVICE_MAX
        );
    }

    public static int assignServiceIdExplicit(int explicit,
                                              Map<String, Integer> existingByName,
                                              Set<Integer> tombstonesOrReserved) {
        requireInRange(explicit, SERVICE_MIN, SERVICE_MAX, "Service ID");
        requireNotUsed(explicit, existingByName.values(), "Service ID");
        requireNotTombstoned(explicit, tombstonesOrReserved, "Service ID");
        return explicit;
    }

    public static int assignMessageId(String messageName,
                                      Map<String, Integer> existingByName,
                                      Set<Integer> tombstonesOrReserved) {
        Integer existing = existingByName.get(messageName);
        if (existing != null) return existing;

        return assignGlobalDeterministic(
                "Message:" + messageName,
                existingByName.values(),
                tombstonesOrReserved,
                MESSAGE_MIN, MESSAGE_MAX
        );
    }

    public static int assignMessageIdExplicit(int explicit,
                                              Map<String, Integer> existingByName,
                                              Set<Integer> tombstonesOrReserved) {
        requireInRange(explicit, MESSAGE_MIN, MESSAGE_MAX, "Message ID");
        requireNotUsed(explicit, existingByName.values(), "Message ID");
        requireNotTombstoned(explicit, tombstonesOrReserved, "Message ID");
        return explicit;
    }

    public static int assignMethodId(String serviceName,
                                     String methodName,
                                     Map<String, Integer> methodIdsByQualifiedName,
                                     Set<Integer> methodTombstonesForService) {
        String key = serviceName + "." + methodName;
        Integer existing = methodIdsByQualifiedName.get(key);
        if (existing != null) return existing;

        // Gather used IDs within the service namespace
        Set<Integer> usedInService = new HashSet<>();
        for (Map.Entry<String, Integer> e : methodIdsByQualifiedName.entrySet()) {
            if (e.getKey().startsWith(serviceName + ".")) {
                usedInService.add(e.getValue());
            }
        }

        // Deterministic allocation with probing in [METHOD_MIN..METHOD_MAX]
        for (int k = 0; k <= 1024; k++) {
            int candidate = METHOD_MIN + (positiveHash(serviceName + "." + methodName + "#" + k) % (METHOD_MAX - METHOD_MIN + 1));
            if (!usedInService.contains(candidate) && (methodTombstonesForService == null || !methodTombstonesForService.contains(candidate))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate method ID for " + key + " after probing");
    }

    public static int assignMethodIdExplicit(int explicit,
                                             String serviceName,
                                             Map<String, Integer> methodIdsByQualifiedName,
                                             Set<Integer> methodTombstonesForService) {
        requireInRange(explicit, METHOD_MIN, METHOD_MAX, "Method ID");
        // Ensure not used in the service namespace by another method
        for (Map.Entry<String, Integer> e : methodIdsByQualifiedName.entrySet()) {
            if (e.getKey().startsWith(serviceName + ".") && e.getValue() == explicit) {
                // If it's the same entry, it's fine, but explicit caller ensures this case.
                throw new IllegalArgumentException("Method ID " + explicit + " already in use in " + serviceName);
            }
        }
        if (methodTombstonesForService != null && methodTombstonesForService.contains(explicit)) {
            throw new IllegalArgumentException("Method ID " + explicit + " is tombstoned for " + serviceName);
        }
        return explicit;
    }

    private static int assignGlobalDeterministic(String canonicalName,
                                                 Iterable<Integer> used,
                                                 Set<Integer> reservedOrTombstoned,
                                                 int min, int max) {
        for (int k = 0; k <= 4096; k++) {
            int candidate = min + (positiveHash(canonicalName + "#" + k) % (max - min + 1));
            if (!contains(used, candidate) && (reservedOrTombstoned == null || !reservedOrTombstoned.contains(candidate))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate ID for " + canonicalName + " after probing");
    }

    private static boolean contains(Iterable<Integer> values, int x) {
        for (Integer v : values) {
            if (v != null && v == x) return true;
        }
        return false;
    }

    private static void requireInRange(int id, int min, int max, String label) {
        if (id < min || id > max) {
            throw new IllegalArgumentException(label + " out of range [" + min + ".." + max + "]: " + id);
        }
    }

    private static void requireNotUsed(int id, Iterable<Integer> used, String label) {
        if (contains(used, id)) {
            throw new IllegalArgumentException(label + " already in use: " + id);
        }
    }

    private static void requireNotTombstoned(int id, Set<Integer> tombstones, String label) {
        if (tombstones != null && tombstones.contains(id)) {
            throw new IllegalArgumentException(label + " is tombstoned: " + id);
        }
    }

    private static int positiveHash(String s) {
        int h = fnv1a32(s);
        return h & 0x7fffffff;
    }

    /**
     * FNV-1a 32-bit hash for UTF-8 content.
     */
    private static int fnv1a32(String s) {
        final int FNV_PRIME = 0x01000193;
        int hash = 0x811C9DC5;
        for (int i = 0, len = s.length(); i < len; i++) {
            hash ^= s.charAt(i);
            hash *= FNV_PRIME;
        }
        return hash;
    }
}
