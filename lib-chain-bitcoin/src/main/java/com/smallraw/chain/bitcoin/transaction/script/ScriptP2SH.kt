package com.smallraw.chain.bitcoin.transaction.script

import com.smallraw.chain.bitcoin.Bitcoin
import com.smallraw.chain.bitcoincore.address.Address
import com.smallraw.chain.bitcoincore.address.P2SHAddress
import com.smallraw.chain.bitcoincore.execptions.ScriptParsingException
import com.smallraw.chain.bitcoincore.network.BaseNetwork
import com.smallraw.chain.bitcoincore.script.Chunk
import com.smallraw.chain.bitcoincore.script.OP_0
import com.smallraw.chain.bitcoincore.script.OP_CHECKMULTISIG
import com.smallraw.chain.bitcoincore.script.OP_EQUAL
import com.smallraw.chain.bitcoincore.script.OP_HASH160
import com.smallraw.chain.bitcoincore.script.OpCodes
import com.smallraw.chain.bitcoincore.script.ScriptChunk
import com.smallraw.chain.bitcoincore.script.isOP
import com.smallraw.chain.bitcoincore.script.toScriptBytes
import com.smallraw.chain.bitcoincore.stream.BitcoinOutputStream
import com.smallraw.chain.lib.core.crypto.Base58
import com.smallraw.chain.lib.core.crypto.DEREncode
import com.smallraw.chain.lib.core.crypto.Ripemd160

class ScriptInputP2SHMultisig : ScriptInput {
    companion object {
        @Throws(ScriptParsingException::class)
        fun isScriptInputP2SHMultisig(chunks: List<ScriptChunk>): Boolean {
            if (chunks.size < 3) {
                return false
            }
            val scriptChunks: List<ScriptChunk>
            try {
                scriptChunks = parseChunks(chunks[chunks.size - 1].toBytes())
            } catch (e: ScriptParsingException) {
                return false
            }
            if (scriptChunks.size < 4) {
                return false
            }
            //starts with an extra op cause of a bug in OP_CHECKMULTISIG
            if (!chunks[0].isOP(OP_0)) {
                return false
            }
            //last chunk in embedded script has to be CHECKMULTISIG
            if (!scriptChunks[scriptChunks.size - 1].isOP(OP_CHECKMULTISIG)) {
                return false
            }
            //first and second last chunk must have length 1, because they should be m and n values
            if (scriptChunks[0].toBytes().size != 1 || scriptChunks[scriptChunks.size - 2].toBytes().size != 1) {
                return false
            }
            //check for the m and n values
            try {
                val m: Int = OpCodes.opToIntValue(scriptChunks[0])
                val n: Int = OpCodes.opToIntValue(scriptChunks[scriptChunks.size - 2])
                if (n < 1 || n > 16) {
                    return false
                }
                if (m > n || m < 1 || m > 16) {
                    return false
                }
                //check that number of pubkeys matches n
                if (n != scriptChunks.size - 3) {
                    return false
                }
            } catch (ex: IllegalStateException) {
                //should hopefully not happen, since we check length before evaluating m and n
                //but its better to not risk BQS stopping in case something weird happens
                return false
            }
            return true
        }
    }

    private var signatures: ArrayList<ByteArray>
    private var m = 0
    private var n = 0
    private var pubKeys: ArrayList<ByteArray>
    private val scriptHash: ByteArray
    private val embeddedScript: ScriptChunk

    constructor(chunks: List<ScriptChunk>, scriptBytes: ByteArray) : super(scriptBytes) {
        //all but the first and last chunks are signatures, last chunk is the script
        signatures = ArrayList(chunks.size - 1)
        signatures.addAll(chunks.subList(1, chunks.size - 1).map { it.toBytes() })
        embeddedScript = chunks[chunks.size - 1]
        val scriptChunks: List<ScriptChunk> = parseChunks(embeddedScript.toBytes())
        scriptHash = Ripemd160.hash160(chunks[chunks.size - 1].toBytes())
        //the number of signatures needed
        m = OpCodes.opToIntValue(scriptChunks[0])
        //the total number of possible signing keys
        n = OpCodes.opToIntValue(scriptChunks[scriptChunks.size - 2])
        //collecting the pubkeys
        pubKeys = ArrayList(n)
        pubKeys.addAll(scriptChunks.subList(1, n + 1).map { it.toBytes() })
    }

    constructor(signatures: Bitcoin.MultiSignature, redeemScript: ByteArray) : super(
        signatures.signature() + listOf(Chunk(redeemScript)).toScriptBytes()
    ) {
        //all but the first and last chunks are signatures, last chunk is the script
        this.signatures = ArrayList(signatures.signSize())
        this.signatures.addAll(signatures.getSignatures().map { it.signature() })
        embeddedScript = Chunk(redeemScript)
        val scriptChunks: List<ScriptChunk> = parseChunks(embeddedScript.toBytes())
        scriptHash = Ripemd160.hash160(chunks[chunks.size - 1].toBytes())
        //the number of signatures needed
        m = OpCodes.opToIntValue(scriptChunks[0])
        //the total number of possible signing keys
        n = OpCodes.opToIntValue(scriptChunks[scriptChunks.size - 2])
        //collecting the pubkeys
        pubKeys = ArrayList(n)
        pubKeys.addAll(scriptChunks.subList(1, n + 1).map { it.toBytes() })
    }

    fun getPubKeys(): List<ByteArray?>? {
        return ArrayList(pubKeys)
    }

    fun getSignatures(): List<ByteArray?>? {
        return ArrayList(signatures)
    }

    fun getScriptHash(): ByteArray? {
        return scriptHash.copyOf()
    }

    fun getEmbeddedScript(): ByteArray {
        return embeddedScript.toBytes().copyOf()
    }

    fun getSigNumberNeeded(): Int {
        return m
    }

    override fun getUnmalleableBytes(): ByteArray? {
        val writer = BitcoinOutputStream()
        for (sig in signatures) {
            val bytes: ByteArray = DEREncode.sigToDer(sig) ?: return null

            writer.writeBytes(bytes.copyOfRange(0, 32))
            writer.writeBytes(bytes.copyOfRange(32, bytes.size))
        }
        return writer.toByteArray()
    }
}

class ScriptOutputP2SH : ScriptOutput {
    companion object {
        fun isScriptOutputP2SH(chunks: List<ScriptChunk>): Boolean {
            if (chunks.size != 3) {
                return false
            }
            if (!chunks[0].isOP(OP_HASH160)) {
                return false
            }
            if (chunks[1].toBytes().size != 20) {
                return false
            }
            return if (!chunks[2].isOP(OP_EQUAL)) {
                false
            } else true
        }
    }

    private val p2shAddressBytes: ByteArray

    constructor(chunks: List<ScriptChunk>, scriptBytes: ByteArray) : super(scriptBytes) {
        p2shAddressBytes = chunks[1].toBytes()
    }

    constructor(addressBytes: ByteArray) : super(
        listOf(
            Chunk(OP_HASH160),
            Chunk(addressBytes),
            Chunk(OP_EQUAL)
        )
    ) {
        p2shAddressBytes = addressBytes
    }

    override fun getAddress(network: BaseNetwork): Address {
        val addressBytes = ByteArray(21)
        addressBytes[0] = (network.addressScriptVersion and 0xFF).toByte()
        System.arraycopy(p2shAddressBytes, 0, addressBytes, 1, 20)
        return P2SHAddress(
            addressBytes,
            network.addressScriptVersion,
            Base58.encodeCheck(addressBytes),
        )
    }

    override fun getAddressBytes() = p2shAddressBytes
}