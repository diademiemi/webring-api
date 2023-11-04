package dev.queercoded.webring

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity
import java.util.*


@Entity
class Site : PanacheEntity() {
    lateinit var name: String
    lateinit var domain: String
    var path: String = "/"
    var https: Boolean = true
    lateinit var author: String
    var date: Date = Date()
    var enabled: Boolean = true
    var disable_checks: Boolean = false
    var dead_end: Boolean = false

}