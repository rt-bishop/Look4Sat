package com.rtbishop.look4sat.core.domain.repository

import com.rtbishop.look4sat.core.domain.model.SatStatusPage

/** AMSAT 卫星状态数据源 */
interface IAmSatRepository {
    /** 抓取并解析 AMSAT 状态页; 失败返回 null */
    suspend fun fetchStatus(): SatStatusPage?
}
