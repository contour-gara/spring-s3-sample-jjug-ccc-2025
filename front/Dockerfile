# Node.js 18のAlpineイメージを使用（軽量）
FROM node:18-alpine as build

# 作業ディレクトリを設定
WORKDIR /app

# package.jsonをコピー（依存関係のキャッシュを活用）
COPY package*.json ./

# 依存関係をインストール（ビルドにはdev dependenciesも必要）
RUN npm install

# ソースコードをコピー
COPY . .

# React アプリケーションをビルド
RUN npm run build

# Nginx を使用してプロダクション用のイメージを作成
FROM nginx:alpine

# カスタムnginx設定をコピー
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# ポート80を公開
EXPOSE 80

# nginxを起動
CMD ["nginx", "-g", "daemon off;"]
