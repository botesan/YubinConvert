# YubinConvert
KEN_ALL.CSVをSQLite3に変換

## 概要
JPZipAddressで使用している郵便番号検索の検索用データベースを作成するのに使用しているプログラムです。
https://play.google.com/store/apps/details?id=jp.mito.jpzipaddress

## ビルド
Kotlin/Nativeで記述しており、Windowsでビルドできることを確認しています。

## 実行方法
下記ダウンロードしたken_all.zipを、作業ディレクトリに置いて実行します。
https://www.post.japanpost.jp/zipcode/dl/kogaki-zip.html
```shell
YubinConvert -d _作業ディレクトリ_ all
```
詳しいコマンドライン引数は下記を参照
```shell
usage:
    YubinConvert.exe [options] {all|unzip|convert}...

options:
    -d &lt;dir&gt; : 作業ディレクトリを指定します

commands:
    all     : 展開と変換を行います
    unzip   : ZIPを展開します
    convert : CSVファイルを変換します
```

## 出力ファイル
- ken_all.sqlite
  - KEN_ALL.CSVの複数行の結合や平仮名への変換など、最低限の加工を行った結果を保存したデータベースです。
- x_ken_all.sqlite
  - JPZipAddressで使用している検索用データベースです。
  - そこそこ無理矢理な方法を使ってファイルサイズを小さくするようにしています。
- x_ken_all.sqlite.??.backup
  - 途中経過のバックアップファイルです。
