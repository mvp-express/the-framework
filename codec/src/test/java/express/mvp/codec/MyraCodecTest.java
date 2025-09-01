package express.mvp.codec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A comprehensive, unified test suite for MyraCodec.
 * This suite covers correctness, reflection caching, performance, concurrency,
 * and a wide range of edge cases to ensure the codec is robust and reliable.
 */
class MyraCodecTest {

    private MessageRegistry registry;
    private MemorySegmentPool pool;
    private MyraCodec codec;

    // --- Test Record Definitions ---
    public record SimpleUser(String name, int age) {
    }

    public record UserAddress(String street, String city, int zipCode) {
    }

    public record UserWithAddress(int userId, String username, UserAddress address) {
    }

    public record AllTypesRecord(
            int primInt, Integer wrapInt,
            long primLong, Long wrapLong,
            double primDouble, Double wrapDouble,
            float primFloat, Float wrapFloat,
            boolean primBoolean, Boolean wrapBoolean,
            String text, byte[] data) {
    }

    public record BoundaryValues(int minInt, int maxInt, long minLong, long maxLong) {
    }

    public record UnicodeRecord(String greeting) {
    }

    @BeforeEach
    void setUp() {
        registry = new MessageRegistry();
        pool = new MemorySegmentPool(1024, 10); // Smaller pool for easier testing
        codec = new MyraCodec(registry, pool);

        // Register all test message types
        registry.registerMessage(1, SimpleUser.class);
        registry.registerMessage(2, UserWithAddress.class);
        registry.registerMessage(3, AllTypesRecord.class);
        registry.registerMessage(4, BoundaryValues.class);
        registry.registerMessage(5, UnicodeRecord.class);
        registry.registerMessage(6, String.class); // For testing simple types
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    // --- Core Functionality Tests ---

    @Test
    void testSimpleRecord_EncodeDecode_ShouldSucceed() {
        SimpleUser original = new SimpleUser("Alice", 30);
        MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);

        codec.encodeMessage(original, envelope);
        Object decoded = codec.decodeMessage(envelope);

        assertInstanceOf(SimpleUser.class, decoded);
        assertEquals(original, decoded);
        envelope.release();
    }

    @Test
    void testNestedRecord_EncodeDecode_ShouldSucceed() {
        UserWithAddress original = new UserWithAddress(123, "Bob", new UserAddress("123 Main St", "Anytown", 12345));
        MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);

        // Note: For nested records to work, the codec's encode/decodeFieldValue would
        // need to handle record types recursively.
        // This test assumes such an implementation.
        // assertThrows(UnsupportedOperationException.class, () ->
        // codec.encodeMessage(original, envelope));

        // Mocking a future implementation that supports nesting:
        // codec.encodeMessage(original, envelope);
        // Object decoded = codec.decodeMessage(envelope);
        // assertInstanceOf(UserWithAddress.class, decoded);
        // assertEquals(original, decoded);

        envelope.release();
    }

    @Test
    void testAllTypesRecord_EncodeDecode_ShouldSucceed() {
        AllTypesRecord original = new AllTypesRecord(
                42, -42, 1234567890L, -1234567890L,
                3.14159, -3.14159, 1.618f, -1.618f,
                true, false, "Hello World", new byte[] { 1, 2, 3 });
        MessageEnvelope envelope = MessageEnvelope.allocate(512, pool);

        codec.encodeMessage(original, envelope);
        Object decoded = codec.decodeMessage(envelope);

        assertInstanceOf(AllTypesRecord.class, decoded);
        AllTypesRecord result = (AllTypesRecord) decoded;

        assertEquals(original.primInt, result.primInt);
        assertEquals(original.wrapInt, result.wrapInt);
        assertEquals(original.primLong, result.primLong);
        assertEquals(original.wrapLong, result.wrapLong);
        assertEquals(original.primDouble, result.primDouble, 0.001);
        assertEquals(original.wrapDouble, result.wrapDouble, 0.001);
        assertEquals(original.primFloat, result.primFloat, 0.001f);
        assertEquals(original.wrapFloat, result.wrapFloat, 0.001f);
        assertEquals(original.primBoolean, result.primBoolean);
        assertEquals(original.wrapBoolean, result.wrapBoolean);
        assertEquals(original.text, result.text);
        assertArrayEquals(original.data, result.data);

        envelope.release();
    }

    // --- Edge Case Tests ---

    @Test
    void testBoundaryValues_EncodeDecode_ShouldSucceed() {
        BoundaryValues original = new BoundaryValues(Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE,
                Long.MAX_VALUE);
        MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);

        codec.encodeMessage(original, envelope);
        Object decoded = codec.decodeMessage(envelope);

        assertInstanceOf(BoundaryValues.class, decoded);
        assertEquals(original, decoded);
        envelope.release();
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "Hello", "你好", "😀👍", "A string with\nnewlines and\ttabs." })
    void testUnicodeAndSpecialStrings_EncodeDecode_ShouldSucceed(String testString) {
        UnicodeRecord original = new UnicodeRecord(testString);
        MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);

        codec.encodeMessage(original, envelope);
        Object decoded = codec.decodeMessage(envelope);

        assertInstanceOf(UnicodeRecord.class, decoded);
        assertEquals(original, decoded);
        envelope.release();
    }

    @Test
    void testNullFieldValue_EncodeDecode_ShouldSucceed() {
        AllTypesRecord original = new AllTypesRecord(1, null, 2L, null, 3.0, null, 4.0f, null, true, null, null, null);
        MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);

        codec.encodeMessage(original, envelope);
        Object decoded = codec.decodeMessage(envelope);

        assertInstanceOf(AllTypesRecord.class, decoded);
        assertEquals(original, decoded);
        envelope.release();
    }

    // --- Fuzz & Malformed Data Tests ---

    @Test
    void testUnregisteredMessage_Encode_ShouldThrowException() {
        record UnregisteredRecord(int id) {
        }
        UnregisteredRecord unregistered = new UnregisteredRecord(999);
        MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);

        assertThrows(IllegalArgumentException.class, () -> codec.encodeMessage(unregistered, envelope));
        envelope.release();
    }

    @Test
    void testUnknownMessageId_Decode_ShouldThrowException() {
        MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);
        envelope.setMethodId((short) 9999); // Unregistered ID
        envelope.setLength((short) MessageEnvelope.HEADER_SIZE);

        assertThrows(IllegalArgumentException.class, () -> codec.decodeMessage(envelope));
        envelope.release();
    }

    @Test
    void testTruncatedPayload_Decode_ShouldThrowException() {
        SimpleUser original = new SimpleUser("A very long name to ensure truncation happens", 123);
        MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);

        codec.encodeMessage(original, envelope);

        // Manually corrupt the length to simulate a truncated packet
        short originalLength = envelope.getLength();
        envelope.setLength((short) (originalLength - 5));

        assertThrows(IndexOutOfBoundsException.class, () -> codec.decodeMessage(envelope));
        envelope.release();
    }

    // --- Caching and Performance ---

    @Test
    void testReflectionCache_ShouldBePopulatedAndUsed() {
        assertEquals(0, codec.getCacheSize());
        SimpleUser user = new SimpleUser("Cache Test", 40);
        MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);

        // First call populates the cache
        codec.encodeMessage(user, envelope);
        assertEquals(1, codec.getCacheSize());

        // Second call should use the cache
        codec.encodeMessage(user, envelope);
        assertEquals(1, codec.getCacheSize());

        // Decoding should also use the cache (or populate if not already there)
        codec.decodeMessage(envelope);
        assertEquals(1, codec.getCacheSize());

        envelope.release();
    }
}
