package dev.queercoded

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity
import java.util.*


@Entity
open class Site() : PanacheEntity() {
    open lateinit var name: String
    open lateinit var domain: String
    open var path: String = "/"
    open var https: Boolean = true
    open lateinit var author: String
    open var date: Date = Date()
    open var enabled: Boolean = true
    open var dead_end: Boolean = false
}