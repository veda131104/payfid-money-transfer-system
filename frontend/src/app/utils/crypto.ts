const KEY = "PayFidSuperSecureSecretKey123";

export function encrypt(value: string | null | undefined): string {
  if (!value) return '';
  const encoder = new TextEncoder();
  const bytes = encoder.encode(value);
  const keyBytes = encoder.encode(KEY);
  const encrypted = new Uint8Array(bytes.length);
  for (let i = 0; i < bytes.length; i++) {
    encrypted[i] = bytes[i] ^ keyBytes[i % keyBytes.length];
  }
  let binary = '';
  const len = encrypted.byteLength;
  for (let i = 0; i < len; i++) {
    binary += String.fromCharCode(encrypted[i]);
  }
  return btoa(binary);
}

export function decrypt(value: string | null | undefined): string {
  if (!value) return '';
  try {
    const binary = atob(value);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    const encoder = new TextEncoder();
    const keyBytes = encoder.encode(KEY);
    const decrypted = new Uint8Array(bytes.length);
    for (let i = 0; i < bytes.length; i++) {
      decrypted[i] = bytes[i] ^ keyBytes[i % keyBytes.length];
    }
    const decoder = new TextDecoder();
    return decoder.decode(decrypted);
  } catch (e) {
    return value || '';
  }
}
