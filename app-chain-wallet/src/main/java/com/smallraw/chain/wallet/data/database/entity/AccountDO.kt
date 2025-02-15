/*
 * Copyright 2021 Smallraw Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smallraw.chain.wallet.data.database.entity

import androidx.annotation.IntDef
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smallraw.chain.wallet.data.database.entity.embed.BaseEntity

@Entity(
    tableName = AccountDO.TABLE_NAME,
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class AccountDO(
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = true)
    override val id: Long? = null,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "encrypted")
    var encrypted: String,

    @ColumnInfo(name = "account_type")
    @AccountType
    var accountType: Int = AccountType.MNEMONIC,

    @ColumnInfo(name = "source_type")
    @SourceType
    var sourceType: Int = SourceType.CREATE,
) : BaseEntity() {

    @IntDef(
        AccountType.MNEMONIC,
        AccountType.PRIVATE,
        AccountType.KEYSTORE,
        AccountType.WATCHING,
    )
    annotation class AccountType {
        companion object {
            const val MNEMONIC: Int = 0
            const val PRIVATE: Int = 1
            const val KEYSTORE: Int = 2
            const val WATCHING: Int = 3
        }
    }

    @IntDef(
        SourceType.CREATE,
        SourceType.IMPORT
    )
    annotation class SourceType {
        companion object {
            const val CREATE: Int = 0
            const val IMPORT: Int = 1
        }
    }

    companion object {
        const val TABLE_NAME = "account_table"
    }

    override fun toString(): String {
        return "AccountDO(id=$id, name=$name, encrypted=$encrypted, accountType=$accountType, sourceType=$sourceType)"
    }
}


