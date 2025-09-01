# MyraCodec Testing Strategy
This document outlines the testing strategy for the `MyraCodec`, focusing on comprehensive coverage of core functionality, edge cases, and performance characteristics.

## 1. Core Functionality Testing
These tests validate the fundamental encode/decode (round-trip) capabilities of the codec.

- `testSimpleRecord`: Ensures a basic record with common primitive types and Strings can be serialized and deserialized correctly.

- `testAllTypesRecord`: A "kitchen sink" test that includes one of every supported primitive and wrapper type (int/Integer, long/Long, etc.) to validate all type handlers.

- `testSimpleType`: Validates that the codec can handle non-record, simple types like String as the top-level message body.

## 2. Edge Case and Boundary Testing
These tests push the codec to its limits with non-standard and potentially problematic data.

### Data Value Edge Cases
- `testBoundaryValues`: Uses Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE to check for overflow or sign-related bugs in the binary reader/writer.

- `testNullFieldValue`: A critical test to ensure that null values for all supported wrapper types and String are handled correctly by the presence-bit mechanism.

- `testEmptyCollectionsAndStrings`: Tests "" (empty string) and empty byte[] arrays.

### String and Character Set Edge Cases
- `testUnicodeAndSpecialStrings`: This is a parameterized test that sends various strings through the codec to stress-test the zero-copy UTF-8 implementation:

    - Empty string ("")

    - Standard ASCII

    - Multi-byte Unicode characters (e.g., 你好)

    - Emojis and symbols that require surrogate pair handling (e.g., 😀👍)

    - Strings containing control characters like newlines (\n) and tabs (\t).

### Nested and Complex Structures
- `testNestedRecord`: Validates that the codec can handle records that contain other records as fields. This is crucial for testing the recursive nature of the serialization logic. Note: This requires the codec to be enhanced to support nested record types.

## 3. Fuzz Testing and Malformed Data
These tests ensure the codec is resilient and fails gracefully when encountering corrupted or unexpected network data.

- `testUnregisteredMessage`: Attempts to encode an object whose class has not been registered. The codec must throw a clear `IllegalArgumentException`.

- `testUnknownMessageId`: Attempts to decode a message envelope containing a `methodId` that doesn't exist in the registry. Must throw a clear `IllegalArgumentException`.

- `testTruncatedPayload`: Simulates a common network error where a packet is cut short. The codec should detect that the buffer's limit is reached before the declared payload length is read and throw an `IndexOutOfBoundsException` (or a similar buffer-underflow exception).

- `testInvalidChecksum`: Manually corrupt a byte in the payload after encoding and before decoding. The checksum validation on the reader should fail.

## 4. Performance and Concurrency Testing
These tests validate the non-functional requirements of the codec.

- `testReflectionCache`: Verifies that the internal recordMetadataCache is populated on the first encounter with a record type and that subsequent operations on the same type do not increase the cache size. Also tests the clearCache() functionality.

- `testConcurrentAccess`: Spawns multiple threads that simultaneously encode and decode messages of the same type to ensure the ConcurrentHashMap cache and the overall codec logic are thread-safe.

By implementing this full suite, we can be highly confident in the correctness, robustness, and performance of the MyraCodec.
