package com.rtbishop.look4sat.domain.usecase

interface IAddToCalendar {
    operator fun invoke(name: String, aosTime: Long, losTime: Long)
}
