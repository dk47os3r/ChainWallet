package com.smallraw.chain.lib

import android.util.Log
import com.smallraw.chain.lib.crypto.Base58
import com.smallraw.chain.lib.crypto.Ripemd160
import com.smallraw.chain.lib.crypto.Sha256
import com.smallraw.chain.lib.extensions.toHex
import java.security.PublicKey
import java.util.*

class BitcoinAddress(
    private val publicKey: PublicKey,
    private val testNet: Boolean
) : Address {
    companion object {
        private const val TEST_NET_ADDRESS_SUFFIX = 0x6f.toByte()
        private const val MAIN_NET_ADDRESS_SUFFIX = 0x00.toByte()
    }

    override fun getFormat(): String {
        return Base58.encode(getAddress()) ?: ""
    }

    override fun getAddress(): ByteArray {
        Log.e("Ripemd1601",publicKey.encoded.toHex())
        val hashedPublicKey = Ripemd160.hash160(publicKey.encoded) ?: return byteArrayOf()
        Log.e("Ripemd160",hashedPublicKey.toHex())
        val addressBytes = ByteArray(1 + hashedPublicKey.size + 4)
        //拼接测试网络或正式网络前缀
        addressBytes[0] = if (isTestNet()) TEST_NET_ADDRESS_SUFFIX else MAIN_NET_ADDRESS_SUFFIX

        System.arraycopy(hashedPublicKey, 0, addressBytes, 1, hashedPublicKey.size)
        //进行双 Sha256 运算
        val check: ByteArray =
            Sha256.doubleSha256(addressBytes, addressBytes.size - 4) ?: return byteArrayOf()

        //将双 Sha256 运算的结果前 4位 拼接到尾部
        System.arraycopy(check, 0, addressBytes, hashedPublicKey.size + 1, 4)

        Arrays.fill(hashedPublicKey, 0.toByte())
        Arrays.fill(check, 0.toByte())
        return addressBytes
    }

    private fun isTestNet(): Boolean = testNet
}