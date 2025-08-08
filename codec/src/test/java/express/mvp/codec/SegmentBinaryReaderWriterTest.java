package express.mvp.codec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SegmentBinaryReader and SegmentBinaryWriter implementations.
 */
public class SegmentBinaryReaderWriterTest {
    
    private Arena arena;
    private MemorySegment segment;
    private SegmentBinaryWriter writer;
    private SegmentBinaryReader reader;
    
    @BeforeEach
    void setUp() {
        arena = Arena.ofConfined();
        segment = arena.allocate(1024); // 1KB buffer
        writer = new SegmentBinaryWriter(segment);
        reader = new SegmentBinaryReader(segment);
    }
    
    @Test
    void testStringWriteRead() {
        String testString = "Hello, MVP.Express!";
        
        writer.writeString(testString);
        reader.position(0); // Reset reader position
        
        String result = reader.readString();
        assertEquals(testString, result);
    }
    
    @Test
    void testNullableStringWriteRead() {
        // Test non-null string
        String testString = "Test String";
        writer.writeNullableString(testString);
        reader.position(0);
        assertEquals(testString, reader.readNullableString());
        
        // Reset for null test
        writer.position(0);
        reader.position(0);
        
        // Test null string
        writer.writeNullableString(null);
        reader.position(0);
        assertNull(reader.readNullableString());
    }
    
    @Test
    void testIntWriteRead() {
        int testValue = 42;
        writer.writeInt(testValue);
        reader.position(0);
        assertEquals(testValue, reader.readInt());
        
        // Test negative value
        writer.position(0);
        reader.position(0);
        int negativeValue = -12345;
        writer.writeInt(negativeValue);
        reader.position(0);
        assertEquals(negativeValue, reader.readInt());
    }
    
    @Test
    void testNullableIntWriteRead() {
        // Test non-null integer
        Integer testValue = 123456;
        writer.writeNullableInt(testValue);
        reader.position(0);
        assertEquals(testValue, reader.readNullableInt());
        
        // Reset for null test
        writer.position(0);
        reader.position(0);
        
        // Test null integer
        writer.writeNullableInt(null);
        reader.position(0);
        assertNull(reader.readNullableInt());
    }
    
    @Test
    void testLongWriteRead() {
        long testValue = 9876543210L;
        writer.writeLong(testValue);
        reader.position(0);
        assertEquals(testValue, reader.readLong());
        
        // Test negative value
        writer.position(0);
        reader.position(0);
        long negativeValue = -9876543210L;
        writer.writeLong(negativeValue);
        reader.position(0);
        assertEquals(negativeValue, reader.readLong());
    }
    
    @Test
    void testNullableLongWriteRead() {
        // Test non-null long
        Long testValue = 987654321098L;
        writer.writeNullableLong(testValue);
        reader.position(0);
        assertEquals(testValue, reader.readNullableLong());
        
        // Reset for null test
        writer.position(0);
        reader.position(0);
        
        // Test null long
        writer.writeNullableLong(null);
        reader.position(0);
        assertNull(reader.readNullableLong());
    }
    
    @Test
    void testBooleanWriteRead() {
        // Test true
        writer.writeBoolean(true);
        reader.position(0);
        assertTrue(reader.readBoolean());
        
        // Test false
        writer.position(0);
        reader.position(0);
        writer.writeBoolean(false);
        reader.position(0);
        assertFalse(reader.readBoolean());
    }
    
    @Test
    void testNullableBooleanWriteRead() {
        // Test non-null boolean
        Boolean testValue = Boolean.TRUE;
        writer.writeNullableBoolean(testValue);
        reader.position(0);
        assertEquals(testValue, reader.readNullableBoolean());
        
        // Reset for null test
        writer.position(0);
        reader.position(0);
        
        // Test null boolean
        writer.writeNullableBoolean(null);
        reader.position(0);
        assertNull(reader.readNullableBoolean());
    }
    
    @Test
    void testFloatWriteRead() {
        float testValue = 3.14159f;
        writer.writeFloat(testValue);
        reader.position(0);
        assertEquals(testValue, reader.readFloat(), 0.00001f);
        
        // Test negative value
        writer.position(0);
        reader.position(0);
        float negativeValue = -2.71828f;
        writer.writeFloat(negativeValue);
        reader.position(0);
        assertEquals(negativeValue, reader.readFloat(), 0.00001f);
    }
    
    @Test
    void testNullableFloatWriteRead() {
        // Test non-null float
        Float testValue = 1.23456f;
        writer.writeNullableFloat(testValue);
        reader.position(0);
        assertEquals(testValue, reader.readNullableFloat());
        
        // Reset for null test
        writer.position(0);
        reader.position(0);
        
        // Test null float
        writer.writeNullableFloat(null);
        reader.position(0);
        assertNull(reader.readNullableFloat());
    }
    
    @Test
    void testDoubleWriteRead() {
        double testValue = 3.141592653589793;
        writer.writeDouble(testValue);
        reader.position(0);
        assertEquals(testValue, reader.readDouble(), 0.000000000000001);
        
        // Test negative value
        writer.position(0);
        reader.position(0);
        double negativeValue = -2.718281828459045;
        writer.writeDouble(negativeValue);
        reader.position(0);
        assertEquals(negativeValue, reader.readDouble(), 0.000000000000001);
    }
    
    @Test
    void testNullableDoubleWriteRead() {
        // Test non-null double
        Double testValue = 1.234567890123456;
        writer.writeNullableDouble(testValue);
        reader.position(0);
        assertEquals(testValue, reader.readNullableDouble());
        
        // Reset for null test
        writer.position(0);
        reader.position(0);
        
        // Test null double
        writer.writeNullableDouble(null);
        reader.position(0);
        assertNull(reader.readNullableDouble());
    }
    
    @Test
    void testBytesWriteRead() {
        byte[] testBytes = {1, 2, 3, 4, 5, -1, -2, -3};
        writer.writeBytes(testBytes);
        reader.position(0);
        assertArrayEquals(testBytes, reader.readBytes());
        
        // Test empty array
        writer.position(0);
        reader.position(0);
        byte[] emptyBytes = {};
        writer.writeBytes(emptyBytes);
        reader.position(0);
        assertArrayEquals(emptyBytes, reader.readBytes());
    }
    
    @Test
    void testNullableBytesWriteRead() {
        // Test non-null bytes
        byte[] testBytes = {10, 20, 30, 40, 50};
        writer.writeNullableBytes(testBytes);
        reader.position(0);
        assertArrayEquals(testBytes, reader.readNullableBytes());
        
        // Reset for null test
        writer.position(0);
        reader.position(0);
        
        // Test null bytes
        writer.writeNullableBytes(null);
        reader.position(0);
        assertNull(reader.readNullableBytes());
    }
    
    @Test
    void testPositionAndRemaining() {
        // Initial state
        assertEquals(0, writer.position());
        assertEquals(1024, writer.remaining());
        assertEquals(0, reader.position());
        assertEquals(1024, reader.remaining());
        
        // Write some data
        writer.writeInt(42);
        assertEquals(4, writer.position());
        assertEquals(1020, writer.remaining());
        
        // Set position manually
        writer.position(100);
        assertEquals(100, writer.position());
        assertEquals(924, writer.remaining());
        
        reader.position(100);
        assertEquals(100, reader.position());
        assertEquals(924, reader.remaining());
    }
    
    @Test
    void testChecksumValidation() {
        // Write some data
        writer.writeString("Test");
        writer.writeInt(42);
        writer.writeLong(123456789L);
        writer.writeChecksum();
        
        // Reset reader and validate
        reader.position(0);
        reader.readString();
        reader.readInt();
        reader.readLong();
        assertTrue(reader.validateChecksum());
    }
    
    @Test
    void testChecksumValidationFailure() {
        // Write some data
        writer.writeString("Test");
        writer.writeInt(42);
        writer.writeChecksum();
        
        // Corrupt the data by changing position and writing different value
        int checksumPos = writer.position();
        writer.position(0);
        writer.writeString("Different"); // Corrupt the data
        writer.position(checksumPos);
        
        // Reset reader and validate - should fail
        reader.position(0);
        reader.readString();
        reader.readInt();
        assertFalse(reader.validateChecksum());
    }
    
    @Test
    void testComplexDataStructure() {
        // Write a complex structure
        writer.writeString("User");
        writer.writeInt(25);
        writer.writeNullableString("john.doe@example.com");
        writer.writeBoolean(true);
        writer.writeNullableFloat(98.6f);
        writer.writeBytes(new byte[]{1, 2, 3, 4});
        writer.writeNullableLong(null);
        writer.writeChecksum();
        
        // Read back and verify
        reader.position(0);
        assertEquals("User", reader.readString());
        assertEquals(25, reader.readInt());
        assertEquals("john.doe@example.com", reader.readNullableString());
        assertTrue(reader.readBoolean());
        assertEquals(98.6f, reader.readNullableFloat(), 0.01f);
        assertArrayEquals(new byte[]{1, 2, 3, 4}, reader.readBytes());
        assertNull(reader.readNullableLong());
        assertTrue(reader.validateChecksum());
    }
}