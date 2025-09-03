package express.mvp.codec;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

/**
 * Zero-copy message envelope for MVP.Express RPC protocol.
 * <p>
 * Uses pooled MemorySegments to eliminate GC pressure and achieve
 * true zero-copy performance. No heap allocations in hot paths.
 * <p>
 * Binary layout:
 * +----------------+------------------+---------------------+
 * | Length (2B)    | Method ID (2B)   | Correlation ID (8B) |
 * +----------------+------------------+---------------------+
 * | Trace ID (16B optional)  | Flags (1B)  | Payload (MYRA) |
 * +---------------------------------------------------------+
 */
public class MessageEnvelope {

    // Layout constants
    public static final int LENGTH_OFFSET = 0;
    public static final int METHOD_ID_OFFSET = 2;
    public static final int CORRELATION_ID_OFFSET = 4;
    public static final int TRACE_ID_OFFSET = 12;
    public static final int FLAGS_OFFSET = 28;
    public static final int PAYLOAD_OFFSET = 29;

    public static final int HEADER_SIZE = PAYLOAD_OFFSET;

    // VarHandles for efficient memory access
    private static final ValueLayout.OfShort LENGTH_LAYOUT = ValueLayout.JAVA_SHORT.withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfShort METHOD_ID_LAYOUT = ValueLayout.JAVA_SHORT.withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfLong CORRELATION_ID_LAYOUT = ValueLayout.JAVA_LONG
            .withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfByte FLAGS_LAYOUT = ValueLayout.JAVA_BYTE;

    // Message flags
    public static final byte FLAG_HAS_TRACE_ID = 0x01;
    public static final byte FLAG_IS_RESPONSE = 0x02;
    public static final byte FLAG_HAS_ERROR = 0x04;

    private final MemorySegment buffer;
    private final MemorySegmentPool pool;
    private final boolean pooled;

    public MessageEnvelope(MemorySegment buffer) {
        this.buffer = buffer;
        this.pool = null;
        this.pooled = false;
    }

    private MessageEnvelope(MemorySegment buffer, MemorySegmentPool pool) {
        this.buffer = buffer;
        this.pool = pool;
        this.pooled = true;
    }

    /**
     * Creates a MessageEnvelope using a pooled MemorySegment.
     * This is the zero-copy, GC-free way to create envelopes.
     *
     * @param payloadSize Size of the payload in bytes
     * @param pool        The MemorySegmentPool to use
     * @return A new MessageEnvelope backed by a pooled segment
     */
    public static MessageEnvelope allocate(int payloadSize, MemorySegmentPool pool) {
        int totalSize = HEADER_SIZE + payloadSize;
        MemorySegment buffer = pool.acquire(totalSize);
        return new MessageEnvelope(buffer, pool);
    }

    /**
     * Releases this envelope back to the pool if it was pool-allocated.
     * Call this when done with the envelope to enable zero-copy reuse.
     */
    public void release() {
        if (pooled && pool != null) {
            pool.release(buffer);
        }
    }

    // Getters
    public short getLength() {
        return buffer.get(LENGTH_LAYOUT, LENGTH_OFFSET);
    }

    public short getMethodId() {
        return buffer.get(METHOD_ID_LAYOUT, METHOD_ID_OFFSET);
    }

    public long getCorrelationId() {
        return buffer.get(CORRELATION_ID_LAYOUT, CORRELATION_ID_OFFSET);
    }

    public byte getFlags() {
        return buffer.get(FLAGS_LAYOUT, FLAGS_OFFSET);
    }

    public MemorySegment getPayload() {
        int payloadSize = getLength() - HEADER_SIZE;
        return buffer.asSlice(PAYLOAD_OFFSET, payloadSize);
    }

    // Setters
    public void setLength(short length) {
        buffer.set(LENGTH_LAYOUT, LENGTH_OFFSET, length);
    }

    public void setMethodId(short methodId) {
        buffer.set(METHOD_ID_LAYOUT, METHOD_ID_OFFSET, methodId);
    }

    public void setCorrelationId(long correlationId) {
        buffer.set(CORRELATION_ID_LAYOUT, CORRELATION_ID_OFFSET, correlationId);
    }

    public void setFlags(byte flags) {
        buffer.set(FLAGS_LAYOUT, FLAGS_OFFSET, flags);
    }

    // Utility methods
    public boolean hasTraceId() {
        return (getFlags() & FLAG_HAS_TRACE_ID) != 0;
    }

    public boolean isResponse() {
        return (getFlags() & FLAG_IS_RESPONSE) != 0;
    }

    public boolean hasError() {
        return (getFlags() & FLAG_HAS_ERROR) != 0;
    }

    public MemorySegment getBuffer() {
        return buffer;
    }

    public int getTotalSize() {
        return getLength();
    }
}