/*
 * Copyright 2014 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.meena.bigquery.fn;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

/**
 * An immutable sequence of bytes.
 *
 * <p><strong>Full disclosure:</strong> this class provides untrusted input and
 * output streams with raw access to the underlying byte array. A hostile
 * stream implementation could keep a reference to the mutable byte string,
 * violating the immutable guarantee of this class. For this reason a byte
 * string's immutability guarantee cannot be relied upon for security in applets
 * and other environments that run both trusted and untrusted code in the same
 * process.
 */
public final class ByteString {

    private static final char[] HEX_DIGITS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    final byte[] data;
    private transient int hashCode; // Lazily computed; 0 if unknown.
    private transient String utf8; // Lazily computed.

    ByteString(byte[] data) {
        this.data = data; // Trusted internal constructor doesn't clone data.
    }

    /**
     * Returns a new byte string containing a clone of the bytes of {@code data}.
     */
    public static ByteString of(byte... data) {
        return new ByteString(data.clone());
    }

    /**
     * Constructs a new {@code String} by decoding the bytes as {@code UTF-8}.
     */
    public String utf8() {
        String result = utf8;
        // We don't care if we double-allocate in racy code.
        return result != null ? result : (utf8 = new String(data, StandardCharsets.UTF_8));
    }

    /**
     * Returns this byte string encoded as <a
     * href="http://www.ietf.org/rfc/rfc2045.txt">Base64</a>. In violation of the
     * RFC, the returned string does not wrap lines at 76 columns.
     */
    public String base64() {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decodes the Base64-encoded bytes and returns their value as a byte string.
     * Returns null if {@code base64} is not a Base64-encoded sequence of bytes.
     */
    public static ByteString decodeBase64(String base64) {
        byte[] decoded = Base64.getDecoder().decode(base64);
        return decoded != null ? new ByteString(decoded) : null;
    }

    /**
     * Returns this byte string encoded in hexadecimal.
     */
    public String hex() {
        char[] result = new char[data.length * 2];
        int c = 0;
        for (byte b : data) {
            result[c++] = HEX_DIGITS[(b >> 4) & 0xf];
            result[c++] = HEX_DIGITS[b & 0xf];
        }
        return new String(result);
    }

    /**
     * Reads {@code count} bytes from {@code in} and returns the result.
     *
     * @throws java.io.EOFException if {@code in} has fewer than {@code count}
     *                              bytes to read.
     */
    public static ByteString read(InputStream in, int byteCount) throws IOException {
        byte[] result = new byte[byteCount];
        for (int offset = 0, read; offset < byteCount; offset += read) {
            read = in.read(result, offset, byteCount - offset);
            if (read == -1) throw new EOFException();
        }
        return new ByteString(result);
    }

    /**
     * Returns the number of bytes in this ByteString.
     */
    public int size() {
        return data.length;
    }

    /**
     * Returns a byte array containing a copy of the bytes in this {@code ByteString}.
     */
    public byte[] toByteArray() {
        return data.clone();
    }

    /**
     * Writes the contents of this byte string to {@code out}.
     */
    public void write(OutputStream out) throws IOException {
        out.write(data);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ByteString && Arrays.equals(((ByteString) o).data, data);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        return result != 0 ? result : (hashCode = Arrays.hashCode(data));
    }

    @Override
    public String toString() {
        if (data.length == 0) {
            return "ByteString[size=0]";
        }
        if (data.length <= 16) {
            return String.format("ByteString[size=%s data=%s]", data.length, hex());
        }
        try {
            return String.format("ByteString[size=%s md5=%s]", data.length,
                    ByteString.of(MessageDigest.getInstance("MD5").digest(data)).hex());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError();
        }
    }
}
