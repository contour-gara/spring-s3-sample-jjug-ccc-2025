import React, { useState, useEffect } from 'react';
import { Camera, FileText, Plus, RefreshCw, AlertCircle, Upload, Image } from 'lucide-react';

const PhotoNoteApp = () => {
  const [photoNotes, setPhotoNotes] = useState([]);
  const [newNote, setNewNote] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState('');
  // Docker環境用のAPI URL設定
  const [apiUrl] = useState(process.env.REACT_APP_API_URL || 'http://localhost:8080');

  // 写真ノート一覧を取得
  const fetchPhotoNotes = async () => {
    setLoading(true);
    setError('');
    
    try {
      const response = await fetch(`${apiUrl}/photonote`);
      if (!response.ok) {
        throw new Error(`HTTPエラー: ${response.status}`);
      }
      const data = await response.json();
      setPhotoNotes(data.photoNotes || []);
    } catch (err) {
      setError(`データの取得に失敗しました: ${err.message}`);
      console.error('取得エラー:', err);
    } finally {
      setLoading(false);
    }
  };

  // ファイル選択処理
  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (file) {
      // 画像ファイルのみ許可
      if (!file.type.startsWith('image/')) {
        setError('画像ファイルを選択してください');
        return;
      }
      // ファイルサイズチェック (10MB以下)
      if (file.size > 10 * 1024 * 1024) {
        setError('ファイルサイズは10MB以下にしてください');
        return;
      }
      setSelectedFile(file);
      setError('');
    }
  };

  // S3への写真アップロード
  const uploadPhotoToS3 = async (presignedUrl, file) => {
    setUploadProgress(0);
    
    try {
      const response = await fetch(presignedUrl, {
        method: 'PUT',
        body: file,
        headers: {
          'Content-Type': file.type,
        },
        // アップロード進捗の監視（XMLHttpRequestを使用）
      });

      if (!response.ok) {
        throw new Error(`S3アップロードエラー: ${response.status}`);
      }
      
      setUploadProgress(100);
      return true;
    } catch (err) {
      console.error('S3アップロードエラー:', err);
      throw err;
    }
  };

  // 新しい写真ノートを作成（写真アップロード付き）
  const createPhotoNoteWithPhoto = async () => {
    if (!newNote.trim()) {
      setError('メモを入力してください');
      return;
    }

    if (!selectedFile) {
      setError('写真を選択してください');
      return;
    }

    setLoading(true);
    setError('');
    setUploadProgress(0);

    try {
      // 1. APIにメモを送信してS3の署名付きURLを取得
      const response = await fetch(`${apiUrl}/photonote`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ note: newNote }),
      });

      if (!response.ok) {
        throw new Error(`APIエラー: ${response.status}`);
      }

      const data = await response.json();
      const presignedUrl = data.url;

      // 2. S3に写真をアップロード
      await uploadPhotoToS3(presignedUrl, selectedFile);

      // 3. 成功時の処理
      setNewNote('');
      setSelectedFile(null);
      
      // 一覧を再取得
      await fetchPhotoNotes();
      
    } catch (err) {
      setError(`作成に失敗しました: ${err.message}`);
      console.error('作成エラー:', err);
    } finally {
      setLoading(false);
      setUploadProgress(0);
    }
  };

  // コンポーネント初期化時にデータを取得
  useEffect(() => {
    fetchPhotoNotes();
  }, []);

  // Enterキーで投稿
  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      createPhotoNoteWithPhoto();
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 p-4">
      <div className="max-w-4xl mx-auto">
        {/* ヘッダー */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-3 mb-4">
            <Camera className="text-indigo-600" size={40} />
            <h1 className="text-4xl font-bold text-gray-800">Photo Note</h1>
          </div>
          <p className="text-gray-600">写真付きメモを管理しよう</p>
        </div>

        {/* エラー表示 */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6 flex items-center gap-2">
            <AlertCircle className="text-red-500" size={20} />
            <span className="text-red-700">{error}</span>
          </div>
        )}

        {/* 新規作成フォーム */}
        <div className="bg-white rounded-xl shadow-lg p-6 mb-8 card-hover">
          <h2 className="text-xl font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <Plus size={24} />
            新しいメモを追加
          </h2>
          
          <div className="space-y-4">
            {/* 写真選択エリア */}
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center">
              <input
                type="file"
                accept="image/*"
                onChange={handleFileSelect}
                className="hidden"
                id="photo-upload"
                disabled={loading}
              />
              <label
                htmlFor="photo-upload"
                className="cursor-pointer flex flex-col items-center gap-3"
              >
                {selectedFile ? (
                  <>
                    <Image className="text-green-500" size={32} />
                    <div>
                      <p className="text-green-700 font-medium">{selectedFile.name}</p>
                      <p className="text-sm text-gray-500">
                        {(selectedFile.size / 1024 / 1024).toFixed(1)}MB
                      </p>
                    </div>
                  </>
                ) : (
                  <>
                    <Upload className="text-gray-400" size={32} />
                    <div>
                      <p className="text-gray-600 font-medium">写真を選択してください</p>
                      <p className="text-sm text-gray-500">JPG, PNG, GIF (最大10MB)</p>
                    </div>
                  </>
                )}
              </label>
            </div>

            {/* 選択した写真のプレビュー */}
            {selectedFile && (
              <div className="relative">
                <img
                  src={URL.createObjectURL(selectedFile)}
                  alt="プレビュー"
                  className="w-full h-48 object-cover rounded-lg"
                />
                <button
                  onClick={() => setSelectedFile(null)}
                  className="absolute top-2 right-2 bg-red-500 text-white rounded-full p-1 hover:bg-red-600"
                  disabled={loading}
                >
                  ×
                </button>
              </div>
            )}
            
            <textarea
              value={newNote}
              onChange={(e) => setNewNote(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="写真のメモを入力してください..."
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
              rows="3"
              disabled={loading}
            />

            {/* アップロード進捗バー */}
            {loading && uploadProgress > 0 && (
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span>アップロード中...</span>
                  <span>{uploadProgress}%</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-indigo-600 h-2 rounded-full transition-all duration-300"
                    style={{ width: `${uploadProgress}%` }}
                  ></div>
                </div>
              </div>
            )}
            
            <div className="flex gap-3">
              <button
                onClick={createPhotoNoteWithPhoto}
                disabled={loading || !newNote.trim() || !selectedFile}
                className="flex-1 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-400 text-white font-medium py-2 px-4 rounded-lg transition-colors flex items-center justify-center gap-2"
              >
                {loading ? (
                  <RefreshCw className="animate-spin" size={20} />
                ) : (
                  <Plus size={20} />
                )}
                {loading ? 'アップロード中...' : '写真メモを追加'}
              </button>
              
              <button
                onClick={fetchPhotoNotes}
                disabled={loading}
                className="bg-gray-500 hover:bg-gray-600 disabled:bg-gray-400 text-white font-medium py-2 px-4 rounded-lg transition-colors flex items-center gap-2"
              >
                <RefreshCw className={loading ? 'animate-spin' : ''} size={20} />
                更新
              </button>
            </div>
          </div>
        </div>

        {/* 写真ノート一覧 */}
        <div className="bg-white rounded-xl shadow-lg p-6">
          <h2 className="text-xl font-semibold text-gray-800 mb-6 flex items-center gap-2">
            <FileText size={24} />
            写真ノート一覧 ({photoNotes.length}件)
          </h2>

          {loading && photoNotes.length === 0 ? (
            <div className="text-center py-8">
              <RefreshCw className="animate-spin mx-auto mb-4 text-indigo-600" size={40} />
              <p className="text-gray-600">読み込み中...</p>
            </div>
          ) : photoNotes.length === 0 ? (
            <div className="text-center py-12">
              <Camera className="mx-auto mb-4 text-gray-400" size={64} />
              <p className="text-gray-500 text-lg">まだメモがありません</p>
              <p className="text-gray-400">上のフォームから最初のメモを追加してみましょう</p>
            </div>
          ) : (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {photoNotes.map((photoNote, index) => (
                <div
                  key={index}
                  className="border border-gray-200 rounded-lg p-4 card-hover"
                >
                  {/* 写真表示エリア */}
                  <div className="aspect-video bg-gray-100 rounded-lg mb-3 flex items-center justify-center overflow-hidden">
                    {photoNote.url ? (
                      <img
                        src={photoNote.url}
                        alt={photoNote.note}
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.target.style.display = 'none';
                          e.target.nextSibling.style.display = 'flex';
                        }}
                      />
                    ) : null}
                    <div className="flex flex-col items-center text-gray-400">
                      <Camera size={32} />
                      <span className="text-sm mt-2">写真</span>
                    </div>
                  </div>
                  
                  {/* メモテキスト */}
                  <p className="text-gray-700 text-sm leading-relaxed">
                    {photoNote.note}
                  </p>
                  
                  {/* URL表示 (デバッグ用) */}
                  {photoNote.url && (
                    <div className="mt-3 pt-3 border-t border-gray-100">
                      <p className="text-xs text-gray-500 truncate">
                        URL: {photoNote.url}
                      </p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* フッター */}
        <div className="text-center mt-8 text-gray-500 text-sm">
          <p>API接続先: {apiUrl}</p>
          <p className="mt-1">Photo Note App - 写真とメモを簡単管理</p>
        </div>
      </div>
    </div>
  );
};

export default PhotoNoteApp;
