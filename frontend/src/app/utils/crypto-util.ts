/**
 * CryptoUtil - RSA-OAEP encryption utility for protecting sensitive data
 * like PIN numbers in network requests.
 *
 * Uses the Web Crypto API (SubtleCrypto) with RSA-OAEP SHA-256.
 * The public key is fetched from the server and cached.
 */

let cachedPublicKey: CryptoKey | null = null;
let cachedPublicKeySpki: string | null = null;

function base64ToArrayBuffer(base64: string): ArrayBuffer {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

async function getPublicKey(): Promise<CryptoKey> {
  if (cachedPublicKey) {
    return cachedPublicKey;
  }

  const stored = typeof window !== 'undefined' ? localStorage.getItem('auth_public_key') : null;
  if (stored) {
    try {
      const spki = base64ToArrayBuffer(stored);
      cachedPublicKey = await crypto.subtle.importKey(
        'spki',
        spki,
        { name: 'RSA-OAEP', hash: 'SHA-256' },
        true,
        ['encrypt']
      );
      cachedPublicKeySpki = stored;
      return cachedPublicKey;
    } catch {
      cachedPublicKey = null;
      cachedPublicKeySpki = null;
    }
  }

  const response = await fetch('http://localhost:8080/api/v1/auth/public-key');
  const data = await response.json();
  const spki = base64ToArrayBuffer(data.publicKey);

  cachedPublicKey = await crypto.subtle.importKey(
    'spki',
    spki,
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    true,
    ['encrypt']
  );
  cachedPublicKeySpki = data.publicKey;

  if (typeof window !== 'undefined') {
    localStorage.setItem('auth_public_key', data.publicKey);
  }

  return cachedPublicKey;
}

export async function encryptSensitiveData(plaintext: string): Promise<string> {
  const publicKey = await getPublicKey();
  const encoded = new TextEncoder().encode(plaintext);
  const encrypted = await crypto.subtle.encrypt(
    { name: 'RSA-OAEP' },
    publicKey,
    encoded
  );
  return arrayBufferToBase64(encrypted);
}

export async function encryptPin(pin: string): Promise<string> {
  return encryptSensitiveData(pin);
}
