package com.thruxion.app.utils

/**
 * Encodes and decodes binary data into a string of zero-width Unicode characters.
 * Inspired by Oversec's invisible encoding logic.
 */
object ZeroWidthEncoder {
    // Zero-width characters used for bit mapping
    private const val ZW_SPACE = '\u200B'    // Bit 0
    private const val ZW_NON_JOINER = '\u200C' // Bit 1
    private const val ZW_JOINER = '\u200D'     // Start marker
    private const val ZW_LTR_MARK = '\u200E'   // End marker

    /**
     * Hides a byte array within a decoy string using zero-width characters.
     */
    fun encode(data: ByteArray, decoy: String = ""): String {
        val sb = StringBuilder()
        sb.append(ZW_JOINER) // Start marker

        for (byte in data) {
            val iByte = byte.toInt() and 0xFF
            for (i in 7 downTo 0) {
                val bit = (iByte shr i) and 1
                if (bit == 0) {
                    sb.append(ZW_SPACE)
                } else {
                    sb.append(ZW_NON_JOINER)
                }
            }
        }

        sb.append(ZW_LTR_MARK) // End marker
        sb.append(decoy)
        return sb.toString()
    }

    /**
     * Extracts a hidden byte array from a string containing zero-width characters.
     * Returns null if no valid hidden data is found.
     */
    fun decode(input: String): ByteArray? {
        val startIndex = input.indexOf(ZW_JOINER)
        val endIndex = input.indexOf(ZW_LTR_MARK)

        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
            return null
        }

        val encodedPart = input.substring(startIndex + 1, endIndex)
        val bits = mutableListOf<Int>()

        for (char in encodedPart) {
            when (char) {
                ZW_SPACE -> bits.add(0)
                ZW_NON_JOINER -> bits.add(1)
            }
        }

        if (bits.size % 8 != 0) return null

        val result = ByteArray(bits.size / 8)
        for (i in result.indices) {
            var byte = 0
            for (j in 0..7) {
                byte = (byte shl 1) or bits[i * 8 + j]
            }
            result[i] = byte.toByte()
        }

        return result
    }

    /**
     * Checks if a string contains Oversec-style hidden data.
     */
    fun hasHiddenData(input: String): Boolean {
        return input.contains(ZW_JOINER) && input.contains(ZW_LTR_MARK)
    }
    
    /**
     * Removes the hidden data from a string, returning only the decoy text.
     */
    fun removeHiddenData(input: String): String {
        val startIndex = input.indexOf(ZW_JOINER)
        val endIndex = input.indexOf(ZW_LTR_MARK)
        if (startIndex == -1 || endIndex == -1) return input
        
        return input.removeRange(startIndex, endIndex + 1)
    }
}
