package express.mvp.codegen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Assigns and validates IDs for a given ServiceSchema using the IdsLock.
 * This currently covers service.id and methods[].id.
 * Messages IDs are tracked in the lock for future use by encoders, even if not present on MessageDef type.
 */
public final class IdAssignment {

    public enum LockMode {
        OFF, CHECK, WRITE
    }

    private IdAssignment() {}

    public static void assignAndValidate(ServiceSchema schema, Path lockfilePath, LockMode mode) throws IOException {
        if (mode == null) mode = LockMode.OFF;
        IdsLock lock = LockfileManager.load(lockfilePath);

        // Resolve service name via aliases (rename support)
        String serviceName = resolveAlias(schema.getService(), lock.aliasServices);
        if (!serviceName.equals(schema.getService())) {
            // Update schema to canonical name if alias exists
            schema.setService(serviceName);
        }

        // 1) Service ID
        int serviceId;
        if (schema.getId() > 0) {
            // Explicit
            serviceId = IdAllocator.assignServiceIdExplicit(schema.getId(), lock.services, lock.tombstoneServices);
            // If lock has a different mapping, fail in CHECK or overwrite in WRITE
            Integer locked = lock.services.get(serviceName);
            if (locked != null && locked != serviceId) {
                failOrFix(mode, "Service ID mismatch for " + serviceName + ": lock=" + locked + ", schema=" + serviceId,
                        () -> lock.services.put(serviceName, serviceId));
            } else {
                lock.services.put(serviceName, serviceId);
            }
        } else {
            // Allocate deterministically based on name
            serviceId = IdAllocator.assignServiceId(serviceName, lock.services, lock.tombstoneServices);
            schema.setId(serviceId);
            lock.services.put(serviceName, serviceId);
        }

        // 2) Methods
        if (schema.getMethods() != null) {
            for (MethodDef m : schema.getMethods()) {
                String qualified = serviceName + "." + m.getName();
                Set<Integer> svcTombstones = lock.tombstonesForService(serviceName);
                int methodId;
                if (m.getId() > 0) {
                    methodId = IdAllocator.assignMethodIdExplicit(m.getId(), serviceName, lock.methods, svcTombstones);
                    Integer locked = lock.methods.get(qualified);
                    if (locked != null && locked != methodId) {
                        failOrFix(mode, "Method ID mismatch for " + qualified + ": lock=" + locked + ", schema=" + methodId,
                                () -> lock.methods.put(qualified, methodId));
                    } else {
                        lock.methods.put(qualified, methodId);
                    }
                } else {
                    methodId = IdAllocator.assignMethodId(serviceName, m.getName(), lock.methods, svcTombstones);
                    m.setId(methodId);
                    lock.methods.put(qualified, methodId);
                }
            }
        }

        // 3) Messages - populate lock if message names exist but ignore if schema type lacks id field.
        if (schema.getMessages() != null) {
            for (MessageDef msg : schema.getMessages()) {
                String msgName = resolveAlias(msg.getName(), lock.aliasMessages);
                if (!msgName.equals(msg.getName())) {
                    msg.setName(msgName);
                }
                // Try to reflectively find explicit id if present (non-fatal if absent)
                Integer explicit = tryGetMessageIdExplicit(msg);
                if (explicit != null) {
                    int id = IdAllocator.assignMessageIdExplicit(explicit, lock.messages, lock.tombstoneMessages);
                    Integer locked = lock.messages.get(msgName);
                    if (locked != null && !locked.equals(id)) {
                        failOrFix(mode, "Message ID mismatch for " + msgName + ": lock=" + locked + ", schema=" + id,
                                () -> lock.messages.put(msgName, id));
                    } else {
                        lock.messages.put(msgName, id);
                    }
                } else {
                    // Allocate if not already present in lock
                    if (!lock.messages.containsKey(msgName)) {
                        int id = IdAllocator.assignMessageId(msgName, lock.messages, lock.tombstoneMessages);
                        // Only write in WRITE mode; in CHECK mode, require existing
                        if (mode == LockMode.CHECK) {
                            throw new IllegalStateException("Missing message ID in lock for " + msgName + " (CHECK mode). Run WRITE mode locally first.");
                        }
                        lock.messages.put(msgName, id);
                        // Attempt to set back via reflection if possible
                        trySetMessageId(msg, id);
                    } else {
                        // ensure schema reflects it if field exists
                        trySetMessageId(msg, lock.messages.get(msgName));
                    }
                }
            }
        }

        // Persist
        if (mode == LockMode.WRITE) {
            LockfileManager.save(lock, lockfilePath);
        } else if (mode == LockMode.CHECK) {
            // In CHECK mode, we ensure no drift occurred beyond what we checked above.
            // If we reached here, mappings are consistent.
        }
    }

    private static String resolveAlias(String name, Map<String, String> aliases) {
        String cur = name;
        // Follow chain up to a sane limit
        for (int i = 0; i < 10; i++) {
            String next = aliases.get(cur);
            if (next == null) return cur;
            cur = next;
        }
        return cur;
    }

    private static void failOrFix(LockMode mode, String msg, Runnable fix) {
        if (mode == LockMode.WRITE) {
            fix.run();
        } else {
            throw new IllegalStateException(msg);
        }
    }

    private static Integer tryGetMessageIdExplicit(MessageDef msg) {
        try {
            // Look for getId()
            var m = msg.getClass().getMethod("getId");
            Object v = m.invoke(msg);
            if (v instanceof Number) {
                int id = ((Number) v).intValue();
                return id > 0 ? id : null;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static void trySetMessageId(MessageDef msg, int id) {
        try {
            var m = msg.getClass().getMethod("setId", int.class);
            m.invoke(msg, id);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
