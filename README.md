# photo-sns-crawler
某写真投稿SNSのクローラー
* プロフィールページ
* ハッシュタグページ
* 投稿ページ

から表示内容を取得します。<br>
必要に応じてログイン用クッキーも取得。

## 使い方
第一引数に取得したいページに該当するコマンド

### コマンド
* プロフィールページ
> prof
* ハッシュタグページ
> tags
* 投稿ページ
> posts

第二引数に取得先URL

### sbt環境で任意のユーザーのプロフィールページから取得する実行例
> sbt run prof https://www.instagram.com/{instagram_id}/

## 注意
* db環境
* seleniumdriverを右記に配置 /selenium

上記二点を対応しないと実行できません
