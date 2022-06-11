# YubinConvert
KEN_ALL.CSVをSQLite3に変換

## 概要
JPZipAddressで使用している郵便番号検索の検索用データベースを作成するのに使用しているプログラムです。
https://play.google.com/store/apps/details?id=jp.mito.jpzipaddress

## ビルド
Kotlin/Nativeで記述しており、Windowsでビルドできることを確認しています。

## 実行方法
下記ダウンロードしたken_all.zipと展開したKEN_ALL.CSVの**両方**を、作業ディレクトリに置いて実行します。
https://www.post.japanpost.jp/zipcode/dl/kogaki-zip.html
> YubinConvert.exe [作業ディレクトリ]

## 出力ファイル
- ken_all.sqlite
  - KEN_ALL.CSVの複数行の結合や平仮名への変換など、最低限の加工を行った結果を保存したデータベースです。
- x_ken_all.sqlite
  - JPZipAddressで使用している検索用データベースです。
  - そこそこ無理矢理な方法を使ってファイルサイズを小さくするようにしています。
- x_ken_all.sqlite.??.backup
  - 途中経過のバックアップファイルです。
