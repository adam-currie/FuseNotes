/*
 * The MIT License
 *
 * Copyright 2017 Adam Currie.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.adam_currie.fusenotesclient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/*
 * Name     AESEncryption
 * Purpose  Encryption and decryption of messages.
 */
class AESEncryption {
    private Charset charset = StandardCharsets.ISO_8859_1;
    private PaddedBufferedBlockCipher encryptCipher;
    private PaddedBufferedBlockCipher decryptCipher;
    private SecureRandom rand = new SecureRandom();
    private byte[] keyBytes;
    private String privateKeyStr;
    
    /*
     * Method           main
     * Description      test main  
     */
    public static void main(String[] args) throws UnsupportedEncodingException, InvalidCipherTextException{
        AESEncryption aes = new AESEncryption("testkeystring");
        String cipherText = aes.encrypt("test string test string test string test string test string test string test string test string test string test string test string");
        
        String secret = aes.decrypt(cipherText);
        String emptyTest = aes.decrypt("");
        String nullTest = aes.decrypt(null);
        return;
    }
    
    /*
     * Method           AESEncryption
     * Description      Creates AESEncryption with a key.
     * Params       
     *  String KeyStr   AES key in base64.      
     */
    AESEncryption(String keyStr) {
        privateKeyStr = keyStr;
        
        encryptCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        decryptCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(keyStr.getBytes(charset));
            keyBytes = md.digest();
        }catch(NoSuchAlgorithmException ex){
            Logger.getLogger(AESEncryption.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException();
        }
        
    }
    
    String getPrivateKeyStr(){
        return privateKeyStr;
    }
    
    /*
     * Method           encrypt
     * Description      ecrypts a payload
     * Params           
     *  String paload   payload to encypt
     * Returns
     *  String          encrypted cypher text
     */
    String encrypt(String payload){
        InputStream in = new ByteArrayInputStream(payload.getBytes(charset));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        //prepare encryptCipher
        byte[] ivBytes = new byte[encryptCipher.getBlockSize()];
	    rand.nextBytes(ivBytes);        
        ParametersWithIV params = new ParametersWithIV(new KeyParameter(keyBytes),ivBytes);
        encryptCipher.init(true, params);
        
        //add iv to cipherText
        out.write(ivBytes, 0, ivBytes.length);
        
        //process
        int numBytesRead;        //number of bytes read from input
        int numBytesProcessed;   //number of bytes processed
        byte[] buf = new byte[16];
        byte[] obuf = new byte[32];
        try{
            while ((numBytesRead = in.read(buf)) >= 0) {
                numBytesProcessed = encryptCipher.processBytes(buf, 0, numBytesRead, obuf, 0);
                out.write(obuf, 0, numBytesProcessed);
            }
        
            numBytesProcessed = encryptCipher.doFinal(obuf, 0);
            out.write(obuf, 0, numBytesProcessed);
            out.flush();
            in.close();
            out.close();
        }catch(IOException | DataLengthException | IllegalStateException | InvalidCipherTextException ex){
            Logger.getLogger(AESEncryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        encryptCipher.reset();
        
        return Base64.getEncoder().encodeToString(out.toByteArray());      
    }
    
    /*
     * Method               decrypt
     * Description          decrypts a message
     * Params       
     *  String cipherText   AES key in base64
     * Returns
     *  String              decrypted payload
     */
    String decrypt(String cipherText) throws InvalidCipherTextException{
        if(cipherText == ""){
            return "";
        }
        
        InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(cipherText));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        //prepare encryptCipher
        byte[] ivBytes = new byte[decryptCipher.getBlockSize()];
        try{
            in.read(ivBytes, 0, ivBytes.length);
        }catch(IOException ex){
            Logger.getLogger(AESEncryption.class.getName()).log(Level.SEVERE, null, ex);
            throw new InvalidCipherTextException("Could not read initialization vector.");
        }
        ParametersWithIV params = new ParametersWithIV(new KeyParameter(keyBytes),ivBytes);
        decryptCipher.init(false, params);
        
        //process
        int numBytesRead;
        int numBytesProcessed;
        byte[] buf = new byte[16];
        byte[] obuf = new byte[16];
        try{
            while ((numBytesRead = in.read(buf)) >= 0) {
                numBytesProcessed = decryptCipher.processBytes(buf, 0, numBytesRead, obuf, 0);
                out.write(obuf, 0, numBytesProcessed);
            }
        
            numBytesProcessed = decryptCipher.doFinal(obuf, 0);
            out.write(obuf, 0, numBytesProcessed);
            out.flush();
            in.close();
            out.close();
        }catch(IOException | DataLengthException | IllegalStateException | InvalidCipherTextException ex){
            Logger.getLogger(AESEncryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        decryptCipher.reset();
        
        return new String(out.toByteArray(), charset);
    }

}
