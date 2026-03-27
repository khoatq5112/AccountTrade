package com.group3.accounttrade.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IdEncoderTest {

    @Autowired
    private IdEncoder idEncoder;

    @Test
    void testEncodeDecodeInteger() {
        Integer originalId = 123;
        String encoded = idEncoder.encode(originalId);
        Integer decoded = idEncoder.decode(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testEncodeDecodeLong() {
        Long originalId = 123456L;
        String encoded = idEncoder.encode(originalId);
        Long decoded = idEncoder.decodeLong(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testEncodeDecodePostId() {
        Integer originalId = 123;
        String encoded = idEncoder.encodePostId(originalId);
        assertTrue(encoded.startsWith("post_"));
        Integer decoded = idEncoder.decodePostId(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testEncodeDecodeOrderId() {
        Long originalId = 123456L;
        String encoded = idEncoder.encodeOrderId(originalId);
        assertTrue(encoded.startsWith("order_"));
        Long decoded = idEncoder.decodeOrderId(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testEncodeDecodeDisputeId() {
        Long originalId = 789L;
        String encoded = idEncoder.encodeDisputeId(originalId);
        assertTrue(encoded.startsWith("dispute_"));
        Long decoded = idEncoder.decodeDisputeId(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testEncodeDecodeCredentialId() {
        Integer originalId = 456;
        String encoded = idEncoder.encodeCredentialId(originalId);
        assertTrue(encoded.startsWith("cred_"));
        Integer decoded = idEncoder.decodeCredentialId(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testEncodeDecodeCategoryId() {
        Integer originalId = 789;
        String encoded = idEncoder.encodeCategoryId(originalId);
        assertTrue(encoded.startsWith("cat_"));
        Integer decoded = idEncoder.decodeCategoryId(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeNumericPostId_BackwardCompatibility() {
        Integer originalId = 123;
        Integer decoded = idEncoder.decodePostId("123");
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeNumericOrderId_BackwardCompatibility() {
        Long originalId = 123456L;
        Long decoded = idEncoder.decodeOrderId("123456");
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeNumericDisputeId_BackwardCompatibility() {
        Long originalId = 789L;
        Long decoded = idEncoder.decodeDisputeId("789");
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeNumericCredentialId_BackwardCompatibility() {
        Integer originalId = 456;
        Integer decoded = idEncoder.decodeCredentialId("456");
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeNumericCategoryId_BackwardCompatibility() {
        Integer originalId = 789;
        Integer decoded = idEncoder.decodeCategoryId("789");
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeNullId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decode(null));
    }

    @Test
    void testDecodeEmptyString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decode(""));
    }

    @Test
    void testDecodeBlankString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decode("   "));
    }

    @Test
    void testDecodeInvalidFormat_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decode("invalid"));
    }

    @Test
    void testEncodeNullInteger_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.encode((Integer) null));
    }

    @Test
    void testEncodeNullLong_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.encode((Long) null));
    }

    @Test
    void testDecodeNegativeValue_ThrowsException() {
        // This test verifies that negative values are rejected
        // Since we can't easily create a negative hashid, we test the validation logic
        // by checking that the decode method validates for positive values
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodePostId("-1"));
    }

    @Test
    void testDecodeLongNegativeValue_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeOrderId("-1"));
    }

    @Test
    void testDecodeZeroValue() {
        Integer originalId = 0;
        String encoded = idEncoder.encode(originalId);
        Integer decoded = idEncoder.decode(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeLongZeroValue() {
        Long originalId = 0L;
        String encoded = idEncoder.encode(originalId);
        Long decoded = idEncoder.decodeLong(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodePostIdNull_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodePostId(null));
    }

    @Test
    void testDecodeOrderIdNull_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeOrderId(null));
    }

    @Test
    void testDecodeDisputeIdNull_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeDisputeId(null));
    }

    @Test
    void testDecodeCredentialIdNull_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeCredentialId(null));
    }

    @Test
    void testDecodeCategoryIdNull_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeCategoryId(null));
    }

    @Test
    void testDecodePostIdEmptyString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodePostId(""));
    }

    @Test
    void testDecodeOrderIdEmptyString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeOrderId(""));
    }

    @Test
    void testDecodeDisputeIdEmptyString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeDisputeId(""));
    }

    @Test
    void testDecodeCredentialIdEmptyString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeCredentialId(""));
    }

    @Test
    void testDecodeCategoryIdEmptyString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeCategoryId(""));
    }

    @Test
    void testDecodePostIdBlankString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodePostId("   "));
    }

    @Test
    void testDecodeOrderIdBlankString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeOrderId("   "));
    }

    @Test
    void testDecodeDisputeIdBlankString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeDisputeId("   "));
    }

    @Test
    void testDecodeCredentialIdBlankString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeCredentialId("   "));
    }

    @Test
    void testDecodeCategoryIdBlankString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> idEncoder.decodeCategoryId("   "));
    }

    @Test
    void testEncodeDecodeLargeInteger() {
        Integer originalId = Integer.MAX_VALUE;
        String encoded = idEncoder.encode(originalId);
        Integer decoded = idEncoder.decode(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testEncodeDecodeLargeLong() {
        Long originalId = 2147483647L; // Within Integer range for compatibility
        String encoded = idEncoder.encode(originalId);
        Long decoded = idEncoder.decodeLong(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testEncodedIdIsNotNumeric() {
        Integer originalId = 123;
        String encoded = idEncoder.encode(originalId);
        assertFalse(encoded.matches("\\d+"), "Encoded ID should not be purely numeric");
    }

    @Test
    void testEncodedPostIdContainsPrefix() {
        Integer originalId = 123;
        String encoded = idEncoder.encodePostId(originalId);
        assertTrue(encoded.startsWith("post_"), "Encoded post ID should start with 'post_'");
    }

    @Test
    void testEncodedOrderIdContainsPrefix() {
        Long originalId = 123456L;
        String encoded = idEncoder.encodeOrderId(originalId);
        assertTrue(encoded.startsWith("order_"), "Encoded order ID should start with 'order_'");
    }

    @Test
    void testEncodedDisputeIdContainsPrefix() {
        Long originalId = 789L;
        String encoded = idEncoder.encodeDisputeId(originalId);
        assertTrue(encoded.startsWith("dispute_"), "Encoded dispute ID should start with 'dispute_'");
    }

    @Test
    void testEncodedCredentialIdContainsPrefix() {
        Integer originalId = 456;
        String encoded = idEncoder.encodeCredentialId(originalId);
        assertTrue(encoded.startsWith("cred_"), "Encoded credential ID should start with 'cred_'");
    }

    @Test
    void testEncodedCategoryIdContainsPrefix() {
        Integer originalId = 789;
        String encoded = idEncoder.encodeCategoryId(originalId);
        assertTrue(encoded.startsWith("cat_"), "Encoded category ID should start with 'cat_'");
    }

    @Test
    void testDifferentIdsProduceDifferentEncodings() {
        Integer id1 = 123;
        Integer id2 = 456;
        String encoded1 = idEncoder.encode(id1);
        String encoded2 = idEncoder.encode(id2);
        assertNotEquals(encoded1, encoded2, "Different IDs should produce different encodings");
    }

    @Test
    void testSameIdProducesSameEncoding() {
        Integer id = 123;
        String encoded1 = idEncoder.encode(id);
        String encoded2 = idEncoder.encode(id);
        assertEquals(encoded1, encoded2, "Same ID should produce same encoding");
    }

    @Test
    void testDecodePostIdWithPrefix() {
        Integer originalId = 123;
        String encoded = idEncoder.encodePostId(originalId);
        Integer decoded = idEncoder.decodePostId(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeOrderIdWithPrefix() {
        Long originalId = 123456L;
        String encoded = idEncoder.encodeOrderId(originalId);
        Long decoded = idEncoder.decodeOrderId(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeDisputeIdWithPrefix() {
        Long originalId = 789L;
        String encoded = idEncoder.encodeDisputeId(originalId);
        Long decoded = idEncoder.decodeDisputeId(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeCredentialIdWithPrefix() {
        Integer originalId = 456;
        String encoded = idEncoder.encodeCredentialId(originalId);
        Integer decoded = idEncoder.decodeCredentialId(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeCategoryIdWithPrefix() {
        Integer originalId = 789;
        String encoded = idEncoder.encodeCategoryId(originalId);
        Integer decoded = idEncoder.decodeCategoryId(encoded);
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodePostIdWithoutPrefix_BackwardCompatibility() {
        Integer originalId = 123;
        Integer decoded = idEncoder.decodePostId("123");
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeOrderIdWithoutPrefix_BackwardCompatibility() {
        Long originalId = 123456L;
        Long decoded = idEncoder.decodeOrderId("123456");
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeDisputeIdWithoutPrefix_BackwardCompatibility() {
        Long originalId = 789L;
        Long decoded = idEncoder.decodeDisputeId("789");
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeCredentialIdWithoutPrefix_BackwardCompatibility() {
        Integer originalId = 456;
        Integer decoded = idEncoder.decodeCredentialId("456");
        assertEquals(originalId, decoded);
    }

    @Test
    void testDecodeCategoryIdWithoutPrefix_BackwardCompatibility() {
        Integer originalId = 789;
        Integer decoded = idEncoder.decodeCategoryId("789");
        assertEquals(originalId, decoded);
    }
}
