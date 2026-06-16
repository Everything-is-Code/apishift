export interface ChatMessage {
  role: string;
  content: string;
  cached?: boolean;
}
