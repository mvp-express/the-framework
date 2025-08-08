package express.mvp.codegen;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Reads/writes IdsLock from/to a Properties-based file.
 * File layout (example):
 *  version=1
 *  services.AccountService=42
 *  methods.AccountService.GetBalance=1
 *  messages.GetBalanceRequest=101
 *  tombstones.services=5,6
 *  tombstones.methods.AccountService=15,16
 *  tombstones.messages=999,1000
 *  aliases.services.Accounts=AccountService
 *  aliases.messages.BalanceResp=GetBalanceResponse
 *
 * Note: We escape '.' and '%' in names to keep the key structure parseable.
 */
public final class LockfileManager {

    public static final String DEFAULT_LOCKFILE = ".mvpe.ids.lock";

    private LockfileManager() {}

    public static IdsLock load(Path path) throws IOException {
        IdsLock lock = new IdsLock();
        if (path == null || !Files.exists(path)) {
            return lock;
        }
        Properties props = new Properties();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            props.load(in);
        }

        lock.version = Integer.parseInt(props.getProperty("version", "1"));

        // Services
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("services.")) {
                String name = unescape(key.substring("services.".length()));
                lock.services.put(name, Integer.parseInt(props.getProperty(key)));
            }
        }
        // Methods
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("methods.")) {
                String name = unescape(key.substring("methods.".length())); // "Service.Method"
                lock.methods.put(name, Integer.parseInt(props.getProperty(key)));
            }
        }
        // Messages
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("messages.")) {
                String name = unescape(key.substring("messages.".length()));
                lock.messages.put(name, Integer.parseInt(props.getProperty(key)));
            }
        }
        // Tombstones global services/messages
        parseIntList(props.getProperty("tombstones.services", "")).forEach(lock.tombstoneServices::add);
        parseIntList(props.getProperty("tombstones.messages", "")).forEach(lock.tombstoneMessages::add);

        // Tombstones methods by service
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("tombstones.methods.")) {
                String service = unescape(key.substring("tombstones.methods.".length()));
                Set<Integer> set = lock.tombstonesForService(service);
                parseIntList(props.getProperty(key, "")).forEach(set::add);
            }
        }

        // Aliases
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("aliases.services.")) {
                String oldName = unescape(key.substring("aliases.services.".length()));
                lock.aliasServices.put(oldName, props.getProperty(key));
            } else if (key.startsWith("aliases.messages.")) {
                String oldName = unescape(key.substring("aliases.messages.".length()));
                lock.aliasMessages.put(oldName, props.getProperty(key));
            }
        }

        return lock;
    }

    public static void save(IdsLock lock, Path path) throws IOException {
        Properties props = new Properties();
        props.setProperty("version", Integer.toString(lock.version));

        for (Map.Entry<String, Integer> e : lock.services.entrySet()) {
            props.setProperty("services." + escape(e.getKey()), Integer.toString(e.getValue()));
        }
        for (Map.Entry<String, Integer> e : lock.methods.entrySet()) {
            props.setProperty("methods." + escape(e.getKey()), Integer.toString(e.getValue()));
        }
        for (Map.Entry<String, Integer> e : lock.messages.entrySet()) {
            props.setProperty("messages." + escape(e.getKey()), Integer.toString(e.getValue()));
        }

        props.setProperty("tombstones.services", joinInts(lock.tombstoneServices));
        props.setProperty("tombstones.messages", joinInts(lock.tombstoneMessages));
        for (Map.Entry<String, Set<Integer>> e : lock.tombstoneMethods.entrySet()) {
            props.setProperty("tombstones.methods." + escape(e.getKey()), joinInts(e.getValue()));
        }

        for (Map.Entry<String, String> e : lock.aliasServices.entrySet()) {
            props.setProperty("aliases.services." + escape(e.getKey()), e.getValue());
        }
        for (Map.Entry<String, String> e : lock.aliasMessages.entrySet()) {
            props.setProperty("aliases.messages." + escape(e.getKey()), e.getValue());
        }

        if (path != null) {
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
                props.store(out, "MVP.Express IDs Lockfile");
            }
        }
    }

    private static List<Integer> parseIntList(String s) {
        List<Integer> result = new ArrayList<>();
        if (s == null || s.isBlank()) return result;
        for (String p : s.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) {
                result.add(Integer.parseInt(t));
            }
        }
        return result;
    }

    private static String joinInts(Collection<Integer> ints) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Integer i : new TreeSet<>(ints)) {
            if (!first) sb.append(",");
            sb.append(i);
            first = false;
        }
        return sb.toString();
    }

    private static String escape(String name) {
        // Escape '%' first then '.'
        return name.replace("%", "%25").replace(".", "%2E");
    }

    private static String unescape(String name) {
        return name.replace("%2E", ".").replace("%25", "%");
    }
}
