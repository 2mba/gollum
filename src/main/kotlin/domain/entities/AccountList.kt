package org.tumba.gollum.domain.entities

import com.dslplatform.json.CompiledJson

@CompiledJson
data class AccountList(
    val accounts: ArrayList<Account>
)