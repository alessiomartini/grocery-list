package com.alessiomartini.dispensa.data

enum class ItemStatus {
    TO_BUY,
    IN_PANTRY,

    /** Hidden from the to-buy and pantry lists but kept (and synced); shown in the archive section of "To buy". */
    ARCHIVED
}
