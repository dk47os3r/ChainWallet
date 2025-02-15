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
package com.smallraw.chain.wallet.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.smallraw.chain.wallet.data.database.databaseView.WalletEmbedDO
import com.smallraw.chain.wallet.data.database.entity.WalletDO
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WalletDao : BaseDao<WalletDO> {
    @Query("SELECT COUNT(*) FROM wallet_table")
    abstract fun count(): Int

    @Query("SELECT * FROM wallet_table WHERE id = :id")
    abstract fun findById(id: Long): WalletDO

    @Query("SELECT * FROM wallet_table WHERE chain_table_id = :chainId")
    abstract fun findByTableChainId(chainId: Long): Flow<List<WalletEmbedDO>>
}
