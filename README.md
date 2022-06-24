# YubinConvert
KEN_ALL.CSVをSQLite3に変換

## 概要
JPZipAddressで使用している郵便番号検索の検索用データベースを作成するのに使用しているプログラムです。
https://play.google.com/store/apps/details?id=jp.mito.jpzipaddress

## ビルド
Kotlin/Nativeで記述しており、Windowsでビルドできることを確認しています。

## 実行方法
下記からken_all.zipをダウンロードし、作業ディレクトリに変換ファイルを出力します。
https://www.post.japanpost.jp/zipcode/dl/kogaki-zip.html
```
YubinConvert -d _作業ディレクトリ_ all
```
詳しいコマンドライン引数は下記を参照
```
usage:
    YubinConvert.exe [options] {all|download|unzip|convert|compress|info}...

options:
    -d  <dir> : 作業ディレクトリを指定します
    -zi <num> : zopfliのiterator値指定
    -zb <num> : zopfliのblock splitting max値の指定

commands:
    all      : ダウンロードと展開、変換を行います（download,unzip,convertのみ）
    download : ken_all.zipをダウンロードします
    unzip    : ken_all.zipを展開します
    convert  : KEN_ALL.CSVファイルを変換します
    compress : 変換したx_ken_all.sqliteをzopfliで圧縮します
    info     : 各ファイルのMD5サム値などを表示します
```

## 出力ファイル
- ken_all.sqlite
  - KEN_ALL.CSVの複数行の結合や平仮名への変換など、最低限の加工を行った結果を保存したデータベースです。
- x_ken_all.sqlite
  - JPZipAddressで使用している検索用データベースです。
  - そこそこ無理矢理な方法を使ってファイルサイズを小さくするようにしています。
- x_ken_all.sqlite.??.backup
  - 途中経過のバックアップファイルです。
