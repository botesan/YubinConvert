# YubinConvert

KEN_ALL.CSVをSQLite3に変換

## 概要

JPZipAddressで使用している郵便番号検索の検索用データベースを作成するのに使用しているプログラムです。
https://play.google.com/store/apps/details?id=jp.mito.jpzipaddress

## ビルド

Kotlin/Nativeで記述しており、WindowsでWindowsバイナリをビルドできることを確認しています。
WindowsではLinuxバイナリのビルドに失敗しますが 、WSL1のUbuntuでWindowsとLinuxバイナリをビルドできることを確認しています。

Linuxではlibcurlのgnutlsバージョンが必要になるかもしれません。
https://ktor.io/docs/client-engines.html#curl

## 実行方法

郵便局のサイトからken_all.zipをダウンロードし、作業ディレクトリに変換ファイルを出力します。
https://www.post.japanpost.jp/zipcode/dl/kogaki-zip.html

```
YubinConvert.exe -d [作業ディレクトリ] all
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

- x_ken_all.csv
    - KEN_ALL.CSVの複数行の結合や平仮名への変換など、最低限の加工を行った結果を文字コードUTF-8で保存したCSVファイルです。
    - 公開されているutf_all.csvファイルと下記の点で異なります。（2023/7/31時点）
        - カタカナを平仮名に変換しています。
        - フォントの影響か文字が見づらいため、波ダッシュ「〜」の文字コードを全角チルダ「～」の文字コードに変換しています。
          （UTF-8としてはutf_all.csvが正しい？）
        - マイナス記号「−」の文字コードを全角ハイフン「－」の文字コードに変換しています。
        - もしかしたら他にもあるかもしれません。
- ken_all.sqlite
    - KEN_ALL.CSVの複数行の結合や平仮名への変換など、最低限の加工を行った結果を保存したデータベースです。
- x_ken_all.sqlite
    - JPZipAddressで使用している検索用データベースです。
    - そこそこ無理矢理な方法を使ってファイルサイズを小さくするようにしています。
- x_ken_all.sqlite.??.backup
    - 途中経過のバックアップファイルです。
