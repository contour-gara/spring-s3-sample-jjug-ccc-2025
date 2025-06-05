// API関連の型定義
export interface PhotoNote {
  note: string;
  url: string;
}

export interface FindAllPhotoNoteResponse {
  photoNotes: PhotoNote[];
}

export interface SavePhotoNoteRequest {
  note: string;
}

export interface SavePhotoNoteResponse {
  url: string;
}

// UI関連の型定義
export interface FileWithPreview extends File {
  preview?: string;
}

// イベントハンドラーの型定義
export type FileChangeEvent = React.ChangeEvent<HTMLInputElement>;
export type FormSubmitEvent = React.FormEvent<HTMLFormElement>;
export type ButtonClickEvent = React.MouseEvent<HTMLButtonElement>;
export type TextAreaChangeEvent = React.ChangeEvent<HTMLTextAreaElement>;
export type KeyPressEvent = React.KeyboardEvent<HTMLTextAreaElement>;

// API エラー型定義
export interface ApiError {
  message: string;
  status?: number;
}

// アップロード進捗の型定義
export interface UploadProgress {
  loaded: number;
  total: number;
  percentage: number;
}
