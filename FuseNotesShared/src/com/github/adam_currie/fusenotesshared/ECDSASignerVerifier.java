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

import java.nio.charset.StandardCharsets;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;

/**\
 * Provides thread-safe verifying and optionally signing.
 * @author Adam Currie
 */
public class ECDSASignerVerifier{
    private final ECPrivateKeyParameters privateKey;
    private final ECPublicKeyParameters publicKey;
    
    //initialize for signing and verifying
    public ECDSASignerVerifier(ECPrivateKeyParameters key){        
        this.privateKey = key;
        this.publicKey = ECDSAUtil.toPublicKeyParams(privateKey);
    }
    
    //initialize for verifying
    public ECDSASignerVerifier(ECPublicKeyParameters key){
        this.privateKey = null;
        this.publicKey = ECDSAUtil.toPublicKeyParams(privateKey);
    }
    
    public boolean canSign(){
        return privateKey != null;
    }

    ECDSASignature sign(String message){
        if(!canSign()){
            throw new IllegalStateException("not initialized for signing");
        }
        
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        
        ECDSASigner signer = new org.bouncycastle.crypto.signers.ECDSASigner();
        signer.init(true, privateKey);
        
        return new ECDSASignature(signer.generateSignature(msgBytes));
    }
    
    public boolean checkSignature(String message, ECDSASignature signature){
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        
        ECDSASigner verifier = new ECDSASigner();
        verifier.init(false, publicKey);
        
        return verifier.verifySignature(msgBytes, signature.r(), signature.s());
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
