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
package com.github.adam_currie.fusenotesshared;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.Arrays;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;

/**
 *
 * @author Adam Currie
 */
public class ECDSASigner{
    private final org.bouncycastle.crypto.signers.ECDSASigner signer;
    private final org.bouncycastle.crypto.signers.ECDSASigner verifier;
    private final ECPrivateKeyParameters privateKey;
    private final ECPublicKeyParameters publicKey;
    
    //initialize for signing and verifying
    public ECDSASigner(ECPrivateKeyParameters key) throws InvalidKeyException{
        signer = new org.bouncycastle.crypto.signers.ECDSASigner();
        verifier = new org.bouncycastle.crypto.signers.ECDSASigner();
        
        this.privateKey = key;
        this.publicKey = ECDSAUtil.toPublicKeyParams(privateKey);
        
        signer.init(true, privateKey);        
        verifier.init(false, publicKey);
    }
    
    //initialize for verifying
    public ECDSASigner(ECPublicKeyParameters key){
        signer = null;
        verifier = new org.bouncycastle.crypto.signers.ECDSASigner();
        
        this.privateKey = null;
        this.publicKey = ECDSAUtil.toPublicKeyParams(privateKey);
             
        verifier.init(false, publicKey);
    }
    
    public boolean canSign(){
        return signer != null;
    }

    byte[] sign(String message){
        if(!canSign()){
            throw new IllegalStateException("not initialized for signing");
        }
        
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        
        BigInteger[] rs = signer.generateSignature(msgBytes);

        byte[] signature = new byte[66];
        
        byte[] r = rs[0].toByteArray();
        byte[] s = rs[1].toByteArray();
        
        //in the binary representation of a negative BigInteger leading 1's are inconsequential, and for positive values leading zeros are inconsequential
        
        //copy with leading zeros
        System.arraycopy(r, 0, signature, 33-r.length, r.length);
        //if the integer is negative, pad with leading 1's (-1 as a byte is 1111111 in binary)
        if(r[0] < 0){
            java.util.Arrays.fill(signature, 0, 33-r.length, (byte)-1);
        }
        
        //copy with leading zeros
        System.arraycopy(s, 0, signature, 66-s.length, s.length);
        //if the integer is negative, pad with leading 1's (-1 as a byte is 1111111 in binary)
        if(s[0] < 0){
            java.util.Arrays.fill(signature, 33, 66-s.length, (byte)-1);
        }
        
        return signature;//debug
    }
    
    public boolean checkSignature(ECPublicKeyParameters key, String message, byte[] signature){
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        
        byte[] r = Arrays.copyOfRange(signature, 0, 33);
        byte[] s = Arrays.copyOfRange(signature, 33, 66);
        
        return verifier.verifySignature(msgBytes, new BigInteger(r), new BigInteger(s));
    }

    public ECPrivateKeyParameters getPrivateKey(){
        if(!canSign()){
            throw new IllegalStateException("not initialized for signing");
        }
        return privateKey;
    }
    
    public String getPrivateKeyString(){
        return ECDSAUtil.toBase64(getPrivateKey());
    }

    public byte[] getPublicKeyBytes(){
        return publicKey.getQ().getEncoded(true);
    }
    
}
