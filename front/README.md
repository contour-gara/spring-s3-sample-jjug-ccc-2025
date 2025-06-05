# Photo Note Frontend

Photo Note APIを使用するReactフロントエンドアプリケーションです。

## 機能

- 📝 写真メモの作成
- 📋 写真メモ一覧の表示
- 🖼️ S3からの画像表示
- 📱 レスポンシブデザイン
- 🐳 Docker対応

## ローカル開発

### 必要な環境

- Node.js 18以上
- npm または yarn

### セットアップ

```bash
# 依存関係をインストール
npm install

# 開発サーバーを起動
npm start
```

アプリケーションは http://localhost:3000 で起動します。

### API接続設定

デフォルトではlocalhost:8080のAPIに接続します。
環境変数 `REACT_APP_API_URL` で変更可能です。

```bash
# 例：異なるAPIサーバーに接続
REACT_APP_API_URL=http://localhost:9000 npm start
```

## Docker での実行

### 単体での実行

```bash
# イメージをビルド
docker build -t photo-note-frontend .

# コンテナを起動
docker run -p 3000:80 photo-note-frontend
```

### Docker Compose での実行

プロジェクトルートのDocker Composeを使用：

```bash
# プロジェクトルートディレクトリで
docker-compose up --build
```

## プロジェクト構成

```
front/
├── public/
│   └── index.html          # HTMLテンプレート
├── src/
│   ├── App.js             # メインReactコンポーネント
│   ├── index.js           # エントリーポイント
│   └── index.css          # スタイルシート
├── Dockerfile             # Docker設定
├── .dockerignore          # Docker除外ファイル
├── package.json           # npm設定
└── README.md              # このファイル
```

## 技術スタック

- **React 18** - UIライブラリ
- **Lucide React** - アイコンライブラリ
- **Tailwind CSS** - CSSフレームワーク（CDN経由）
- **Nginx** - プロダクション用Webサーバー
- **Docker** - コンテナ化

## API仕様

### エンドポイント

- `GET /photonote` - 写真メモ一覧取得
- `POST /photonote` - 写真メモ作成

### リクエスト例

```bash
# 一覧取得
curl http://localhost:8080/photonote

# 新規作成
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"note": "サンプルメモ"}' \
  http://localhost:8080/photonote
```

## 環境変数

| 変数名 | デフォルト値 | 説明 |
|--------|-------------|------|
| REACT_APP_API_URL | http://localhost:8080 | APIサーバーのURL |

## トラブルシューティング

### CORS エラーが発生する場合

1. API側でCORSが有効になっているか確認
2. Docker Compose使用時はサービス名で通信

### 画像が表示されない場合

1. S3のURLが正しいか確認
2. S3のCORS設定を確認
3. 画像URLがアクセス可能か確認

## ライセンス

このプロジェクトはサンプルアプリケーションです。
