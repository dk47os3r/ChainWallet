package com.smallraw.chain.lib.extensions

import com.smallraw.chain.lib.jni.CryptoJNI

fun ByteArray.toHex() = CryptoJNI.hexToStr(this, this.size)