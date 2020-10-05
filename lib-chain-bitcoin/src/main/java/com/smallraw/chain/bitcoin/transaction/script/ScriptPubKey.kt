package com.smallraw.chain.bitcoin.transaction.script

import com.smallraw.chain.bitcoin.Bitcoin
import com.smallraw.chain.lib.crypto.Base58
import com.smallraw.chain.lib.crypto.DEREncode
import com.smallraw.chain.lib.crypto.Ripemd160
import com.smallraw.chain.bitcoin.network.BaseNetwork
import com.smallraw.chain.bitcoin.stream.ByteReader
import com.smallraw.chain.bitcoin.stream.ByteWriter
import java.io.EOFException

class ScriptInputPubKey : ScriptInput {
    companion object {
        @Throws(ScriptParsingException::class)
        fun isScriptInputPubKey(chunks: List<Chunk>): Boolean {
            return try {
                if (chunks.size != 1) {
                    return false
                }

                // Verify that the chunk contains two DER encoded BigIntegers
                val reader = ByteReader(chunks[0].toBytes())

                // Read tag, must be 0x30
                if (reader.readByte() and 0xFF != 0x30) {
                    return false
                }

                // Read total length as a byte, standard inputs never get longer than
                // this
                val length = reader.readByte() as Int and 0xFF
                if (reader.available() < length) {
                    return false
                }

                // Read first type, must be 0x02
                if (reader.readByte() and 0xFF != 0x02) {
                    return false
                }

                // Read first length
                val length1 = reader.readByte() and 0xFF
                if (reader.available() < length1) {
                    return false
                }
                reader.skip(length1.toLong())

                // Read second type, must be 0x02
                if (reader.readByte() and 0xFF != 0x02) {
                    return false
                }

                // Read second length
                val length2 = reader.readByte() and 0xFF
                if (reader.available() < length2) {
                    return false
                }
                reader.skip(length2.toLong())

                // Make sure that we have 0x01 at the end
                if (reader.available() !== 1) {
                    return false
                }
                if (reader.readByte() and 0xFF != 0x01) {
                    false
                } else true
            } catch (e: EOFException) {
                throw ScriptParsingException("Unable to parse " + ScriptInputPubKey::class.java.simpleName)
            }
        }
    }

    private val _signature: ByteArray

    constructor(chunks: List<Chunk>, scriptBytes: ByteArray) : super(scriptBytes) {
        _signature = chunks[0].toBytes()
    }

    /**
     * Get the signature of this input.
     */
    fun getSignature(): ByteArray {
        return _signature
    }

    override fun getUnmalleableBytes(): ByteArray? {
        val bytes: ByteArray = DEREncode.sigToDer(_signature) ?: return null

        val writer = ByteWriter()
        writer.writeBytes(bytes.copyOfRange(0, 32))
        writer.writeBytes(bytes.copyOfRange(32, bytes.size))
        return writer.toByteArray()
    }
}

class ScriptOutputPubkey : ScriptOutput {
    companion object {
        fun isScriptOutputPubkey(chunks: List<Chunk>): Boolean {
            if (chunks.size != 2) {
                return false
            }
            return if (!isOP(chunks[1], OP_CHECKSIG)) {
                false
            } else true
        }
    }

    private val _publicKeyBytes: ByteArray

    constructor(chunks: List<Chunk>, scriptBytes: ByteArray) : super(scriptBytes) {
        _publicKeyBytes = chunks[0].toBytes()
    }

    /**
     * Get the public key bytes that this output is for.
     *
     * @return The public key bytes that this output is for.
     */
    fun getPublicKeyBytes(): ByteArray {
        return _publicKeyBytes
    }

    override fun getAddress(network: BaseNetwork): Bitcoin.Address {
        val addressBytes = byteArrayOf(network.addressVersion.toByte()) + getAddressBytes()
        return Bitcoin.LegacyAddress(
            Base58.encodeCheck(addressBytes),
            addressBytes,
            Bitcoin.Address.AddressType.P2SH
        )
    }

    override fun getAddressBytes(): ByteArray {
        return Ripemd160.hash160(getPublicKeyBytes())
    }
}