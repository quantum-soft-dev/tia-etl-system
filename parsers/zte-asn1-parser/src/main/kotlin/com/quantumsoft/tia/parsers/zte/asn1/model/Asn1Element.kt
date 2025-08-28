package com.quantumsoft.tia.parsers.zte.asn1.model

/**
 * Represents an ASN.1 element in the decoded structure tree.
 * 
 * This class models a single ASN.1 element with its tag, length, and content
 * information. It supports both primitive and constructed ASN.1 types and
 * maintains the hierarchical structure of complex ASN.1 data.
 * 
 * ## Design Principles
 * 
 * - **Immutability**: Elements are immutable once created
 * - **Type Safety**: Clear distinction between primitive and constructed types
 * - **Memory Efficiency**: Lazy evaluation of child elements where possible
 * - **Navigation**: Support for tree traversal and element lookup
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
data class Asn1Element(
    
    /**
     * ASN.1 tag information for this element.
     */
    val tag: Asn1Tag,
    
    /**
     * Length of the content in bytes.
     * For constructed types, this includes all child element content.
     */
    val contentLength: Long,
    
    /**
     * Raw content bytes for primitive elements.
     * Null for constructed elements (use children instead).
     */
    val content: ByteArray? = null,
    
    /**
     * Child elements for constructed types.
     * Empty list for primitive elements.
     */
    val children: List&lt;Asn1Element&gt; = emptyList(),
    
    /**
     * Byte offset of this element within the original ASN.1 data.
     * Used for error reporting and debugging.
     */
    val offset: Long = 0L
) {
    
    /**
     * Indicates whether this is a constructed (complex) element.
     */
    val isConstructed: Boolean
        get() = tag.isConstructed
    
    /**
     * Indicates whether this is a primitive (simple) element.
     */
    val isPrimitive: Boolean
        get() = !tag.isConstructed
    
    /**
     * Gets the total size of this element including tag and length encoding.
     */
    val totalSize: Long
        get() = tag.encodedLength + lengthEncodingSize + contentLength
    
    /**
     * Estimated size of the length encoding based on content length.
     */
    private val lengthEncodingSize: Long
        get() = when {
            contentLength &lt; 128 -&gt; 1L
            contentLength &lt; 256 -&gt; 2L
            contentLength &lt; 65536 -&gt; 3L
            contentLength &lt; 16777216 -&gt; 4L
            else -&gt; 5L
        }
    
    /**
     * Finds the first child element with the specified tag.
     * 
     * @param searchTag Tag to search for
     * @return First matching child element or null if not found
     */
    fun findChildByTag(searchTag: Asn1Tag): Asn1Element? {
        return children.find { it.tag == searchTag }
    }
    
    /**
     * Finds all child elements with the specified tag.
     * 
     * @param searchTag Tag to search for
     * @return List of matching child elements
     */
    fun findChildrenByTag(searchTag: Asn1Tag): List&lt;Asn1Element&gt; {
        return children.filter { it.tag == searchTag }
    }
    
    /**
     * Finds the first descendant element with the specified tag (recursive search).
     * 
     * @param searchTag Tag to search for
     * @return First matching descendant element or null if not found
     */
    fun findDescendantByTag(searchTag: Asn1Tag): Asn1Element? {
        // Check direct children first
        val directMatch = findChildByTag(searchTag)
        if (directMatch != null) return directMatch
        
        // Recursively search in constructed children
        for (child in children) {
            if (child.isConstructed) {
                val descendantMatch = child.findDescendantByTag(searchTag)
                if (descendantMatch != null) return descendantMatch
            }
        }
        
        return null
    }
    
    /**
     * Gets the content as a string using UTF-8 encoding.
     * Only applicable to primitive elements with string content.
     * 
     * @return String content or empty string if not applicable
     */
    fun getContentAsString(): String {
        return content?.toString(Charsets.UTF_8) ?: ""
    }
    
    /**
     * Gets the content as a long integer.
     * Only applicable to primitive elements with numeric content.
     * 
     * @return Long value or 0 if not applicable or invalid
     */
    fun getContentAsLong(): Long {
        if (content == null || content.isEmpty()) return 0L
        
        return try {
            var result = 0L
            for (byte in content) {
                result = (result shl 8) or (byte.toInt() and 0xFF).toLong()
            }
            result
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Gets the content as a byte array.
     * Returns a copy to maintain immutability.
     * 
     * @return Copy of content bytes or empty array if not applicable
     */
    fun getContentAsBytes(): ByteArray {
        return content?.copyOf() ?: byteArrayOf()
    }
    
    /**
     * Validates the structure of this element and its children.
     * 
     * @return List of validation issues, empty if valid
     */
    fun validateStructure(): List&lt;String&gt; {
        val issues = mutableListOf&lt;String&gt;()
        
        // Validate tag consistency
        if (tag.isConstructed && content != null) {
            issues.add("Constructed element should not have primitive content")
        }
        
        if (!tag.isConstructed && children.isNotEmpty()) {
            issues.add("Primitive element should not have children")
        }
        
        // Validate content length consistency
        if (isPrimitive && content != null && content.size.toLong() != contentLength) {
            issues.add("Content length mismatch: declared $contentLength, actual ${content.size}")
        }
        
        if (isConstructed) {
            val childrenTotalSize = children.sumOf { it.totalSize }
            if (childrenTotalSize != contentLength) {
                issues.add("Children total size mismatch: declared $contentLength, calculated $childrenTotalSize")
            }
        }
        
        // Recursively validate children
        children.forEach { child -&gt;
            issues.addAll(child.validateStructure().map { "Child[$child.tag]: $it" })
        }
        
        return issues
    }
    
    /**
     * Creates a tree representation of this element for debugging.
     * 
     * @param indent Current indentation level
     * @return Multi-line string representation of the element tree
     */
    fun toTreeString(indent: String = ""): String {
        val sb = StringBuilder()
        sb.append("$indent$tag")
        
        if (isPrimitive) {
            sb.append(" = ")
            if (content != null && content.size &lt;= 32) {
                // Show content as hex for small primitive values
                sb.append(content.joinToString("") { "%02x".format(it) })
            } else {
                sb.append("[${contentLength} bytes]")
            }
        } else {
            sb.append(" {${children.size} children}")
        }
        
        sb.appendLine()
        
        // Add children with increased indentation
        children.forEach { child -&gt;
            sb.append(child.toTreeString("$indent  "))
        }
        
        return sb.toString()
    }
    
    /**
     * Creates a summary string for logging and debugging.
     * 
     * @return Concise element summary
     */
    fun toSummary(): String {
        return if (isPrimitive) {
            "ASN1[$tag, ${contentLength}B primitive]"
        } else {
            "ASN1[$tag, ${children.size} children, ${contentLength}B total]"
        }
    }
    
    // Custom equals and hashCode to handle ByteArray properly
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Asn1Element
        
        if (tag != other.tag) return false
        if (contentLength != other.contentLength) return false
        if (content != null && other.content != null) {
            if (!content.contentEquals(other.content)) return false
        } else if (content != other.content) return false
        if (children != other.children) return false
        if (offset != other.offset) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + contentLength.hashCode()
        result = 31 * result + (content?.contentHashCode() ?: 0)
        result = 31 * result + children.hashCode()
        result = 31 * result + offset.hashCode()
        return result
    }
}