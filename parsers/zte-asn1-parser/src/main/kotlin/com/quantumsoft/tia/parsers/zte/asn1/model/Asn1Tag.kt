package com.quantumsoft.tia.parsers.zte.asn1.model

/**
 * Represents an ASN.1 tag with class, construction type, and tag number.
 * 
 * ASN.1 tags identify the type and encoding of ASN.1 elements. This class
 * provides a type-safe representation of ASN.1 tags with proper validation
 * and utility methods for tag manipulation and comparison.
 * 
 * ## ASN.1 Tag Structure
 * 
 * - **Class**: Universal, Application, Context-specific, or Private
 * - **Construction**: Primitive (simple value) or Constructed (complex structure)
 * - **Tag Number**: Identifies the specific type within the class
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
data class Asn1Tag(
    
    /**
     * ASN.1 tag class (Universal, Application, Context-specific, Private).
     */
    val tagClass: TagClass,
    
    /**
     * Whether this tag represents a constructed (complex) type.
     * - true: Constructed type (contains other elements)
     * - false: Primitive type (contains simple value)
     */
    val isConstructed: Boolean,
    
    /**
     * Tag number within the specified class.
     * For universal class, this corresponds to standard ASN.1 types.
     * For other classes, this is application or context-specific.
     */
    val tagNumber: Long
) {
    
    /**
     * Whether this tag represents a primitive (simple) type.
     */
    val isPrimitive: Boolean
        get() = !isConstructed
    
    /**
     * Estimated length of this tag when encoded in ASN.1 format.
     * Used for size calculations and memory allocation.
     */
    val encodedLength: Long
        get() = when {
            tagNumber &lt; 31 -&gt; 1L // Short form: single byte
            tagNumber &lt; 128 -&gt; 2L // Long form: tag + 1 byte number
            tagNumber &lt; 16384 -&gt; 3L // Long form: tag + 2 byte number
            tagNumber &lt; 2097152 -&gt; 4L // Long form: tag + 3 byte number
            else -&gt; 5L // Long form: tag + 4+ byte number
        }
    
    /**
     * Creates a string representation suitable for debugging and logging.
     * 
     * Format examples:
     * - "UNIVERSAL[2] PRIMITIVE" (for INTEGER)
     * - "CONTEXT[0] CONSTRUCTED" (for context-specific constructed)
     * - "APPLICATION[5] PRIMITIVE" (for application-specific primitive)
     * 
     * @return Human-readable tag representation
     */
    override fun toString(): String {
        val construction = if (isConstructed) "CONSTRUCTED" else "PRIMITIVE"
        return "${tagClass.name}[$tagNumber] $construction"
    }
    
    /**
     * Creates a compact string representation for use in logs and metrics.
     * 
     * Format examples:
     * - "U2" (Universal tag 2, primitive)
     * - "C0+" (Context tag 0, constructed)
     * - "A5" (Application tag 5, primitive)
     * 
     * @return Compact tag representation
     */
    fun toCompactString(): String {
        val classPrefix = when (tagClass) {
            TagClass.UNIVERSAL -&gt; "U"
            TagClass.APPLICATION -&gt; "A"
            TagClass.CONTEXT_SPECIFIC -&gt; "C"
            TagClass.PRIVATE -&gt; "P"
        }
        val constructedSuffix = if (isConstructed) "+" else ""
        return "$classPrefix$tagNumber$constructedSuffix"
    }
    
    /**
     * Checks if this tag matches another tag, ignoring construction type.
     * 
     * This is useful when searching for tags where the construction type
     * might vary but the class and number should match.
     * 
     * @param other Tag to compare against
     * @return true if class and tag number match
     */
    fun matchesIgnoringConstruction(other: Asn1Tag): Boolean {
        return tagClass == other.tagClass && tagNumber == other.tagNumber
    }
    
    /**
     * Creates a copy of this tag with the specified construction type.
     * 
     * @param constructed Whether the new tag should be constructed
     * @return New tag with the same class and number but different construction
     */
    fun withConstruction(constructed: Boolean): Asn1Tag {
        return copy(isConstructed = constructed)
    }
    
    companion object {
        
        // === Universal Class Standard Tags ===
        
        /** Universal tag for BOOLEAN type */
        val BOOLEAN = Asn1Tag(TagClass.UNIVERSAL, false, 1)
        
        /** Universal tag for INTEGER type */
        val INTEGER = Asn1Tag(TagClass.UNIVERSAL, false, 2)
        
        /** Universal tag for BIT STRING type */
        val BIT_STRING = Asn1Tag(TagClass.UNIVERSAL, false, 3)
        
        /** Universal tag for OCTET STRING type */
        val OCTET_STRING = Asn1Tag(TagClass.UNIVERSAL, false, 4)
        
        /** Universal tag for NULL type */
        val NULL = Asn1Tag(TagClass.UNIVERSAL, false, 5)
        
        /** Universal tag for OBJECT IDENTIFIER type */
        val OBJECT_IDENTIFIER = Asn1Tag(TagClass.UNIVERSAL, false, 6)
        
        /** Universal tag for ENUMERATED type */
        val ENUMERATED = Asn1Tag(TagClass.UNIVERSAL, false, 10)
        
        /** Universal tag for UTF8String type */
        val UTF8_STRING = Asn1Tag(TagClass.UNIVERSAL, false, 12)
        
        /** Universal tag for SEQUENCE type (constructed) */
        val SEQUENCE = Asn1Tag(TagClass.UNIVERSAL, true, 16)
        
        /** Universal tag for SET type (constructed) */
        val SET = Asn1Tag(TagClass.UNIVERSAL, true, 17)
        
        /** Universal tag for PrintableString type */
        val PRINTABLE_STRING = Asn1Tag(TagClass.UNIVERSAL, false, 19)
        
        /** Universal tag for VisibleString type */
        val VISIBLE_STRING = Asn1Tag(TagClass.UNIVERSAL, false, 26)
        
        /** Universal tag for GeneralizedTime type */
        val GENERALIZED_TIME = Asn1Tag(TagClass.UNIVERSAL, false, 24)
        
        /** Universal tag for UTCTime type */
        val UTC_TIME = Asn1Tag(TagClass.UNIVERSAL, false, 23)
        
        // === ZTE-Specific Context Tags (commonly used) ===
        
        /** ZTE context tag 0 (typically for call ID) */
        fun zteContext(tagNumber: Long, constructed: Boolean = false): Asn1Tag {
            return Asn1Tag(TagClass.CONTEXT_SPECIFIC, constructed, tagNumber)
        }
        
        /** ZTE application tag */
        fun zteApplication(tagNumber: Long, constructed: Boolean = false): Asn1Tag {
            return Asn1Tag(TagClass.APPLICATION, constructed, tagNumber)
        }
        
        /**
         * Creates a tag from raw ASN.1 tag byte.
         * 
         * This method parses the first byte of an ASN.1 tag to extract
         * class, construction, and tag number information.
         * 
         * @param tagByte Raw ASN.1 tag byte
         * @return Parsed ASN.1 tag
         * @throws IllegalArgumentException if tag byte format is invalid
         */
        fun fromTagByte(tagByte: Byte): Asn1Tag {
            val tagInt = tagByte.toInt() and 0xFF
            
            val tagClass = when ((tagInt and 0xC0) ushr 6) {
                0 -&gt; TagClass.UNIVERSAL
                1 -&gt; TagClass.APPLICATION
                2 -&gt; TagClass.CONTEXT_SPECIFIC
                3 -&gt; TagClass.PRIVATE
                else -&gt; throw IllegalArgumentException("Invalid tag class in byte: $tagByte")
            }
            
            val isConstructed = (tagInt and 0x20) != 0
            val tagNumber = (tagInt and 0x1F).toLong()
            
            return Asn1Tag(tagClass, isConstructed, tagNumber)
        }
        
        /**
         * Creates a tag from multiple bytes for long-form tag encoding.
         * 
         * @param tagBytes Array of tag bytes (first byte + continuation bytes)
         * @return Parsed ASN.1 tag
         * @throws IllegalArgumentException if tag format is invalid
         */
        fun fromTagBytes(tagBytes: ByteArray): Asn1Tag {
            require(tagBytes.isNotEmpty()) { "Tag bytes cannot be empty" }
            
            val firstByte = tagBytes[0].toInt() and 0xFF
            
            val tagClass = when ((firstByte and 0xC0) ushr 6) {
                0 -&gt; TagClass.UNIVERSAL
                1 -&gt; TagClass.APPLICATION
                2 -&gt; TagClass.CONTEXT_SPECIFIC
                3 -&gt; TagClass.PRIVATE
                else -&gt; throw IllegalArgumentException("Invalid tag class")
            }
            
            val isConstructed = (firstByte and 0x20) != 0
            
            val tagNumber = if ((firstByte and 0x1F) &lt; 31) {
                // Short form: tag number is in first byte
                (firstByte and 0x1F).toLong()
            } else {
                // Long form: tag number in subsequent bytes
                require(tagBytes.size &gt; 1) { "Long form tag requires additional bytes" }
                
                var number = 0L
                for (i in 1 until tagBytes.size) {
                    val byte = tagBytes[i].toInt() and 0xFF
                    number = (number shl 7) or (byte and 0x7F).toLong()
                    
                    // Check for continuation bit (0x80)
                    if ((byte and 0x80) == 0) break
                }
                number
            }
            
            return Asn1Tag(tagClass, isConstructed, tagNumber)
        }
    }
}

/**
 * ASN.1 tag classes as defined in the ASN.1 standard.
 */
enum class TagClass {
    /**
     * Universal tags defined by ASN.1 standard.
     * These are the standard ASN.1 types like INTEGER, SEQUENCE, etc.
     */
    UNIVERSAL,
    
    /**
     * Application-specific tags defined by the application.
     * Used by specific protocols or applications for their own types.
     */
    APPLICATION,
    
    /**
     * Context-specific tags for disambiguation within a structure.
     * Used to distinguish between different uses of the same type.
     */
    CONTEXT_SPECIFIC,
    
    /**
     * Private tags for organization-specific use.
     * Used by organizations for their proprietary extensions.
     */
    PRIVATE
}