package convert

import codepoint.codePointToString
import codepoint.isHankaku
import codepoint.toCodePoints
import files.fileModifiedTimeSec
import ksqlite3.*
import tool.*

interface DBFilenames {
    val zipKenAll: String
    val dbKenAll: String
    val dbXKenAll: String
}

private inline fun SQLiteDB.run(
    f: DBFilenames,
    no: Int,
    max: Int,
    message: String,
    isBackup: Boolean,
    block: SQLiteDB.() -> Unit
) {
    val noText = no.toString().let { if (it.length == 1) "0$it" else it }
    val maxText = max.toString().let { if (it.length == 1) "0$it" else it }
    println("[${currentTimeText()}] DB Convert $noText/$maxText : $message")
    block()
    if (isBackup) {
        print("\tbackup db...")
        execute("vacuum")
        backupTo("${f.dbXKenAll}.$noText.backup")
        println("done.")
    }
}

private fun SQLiteDB.createTableXKenAll() {
    runInTransaction {
        // 使用テーブル
        dropTableIfExists("date_time")
        dropTableIfExists("prefecture")
        dropTableIfExists("city")
        dropTableIfExists("zip_code_shrink")
        dropTableIfExists("old_zip_code_shrink")
        dropTableIfExists("town_area_shrink")
        dropViewIfExists("zip_code")
        // 作業用
        dropTableIfExists("zip_code_org")
        dropTableIfExists("zip_code_shrink_temp")
        dropTableIfExists("old_zip_code_shrink_temp")
        dropTableIfExists("town_area_shrink_temp")
        dropIndexIfExists("zip_code_town_area")
        dropIndexIfExists("zip_code_kana_area")
        dropIndexIfExists("town_area_shrink_temp_name")
        dropIndexIfExists("town_area_shrink_temp_kana")
        dropIndexIfExists("zip_code_shrink_temp_id_len_all")
        dropIndexIfExists("zip_code_shrink_temp_id_len_kana")
        dropIndexIfExists("zip_code_shrink_temp_town_area_id")
        // 作成
        // language=sql
        executeScript(
            """
            create table date_time (
                id        integer primary key,
                created   integer not null,
                processed integer not null
            )
            ;
            create table prefecture (
                x0401_code integer primary key,
                kana       text    not null,
                name       text    not null
            )
            ;
            create table city (
                x0402_code integer primary key,
                kana       text    not null,
                name       text    not null
            )
            ;
            create table zip_code_shrink (
                id              integer primary key,
                x0401_02_code   integer not null,
                old_zip_code_id integer not null,
                zip_code        integer not null,
                kana_town_area  text    not null,
                town_area       text    not null,
                town_area_id    integer
            )
            ;
            create table old_zip_code_shrink (
                id           integer primary key,
                old_zip_code text    not null
            )
            ;
            create table town_area_shrink (
                id   integer primary key,
                kana text not null,
                name text not null
            )
            ;
            create view zip_code as
                select
                    zip_code_shrink.id as id,
                    x0401_02_code,
                    old_zip_code_shrink.old_zip_code as old_zip_code,
                    substr('00'||zip_code,-7) as zip_code,
                    case
                        when town_area_id is null then kana_town_area
                        else town_area_shrink.kana || kana_town_area
                    end as kana_town_area,
                    case
                        when town_area_id is null then town_area
                        else town_area_shrink.name || town_area
                    end as town_area
                from
                    zip_code_shrink left outer join town_area_shrink
                        on town_area_shrink.id = town_area_id
                        inner join old_zip_code_shrink
                        on zip_code_shrink.old_zip_code_id = old_zip_code_shrink.id
            ;
            create table zip_code_org (
                id             integer primary key,
                x0401_02_code  integer not null,
                old_zip_code   text    not null,
                zip_code       text    not null,
                kana_town_area text    not null,
                town_area      text    not null
            )
            ;
            create table zip_code_shrink_temp (
                id             integer not null,
                x0401_02_code  integer not null,
                old_zip_code   text    not null,
                zip_code       integer not null,
                kana_town_area text    not null,
                town_area      text    not null,
                town_area_id   integer,
                len_all        integer,
                len_kana       integer,
                len_name       integer
            )
            ;
            create table old_zip_code_shrink_temp (
                id           integer primary key,
                old_zip_code text    not null
            )
            ;
            create table town_area_shrink_temp (
                id   integer primary key,
                kana text not null,
                name text not null,
                unique(kana,name) on conflict ignore
            )
        """, isTrim = true
        )
        // language=sql
        execute("create index zip_code_town_area      on zip_code_org(town_area)")
        // language=sql
        execute("create index zip_code_kana_town_area on zip_code_org(kana_town_area)")
        // language=sql
        execute("create index town_area_shrink_temp_kana on town_area_shrink_temp(kana)")
        // language=sql
        execute("create index town_area_shrink_temp_name on town_area_shrink_temp(name)")
        // language=sql
        execute("create index zip_code_shrink_temp_id_len_all   on zip_code_shrink_temp(id,len_all)")
        // language=sql
        execute("create index zip_code_shrink_temp_id_len_kana  on zip_code_shrink_temp(id,len_kana)")
        // language=sql
        execute("create index zip_code_shrink_temp_town_area_id on zip_code_shrink_temp(town_area_id)")
    }
    // 作業用設定
    val beforeCacheSize = execute("pragma cache_size").second.firstOrNull()?.firstOrNull()
    executeScript("pragma cache_size=-65536; pragma page_size=65536; vacuum")
    val afterCacheSize = execute("pragma cache_size").second.firstOrNull()?.firstOrNull()
    println("\tcache size: before=$beforeCacheSize after=$afterCacheSize")
}

private fun SQLiteDB.copyFromKenAll(f: DBFilenames) {
    val inSchema = "inSchema"
    // language=sql
    execute("attach database '${f.dbKenAll}' as $inSchema")
    runInTransaction {
        // language=sql
        executeScript(
            """
            insert into prefecture
                select   x0401_02_code/1000 as x0401_code,
                         kana_prefecture,
                         prefecture
                from     $inSchema.ken_all
                group by x0401_code
            ;
            insert into city
                select   x0401_02_code,
                         kana_city,
                         city
                from     $inSchema.ken_all
                group by x0401_02_code
            ;
            insert into zip_code_org(x0401_02_code,
                                     old_zip_code,
                                     zip_code,
                                     kana_town_area,
                                     town_area)
                select  x0401_02_code,
                        trim(old_zip_code),
                        zip_code,
                        kana_town_area,
                        town_area
                from    $inSchema.ken_all
        """
        )
    }
    execute("detach database $inSchema")
}

private fun SQLiteDB.replaceWords() = runInTransaction {
    // language=sql
    executeScript(
        """
        update  zip_code_org
        set     town_area      = '',
                kana_town_area = ''
        where   town_area like '%_市一円'
            and kana_town_area like '%_しいちえん'
        or      town_area like '%_町一円'
            and (kana_town_area like '%_ちょういちえん' or
                    kana_town_area like '%_まちいちえん')
        or      town_area like '%_村一円'
            and (kana_town_area like '%_そんいちえん' or
                    kana_town_area like '%_むらいちえん')
        ;
        update  zip_code_org
        set     town_area      = '（他に掲載がない場合）',
                kana_town_area = '（ほかにけいさいがないばあい）'
        where   town_area = '以下に掲載がない場合'
            and kana_town_area = 'いかにけいさいがないばあい'
        ;
        update  zip_code_org
        set     town_area      = '（' || town_area      || '）',
                kana_town_area = '（' || kana_town_area || '）'
        where   town_area like '%_の次に%番地%がくる場合%'
            and kana_town_area like '%_のつぎに%ばんち%がくるばあい%'
        ;
        update  zip_code_org
        set     town_area      = replace(town_area,      '次の',   '他に掲載がある'),
                kana_town_area = replace(kana_town_area, 'つぎの', 'ほかにけいさいがある')
        where   town_area like '%（次のビルを除く）'
            and kana_town_area like '%（つぎのびるをのぞく）'
    """
    )
}

private fun SQLiteDB.makeTownAreaShrinkBase() = runInTransaction {
    // language=sql
    execute(
        """
        insert into town_area_shrink_temp(id,kana,name)
            values(200000, '（ほかにけいさいがないばあい）', '（他に掲載がない場合）')
    """
    )
    // language=sql
    execute(
        """
        insert into town_area_shrink_temp(kana,name)
            select * from
            (
                select kana_town_area, town_area
                from zip_code_org
                where length(kana_town_area) > 0 and length(town_area) > 0
                group by kana_town_area, town_area
                having count(kana_town_area) >= 3

                union select kana_town_area_sub, town_area_sub
                from (  select  substr(kana_town_area,1,instr(kana_town_area,'（')) as kana_town_area_sub,
                                substr(town_area,     1,instr(town_area,     '（')) as town_area_sub
                        from zip_code_org
                        where length(kana_town_area_sub) > 1 and length(town_area_sub) > 1)
                group by kana_town_area_sub, town_area_sub
                having count(kana_town_area_sub) >= 3

                union select kana_town_area_sub, town_area_sub
                from (  select  substr(kana_town_area,1,9) as kana_town_area_sub,
                                substr(town_area,     1,5) as town_area_sub
                        from zip_code_org
                        where length(kana_town_area) > 0 and length(town_area) > 0)
                group by kana_town_area_sub, town_area_sub
                having count(kana_town_area_sub) >= 3

                union select kana_town_area_sub, town_area_sub
                from (  select  substr(kana_town_area,1,7) as kana_town_area_sub,
                                substr(town_area,     1,4) as town_area_sub
                        from zip_code_org
                        where length(kana_town_area) > 0 and length(town_area) > 0)
                group by kana_town_area_sub, town_area_sub
                having count(kana_town_area_sub) >= 3

                union select kana_town_area_sub, town_area_sub
                from (  select  substr(kana_town_area,1,5) as kana_town_area_sub,
                                substr(town_area,     1,3) as town_area_sub
                        from zip_code_org
                        where length(kana_town_area) > 0 and length(town_area) > 0)
                group by kana_town_area_sub, town_area_sub
                having count(kana_town_area_sub) >= 3

                union select kana_town_area_sub, town_area_sub
                from (  select  substr(kana_town_area,1,4) as kana_town_area_sub,
                                substr(town_area,     1,2) as town_area_sub
                        from zip_code_org
                        where length(kana_town_area) > 0 and length(town_area) > 0)
                group by kana_town_area_sub, town_area_sub
                having count(kana_town_area_sub) >= 3

                union select kana_town_area_sub, town_area_sub
                from (  select  substr(kana_town_area,1,3) as kana_town_area_sub,
                                substr(town_area,     1,2) as town_area_sub
                        from zip_code_org
                        where length(kana_town_area) > 0 and length(town_area) > 0)
                group by kana_town_area_sub, town_area_sub
                having count(kana_town_area_sub) >= 3
            )
    """
    )
    // language=sql
    execute("insert into town_area_shrink select id,kana,name from town_area_shrink_temp")
    println("\tinserted count=${lastChangesCount()}")
}

private fun SQLiteDB.makeZipCodeShrinkBase() = runInTransaction {
    // language=sql
    execute(
        """
        insert into zip_code_shrink_temp
            select
                zip_code_org.id as id,
                x0401_02_code,
                old_zip_code,
                zip_code,
                case
                    when town_area_shrink_temp.id is not null
                        then substr(kana_town_area,length(town_area_shrink_temp.kana)+1)
                        else kana_town_area
                end as kana_town_area,
                case
                    when town_area_shrink_temp.id is not null
                        then substr(town_area,length(town_area_shrink_temp.name)+1)
                        else town_area
                end as town_area,
                town_area_shrink_temp.id as town_area_id,
                length(town_area_shrink_temp.kana)+length(town_area_shrink_temp.name) as len_all,
                length(town_area_shrink_temp.kana) as len_kana,
                length(town_area_shrink_temp.name) as len_name
            from
                zip_code_org left outer join town_area_shrink_temp
                    on  kana_town_area glob town_area_shrink_temp.kana || '*'
                    and town_area      glob town_area_shrink_temp.name || '*'
    """
    )
    println("\tinserted count=${lastChangesCount()}")
}

private fun SQLiteDB.deleteZipCodeShrinkExtra1() = runInTransaction {
    // language=sql
    execute(
        """
        delete from zip_code_shrink_temp
        where rowid in (
            select
                zip_code_shrink_temp.rowid
            from
                zip_code_shrink_temp inner join (
                    select
                        id,
                        max(len_all) as max_all
                    from
                        zip_code_shrink_temp
                    group by
                        id
                    having
                        count(id) > 1
                ) as temp_c
                    on  zip_code_shrink_temp.id      = temp_c.id
                    and zip_code_shrink_temp.len_all < temp_c.max_all
            order by
                zip_code_shrink_temp.rowid
        )
    """
    )
    println("\tdeleted count=${lastChangesCount()}")
}

private fun SQLiteDB.deleteZipCodeShrinkExtra2() = runInTransaction {
    // language=sql
    execute(
        """
        delete from zip_code_shrink_temp
        where rowid in (
            select
                zip_code_shrink_temp.rowid
            from
                zip_code_shrink_temp inner join (
                    select
                        id,
                        max(len_kana) as max_kana
                    from
                        zip_code_shrink_temp
                    group by
                        id
                    having
                        count(id) > 1
                ) as temp_c
                    on  zip_code_shrink_temp.id       = temp_c.id
                    and zip_code_shrink_temp.len_kana < temp_c.max_kana
            order by
                zip_code_shrink_temp.rowid
        )
    """
    )
    println("\tdeleted count=${lastChangesCount()}")
}

private fun SQLiteDB.renumberTownAreaShrink() {
    val oldIds = mutableListOf<Long>()
    // language=sql
    prepare(
        """
        select
            town_area_shrink.id
        from
            zip_code_shrink_temp inner join town_area_shrink
                on zip_code_shrink_temp.town_area_id = town_area_shrink.id
        group by
            town_area_shrink.id
        order by
            count(town_area_shrink.id) desc, length(kana), length(name), kana, name
    """
    ).use {
        it.rowEach { _, row ->
            (row.firstOrNull() as? Long)?.let { oldId -> oldIds.add(oldId) }
            true
        }
    }
    // language=sql
    prepare("update town_area_shrink set id=? where id=?").use { update1 ->
        prepare("update zip_code_shrink_temp set town_area_id=? where town_area_id=?").use { update2 ->
            runInTransaction {
                var newId = -128L
                oldIds.forEach { oldId ->
                    update1.apply {
                        reset()
                        bind(1, newId)
                        bind(2, oldId)
                    }.execute()
                    update2.apply {
                        reset()
                        bind(1, newId)
                        bind(2, oldId)
                    }.execute()
                    newId++
                }
                println("\tlast newId=$newId")
            }
        }
    }
}

private fun SQLiteDB.zipCodeShrinkJoinTownArea() = runInTransaction {
    // まとめる個数が一つのものはまとめない
    // language=sql
    execute(
        """
        insert into zip_code_shrink_temp
            select
                zip_code_shrink_temp.id as id,
                x0401_02_code,
                old_zip_code,
                zip_code,
                town_area_shrink.kana || kana_town_area,
                town_area_shrink.name || town_area,
                null as town_area_id,
                null as len_all,
                null as len_kana,
                null as len_name
            from
                zip_code_shrink_temp inner join town_area_shrink
                    on zip_code_shrink_temp.town_area_id = town_area_shrink.id
            group by
                town_area_id
            having
                count(town_area_id) = 1
    """
    )
    println("\tinserted count=${lastChangesCount()}")
}

private fun SQLiteDB.deleteZipCodeShrinkExtra3() = runInTransaction {
    // まとめる個数が一つのものはまとめない
    // language=sql
    execute(
        """
        delete from zip_code_shrink_temp
        where rowid in (
            select
                zip_code_shrink_temp.rowid
            from
                zip_code_shrink_temp inner join town_area_shrink
                    on zip_code_shrink_temp.town_area_id = town_area_shrink.id
            group by
                town_area_id
            having
                count(town_area_id) = 1
            order by
                zip_code_shrink_temp.rowid
        )
    """
    )
    println("\tdeleted count=${lastChangesCount()}")
}

private fun SQLiteDB.makeOldZipCodeShrink() = runInTransaction {
    // language=sql
    executeScript(
        """
        insert into old_zip_code_shrink_temp(old_zip_code)
            select   old_zip_code
            from     zip_code_shrink_temp
            group by old_zip_code
            order by count(old_zip_code)*length(old_zip_code) desc
        ;
        insert into old_zip_code_shrink
            select  id-1-128, old_zip_code
            from    old_zip_code_shrink_temp
    """
    )
}

private fun SQLiteDB.makeZipCodeShrink() = runInTransaction {
    // language=sql
    execute(
        """
        insert into zip_code_shrink
        select
            zip_code_shrink_temp.id,
            zip_code_shrink_temp.x0401_02_code,
            old_zip_code_shrink.id,
            zip_code_shrink_temp.zip_code,
            zip_code_shrink_temp.kana_town_area,
            zip_code_shrink_temp.town_area,
            zip_code_shrink_temp.town_area_id
        from
            zip_code_shrink_temp inner join old_zip_code_shrink
                on zip_code_shrink_temp.old_zip_code = old_zip_code_shrink.old_zip_code
        order by
            zip_code_shrink_temp.id
    """
    )
    println("\tinserted count=${lastChangesCount()}")
}

private fun SQLiteDB.deleteTownAreaShrinkExtra() = runInTransaction {
    // language=sql
    execute(
        """
        delete from town_area_shrink
        where rowid in (
            select  rowid
            from    town_area_shrink
            where   id in (
                    select  town_area_shrink.id
                    from    town_area_shrink left outer join zip_code_shrink
                                on town_area_shrink.id = zip_code_shrink.town_area_id
                    where   town_area_id is null
            )
        )
    """
    )
    println("\tdeleted count=${lastChangesCount()}")
}

private fun SQLiteDB.checkHankaku() {
    println("\thankaku check start.")
    execute(
        // language=sql
        "select kana_town_area||town_area from zip_code_shrink"
    ).second
        .asSequence()
        .flatMap { it.asSequence() }
        .filterIsInstance<String>()
        .map { it.toCodePoints() }
        .fold(mutableSetOf<Int>()) { acc, value -> acc.apply { addAll(value) } }
        .filter { isHankaku(it) }
        .forEach { println("\tzip code shrink hankaku=${it.codePointToString()}") }
    println("\tzip code shrink hankaku check finish.")
    execute(
        // language=sql
        "select kana||name from town_area_shrink"
    ).second
        .asSequence()
        .flatMap { it.asSequence() }
        .filterIsInstance<String>()
        .map { it.toCodePoints() }
        .fold(mutableSetOf<Int>()) { acc, value -> acc.apply { addAll(value) } }
        .filter { isHankaku(it) }
        .forEach { println("\ttown area shrink hankaku=${it.codePointToString()}") }
    println("\ttown area shrink hankaku check finish.")
}

private fun SQLiteDB.checkZipCode() {
    // language=sql
    val rows = execute(
        """
        select count(id),id,x0401_02_code,old_zip_code,zip_code,kana_town_area,town_area
        from (  select   id,x0401_02_code,old_zip_code,zip_code,kana_town_area,town_area
                from zip_code
            union all
                select   id,x0401_02_code,old_zip_code,zip_code,kana_town_area,town_area
                from zip_code_org
        )
        group by         id,x0401_02_code,old_zip_code,zip_code,kana_town_area,town_area
        having count(id) <> 2
    """
    ).second
    if (rows.isNotEmpty()) {
        rows.take(2).forEach { println("\t$it") }
        error("Fail check.")
    }
    println("\tzip code check ok.")
}

private fun SQLiteDB.dropTempTable() = runInTransaction {
    dropTableIfExists("zip_code_org")
    dropTableIfExists("zip_code_shrink_temp")
    dropTableIfExists("old_zip_code_shrink_temp")
    dropTableIfExists("town_area_shrink_temp")
    dropIndexIfExists("zip_code_town_area")
    dropIndexIfExists("zip_code_kana_area")
    dropIndexIfExists("town_area_shrink_temp_name")
    dropIndexIfExists("town_area_shrink_temp_kana")
    dropIndexIfExists("zip_code_shrink_temp_id_len_all")
    dropIndexIfExists("zip_code_shrink_temp_id_len_kana")
    dropIndexIfExists("zip_code_shrink_temp_town_area_id")
}

private fun SQLiteDB.setDateTime(files: DBFilenames) {
    runInTransaction {
        val createdTimeMillis = 1000 * fileModifiedTimeSec(files.zipKenAll)
        val processedTimeMillis = 1000 * currentTimeSec()
        prepare("insert or replace into date_time values(?,?,?)").use {
            it.bind(1, 1)
            it.bind(2, createdTimeMillis)
            it.bind(3, processedTimeMillis)
            it.execute()
        }
    }
}

private fun SQLiteDB.printLastInformation() {
    // language=sql
    val shrinkCount = execute(
        """
        select sum(sum_len)
        from (        select  sum(length(kana)+length(name)) as sum_len
                        from    zip_code_shrink inner join town_area_shrink
                        on    zip_code_shrink.town_area_id = town_area_shrink.id
                union select -sum(length(town_area_shrink.kana)+length(town_area_shrink.name)+1)
                        from    town_area_shrink)
    """
    ).second.firstOrNull()?.firstOrNull()
    val pageSize = execute("pragma page_size").second.firstOrNull()?.firstOrNull()
    val pageCount = execute("pragma page_count").second.firstOrNull()?.firstOrNull()
    val schemaVersion = execute("pragma schema_version").second.firstOrNull()?.firstOrNull()
    val integrityCheck = execute("pragma integrity_check").second.firstOrNull()?.firstOrNull()
    println(
        arrayOf(
            "shrinkCount=$shrinkCount",
            "pageSize=$pageSize",
            "pageCount=$pageCount",
            "schemaVersion=$schemaVersion",
            "integrityCheck=$integrityCheck"
        ).joinToString(prefix = "\t")
    )
}

fun dbConvert(f: DBFilenames) {
    SQLiteDB(f.dbXKenAll, SQLiteOpenType.ReadWriteCreate).use { db ->
        db.run(f, 1, 19, "create tables and etc.", true) { createTableXKenAll() }
        db.run(f, 2, 19, "copy from kan_all", true) { copyFromKenAll(f) }
        db.run(f, 3, 19, "replace words", true) { replaceWords() }
        db.run(f, 4, 19, "make town area shrink base", true) { makeTownAreaShrinkBase() }
        db.run(f, 5, 19, "make zip code shrink base", true) { makeZipCodeShrinkBase() }
        db.run(f, 6, 19, "delete zip code shrink extra 1", true) { deleteZipCodeShrinkExtra1() }
        db.run(f, 7, 19, "delete zip code shrink extra 2", true) { deleteZipCodeShrinkExtra2() }
        db.run(f, 8, 19, "renumber town area shrink", true) { renumberTownAreaShrink() }
        db.run(f, 9, 19, "zip code shrink join town area", true) { zipCodeShrinkJoinTownArea() }
        db.run(f, 10, 19, "delete zip code shrink extra 3", true) { deleteZipCodeShrinkExtra3() }
        db.run(f, 11, 19, "make old zip code shrink", true) { makeOldZipCodeShrink() }
        db.run(f, 12, 19, "make zip code shrink", true) { makeZipCodeShrink() }
        db.run(f, 13, 19, "delete town area shrink extra", true) { deleteTownAreaShrinkExtra() }
        //
        db.run(f, 14, 19, "check hankaku", false) { checkHankaku() }
        db.run(f, 15, 19, "check zip code", false) { checkZipCode() }
        //
        db.run(f, 16, 19, "drop temp tables and etc.", false) { dropTempTable() }
        db.run(f, 17, 19, "set date time", false) { setDateTime(f) }
        db.run(f, 18, 19, "set page size and vacuum", false) { setPageSizeAndVacuum() }
        //
        db.run(f, 19, 19, "print last information", false) { printLastInformation() }
    }
}
