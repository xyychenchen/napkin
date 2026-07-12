package com.imageflow.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移脚本集合。新增字段时务必加 Migration，不能开 destructive fallback。
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf(
        // 后续版本的 Migration 加在这里
    )
}
