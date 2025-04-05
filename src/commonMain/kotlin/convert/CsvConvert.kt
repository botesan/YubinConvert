package convert

import codepoint.asSjisSequence
import codepoint.codePointToString
import codepoint.isHankaku
import codepoint.toCodePoints
import csv.asCsvSequence
import files.readFile
import files.writeFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.getBytes
import kotlinx.cinterop.utf8
import ksqlite3.SQLiteDB
import ksqlite3.SQLiteOpenType
import ksqlite3.runInTransaction
import tool.currentTimeText
import tool.dropTableIfExists
import tool.executeScript
import tool.setPageSizeAndVacuum
import util.use

interface CSVFilenames {
    val csvKenAll: String
    val csvXKenAll: String
    val dbKenAll: String
}

private fun stringPair2CharPairs(text1: String, text2: String): List<Pair<Char, Char>> {
    if (text1.length != text2.length) error("Not same length[text1=$text1, text2=$text2]")
    return text1.mapIndexed { index, c -> c to text2[index] }
}

private val han2ZenStringPairs = listOf(
    "ｶﾞ" to "が", "ｷﾞ" to "ぎ", "ｸﾞ" to "ぐ", "ｹﾞ" to "げ", "ｺﾞ" to "ご",
    "ｻﾞ" to "ざ", "ｼﾞ" to "じ", "ｽﾞ" to "ず", "ｾﾞ" to "ぜ", "ｿﾞ" to "ぞ",
    "ﾀﾞ" to "だ", "ﾁﾞ" to "ぢ", "ﾂﾞ" to "づ", "ﾃﾞ" to "で", "ﾄﾞ" to "ど",
    "ﾊﾞ" to "ば", "ﾋﾞ" to "び", "ﾌﾞ" to "ぶ", "ﾍﾞ" to "べ", "ﾎﾞ" to "ぼ",
    "ﾊﾟ" to "ぱ", "ﾋﾟ" to "ぴ", "ﾌﾟ" to "ぷ", "ﾍﾟ" to "ぺ", "ﾎﾟ" to "ぽ"
)

private val han2ZenCharMap = (
        stringPair2CharPairs(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ"
        ) + stringPair2CharPairs(
            "0123456789",
            "０１２３４５６７８９"
        ) + stringPair2CharPairs(
            "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ",
            "あいうえおかきくけこさしすせそ"
        ) + stringPair2CharPairs(
            "ﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎ",
            "たちつてとなにぬねのはひふへほ"
        ) + stringPair2CharPairs(
            "ﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜｦﾝ",
            "まみむめもやゆよらりるれろわをん"
        ) + stringPair2CharPairs(
            "ｧｨｩｪｫｯｬｭｮﾞ",
            "ぁぃぅぇぉっゃゅょ゛"
        ) + stringPair2CharPairs(
            "ｰ､･-()<>.",
            "ー、・－（）＜＞．"
        )).toMap()

private fun han2Zen(text: String): String {
    var work = text
    han2ZenStringPairs.forEach { (from, to) -> if (work.indexOf(from) >= 0) work = work.replace(from, to) }
    return work.map { han2ZenCharMap[it] ?: it }.toCharArray()
        .let { chars -> buildString(chars.size) { append(chars) } }
}

/*
 0: 全国地方公共団体コード（JIS X0401、X0402）…… 半角数字
 1: （旧）郵便番号（5桁）………………………………　半角数字
 2: 郵便番号（7桁）………………………………………　半角数字
 3: 都道府県名　…………　半角カタカナ（コード順に掲載）　（注1）
 4: 市区町村名　…………　半角カタカナ（コード順に掲載）　（注1）
 5: 町域名　………………　半角カタカナ（五十音順に掲載）　（注1）
 6: 都道府県名　…………　漢字（コード順に掲載）　（注1,2）
 7: 市区町村名　…………　漢字（コード順に掲載）　（注1,2）
 8: 町域名　………………　漢字（五十音順に掲載）　（注1,2）
 9: 一町域が二以上の郵便番号で表される場合の表示　（注3）　（「1」は該当、「0」は該当せず）
10: 小字毎に番地が起番されている町域の表示　（注4）　（「1」は該当、「0」は該当せず）
11: 丁目を有する町域の場合の表示　（「1」は該当、「0」は該当せず）
12: 一つの郵便番号で二以上の町域を表す場合の表示　（注5）　（「1」は該当、「0」は該当せず）
13: 更新の表示（注6）（「0」は変更なし、「1」は変更あり、「2」廃止（廃止データのみ使用））
14: 変更理由　（「0」は変更なし、「1」市政・区政・町政・分区・政令指定都市施行、
                「2」住居表示の実施、「3」区画整理、「4」郵便区調整等、「5」訂正、
                「6」廃止（廃止データのみ使用））
--------------------------------------
マージのチェックが必要かもしれない郵便番号
0285102
8260043
8710046
http://www.f3.dion.ne.jp/~element/msaccess/AcTipsKenAllCsv.html
http://d.hatena.ne.jp/H_Yamaguchi/20090130/p1
http://bleis-tift.hatenablog.com/entry/20080531/1212217681
http://www.my-hobby.jp/index.php/2010/03/zipsearch1/
select * from ken_all where zip_code in ('0285102','8260043','8710046','7711251','9218046','4091    321','5810027')
--------------------------------------
複数件存在するデータ（読み仮名が違う）
5810027 大阪府八尾市八尾木
6730012 兵庫県明石市和坂
--------------------------------------
複数行に分かれているもの＆町域名の仮名が同じ場合に合成が重複する
6028064 2018/11/30データ修正済み
6028134 2018/11/30データ未修正
select * from ken_all where zip_code in (
'6008016','6008023','6008025','6008028','6008032','6008043','6008044','6008052','6008057','6008066',
'6008067','6008069','6008070','6008093','6008098','6008099','6008120','6008121','6008126','6008128',
'6008134','6008143','6008144','6008146','6008157','6008161','6008162','6008164','6008168','6008173',
'6008178','6008182','6008183','6008185','6008201','6008223','6008255','6008262','6008266','6008268',
'6008301','6008310','6008314','6008317','6008321','6008329','6008347','6008349','6008351','6008359',
'6008390','6008399','6008425','6008440','6008449','6008464','6008468','6008483','6008487','6008498',
'6020033','6020046','6020093','6020816','6020842','6020846','6020847','6020848','6020849','6020856',
'6020956','6020958','6028019','6028034','6028045','6028046','6028048','6028056','6028058','6028062',
'6028064','6028065','6028072','6028073','6028076','6028114','6028118','6028119','6028125','6028134',
'6028154','6028202','6028227','6028244','6028256','6028315','6028322','6028325','6028346','6028362',
'6028363','6028368','6028374','6028391','6028392','6028405','6028414','6028444','6028454','6028482',
'6040014','6040043','6040071','6040082','6040091','6040092','6040094','6040801','6040802','6040803',
'6040805','6040811','6040814','6040835','6040836','6040865','6040874','6040905','6040911','6040941',
'6040944','6040964','6040971','6040972','6040981','6040982','6040983','6048006','6048012','6048031',
'6048036','6048042','6048051','6048053','6048063','6048072','6048073','6048083','6048086','6048095',
'6048103','6048111','6048112','6048127','6048135','6048187','6048202','6048214','6048236','6048247',
'6048272','6048315','6048345','6050034','6050832','6050874'
)
*/

private fun checkDividedLine(row: List<String>, hold: List<String>): Boolean {
    val isAllSame = intArrayOf(1, 2, 3, 4, 6, 7).all { row[it] == hold[it] }
    if (!isAllSame) return false
    if (row[12] == "0") {
        println("\tuse flag4 concat : ${row[2]} : ${hold[5] == row[5] || hold[8] == row[8]}")
        return true
    }
    if (hold[5].lastIndexOf('(') > hold[5].lastIndexOf(')')) {
        println("\tuse ()    concat : ${row[2]} : ${hold[5] == row[5] || hold[8] == row[8]}")
        return true
    }
    if (hold[8].lastIndexOf('（') > hold[8].lastIndexOf('）')) {
        println("\tuse （）  concat : ${row[2]} : ${hold[5] == row[5] || hold[8] == row[8]}")
        return true
    }
    return false
}

private fun MutableList<MutableList<String>>.mergeDividedLine(
    row: MutableList<String>?,
    hold: MutableList<String>?
): MutableList<String>? {
    // 最終呼び出しなら追加のみ
    if (row == null) {
        if (hold != null) add(hold)
        return null
    }
    // 保持しているデータが無ければ行を次の処理に渡す
    if (hold == null) return row
    // 分割行なら該当項目を繋げる
    if (checkDividedLine(row, hold)) {
        intArrayOf(5, 8)
            .filter { hold[it] != row[it] }
            .forEach { hold[it] = "${hold[it]}${row[it]}" }
        return hold
    }
    // 単独行or統合済みなら追加
    add(hold)
    // 行を次の処理に渡す
    return row
}

private fun List<String>.asMutable() = if (this is MutableList<String>) this else ArrayList(this)

private fun Sequence<List<String>>.mergeDividedLine(): List<MutableList<String>> {
    val merged = ArrayList<MutableList<String>>()
    var hold: MutableList<String>? = null
    forEach { row -> hold = merged.mergeDividedLine(row.asMutable(), hold) }
    merged.mergeDividedLine(null, hold)
    merged.trimToSize()
    return merged
}

private fun List<List<String>>.checkDuplicateWithoutKana() {
    println("\tduplicate without kana check start.")
    var lastRow: List<String>? = null
    asSequence()
        .map { row -> intArrayOf(0, 1, 2, 6, 7, 8).map { row[it] } }
        .sortedWith { a, b ->
            a.indices.asSequence().map { a[it].compareTo(b[it]) }.find { it != 0 } ?: 0
        }
        .filter { row ->
            if (lastRow == row) {
                true
            } else {
                lastRow = row; false
            }
        }
        .toSet()
        .forEach { row -> println("\t\t$row") }
    println("\tduplicate without kana check finish.")
}

private fun List<MutableList<String>>.han2ZenEtc() {
    forEach { row ->
        // 半角→全角変換
        row[3] = han2Zen(row[3])
        row[4] = han2Zen(row[4])
        row[5] = han2Zen(row[5])
        // 波ダッシュを全角チルダへ
        row[8] = row[8].replace('\u301c', '\uff5e')
        // MINUS SIGNを全角ハイフンへ
        row[8] = row[8].replace('\u2212', '－')
    }
}

private fun List<List<String>>.checkHankaku() {
    println("\thankaku check start.")
    asSequence()
        .flatMap { row -> intArrayOf(3, 4, 5, 6, 7, 8).map { row[it] }.asSequence() }
        .map { it.toCodePoints() }
        .fold(mutableSetOf<Int>()) { acc, value -> acc.apply { addAll(value) } }
        .filter { isHankaku(it) }
        .forEach { println("\thankaku=${it.codePointToString()}") }
    println("\thankaku check finish.")
}

@OptIn(ExperimentalForeignApi::class)
private fun List<List<String>>.writeToCsv(csvFilePath: String) {
    val data = joinToString(separator = "\r\n", postfix = "\r\n") { line ->
        line.withIndex().joinToString(separator = ",") { col ->
            when (col.index) {
                0, in 9..14 -> col.value
                else -> "\"${col.value}\""
            }
        }
    }.utf8.getBytes()
    writeFile(filePath = csvFilePath, data = data.sliceArray(indices = 0..<data.lastIndex))
}

private fun SQLiteDB.writeToDb(csv: List<List<String>>) = runInTransaction {
    dropTableIfExists("ken_all")
    executeScript(
        """
        create table ken_all (
            id              integer primary key,
            x0401_02_code   integer not null,
            old_zip_code    text    not null,
            zip_code        text    not null,
            kana_prefecture text    not null,
            kana_city       text    not null,
            kana_town_area  text    not null,
            prefecture      text    not null,
            city            text    not null,
            town_area       text    not null,
            flag1           integer not null,
            flag2           integer not null,
            flag3           integer not null,
            flag4           integer not null,
            flag5           integer not null,
            flag6           integer not null
        )
    """, isTrim = true
    )
    prepare(
        """
        insert into ken_all(
            x0401_02_code,
            old_zip_code,
            zip_code,
            kana_prefecture,
            kana_city,
            kana_town_area,
            prefecture,
            city,
            town_area,
            flag1,
            flag2,
            flag3,
            flag4,
            flag5,
            flag6
        ) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    """
    ).use {
        csv.forEach { row ->
            it.reset()
            row.forEachIndexed { index, col -> it.bind(index + 1, col) }
            it.execute()
        }
    }
}

private fun printProgress(no: Int, max: Int, message: String) {
    println("[${currentTimeText()}] CSV Convert $no/$max : $message")
}

fun csvConvert(f: CSVFilenames) {
    val merged = run {
        printProgress(1, 8, "create csv sequence")
        val csv = readFile(f.csvKenAll).asSjisSequence().asCsvSequence()
        printProgress(2, 8, "merge divided line")
        csv.mergeDividedLine()
    }
    printProgress(3, 8, "check duplicate without kana")
    merged.checkDuplicateWithoutKana()
    printProgress(4, 8, "han to zen etc")
    merged.han2ZenEtc()
    printProgress(5, 8, "check hankaku")
    merged.checkHankaku()
    printProgress(6, 8, "write to CSV")
    merged.writeToCsv(csvFilePath = f.csvXKenAll)
    SQLiteDB(f.dbKenAll, SQLiteOpenType.ReadWriteCreate).use { db ->
        printProgress(7, 8, "write to db")
        db.writeToDb(merged)
        printProgress(8, 8, "set page size and vacuum")
        db.setPageSizeAndVacuum()
    }
}
